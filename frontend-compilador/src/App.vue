<template>


  <nav class="sidebar">
    <div style="margin-bottom: 30px; font-size: 24px; color: var(--texto-cinza)">≡</div>
    <button class="sidebar-item active"><span class="sidebar-icon">🏠</span>Início</button>

    <div class="theme-container">
      <button class="sidebar-item" @click="menuTemaAberto = !menuTemaAberto">
        <span class="sidebar-icon">🎨</span> Tema
      </button>

      <div class="theme-menu" :class="{ 'show': menuTemaAberto }">
        <div class="theme-option" @click="mudarTema('claro')">☀️ Claro</div>
        <div class="theme-option" @click="mudarTema('escuro')">🌙 Escuro</div>
        <div class="theme-option" @click="mudarTema('dracula')">🧛 Dracula</div>
      </div>
    </div>
  </nav>

  <div class="app-content">

    <div class="dashboard-col">
      <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-editor' }" @click="mudarVisao('visao-editor')">
        <div class="card-header">
          <div class="card-icon" style="background: #e28e83;">&lt;&gt;</div>
          Código-fonte
        </div>
        <div class="card-number">{{ linhasDigitadas }} <span class="card-label">linhas digitadas</span></div>
      </div>

      <div class="section-title">Análise</div>

      <div class="row-cards">
        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-lexemas' }" @click="mudarVisao('visao-lexemas')">
          <div class="card-header" style="font-size: 15px;">
            <div class="card-icon" style="background: #a390eb;">🏷️</div>
            Léxica
          </div>
          <div class="card-number">0 <span class="card-label">tokens</span></div>
        </div>

        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-simbolos' }" @click="mudarVisao('visao-simbolos')">
          <div class="card-header" style="font-size: 15px;">
            <div class="card-icon" style="background: #8aa9e8;">☰</div>
            Símbolos
          </div>
          <div class="card-number">0 <span class="card-label">símbolos</span></div>
        </div>
      </div>

      <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-arvore' }" @click="mudarVisao('visao-arvore')">
        <div class="card-header">
          <div class="card-icon" style="background: #a5d688;">🧩</div>
          Sintática (AST)
        </div>
        <div class="card-number">{{ totalNos }} <span class="card-label">nós</span></div>
      </div>

    </div>

    <div class="workspace-col">

      <div class="toolbar">
        <div class="menu-actions">
          <span @click="novoArquivo">Novo</span>
          <span>Abrir</span>
          <span>Salvar</span>
        </div>
        <button class="btn-compilar" @click="compilar">Compilar <span class="btn-hint">ctrl + enter</span></button>
      </div>

      <div class="main-display-area">

        <div v-show="visaoAtiva === 'visao-editor'" style="display: flex; height: 100%;" class="editor-container">
          <textarea id="codigo-fonte" v-model="codigoFonte" @input="atualizarLinhas" spellcheck="false" placeholder="Digite seu código LALG aqui..."></textarea>
        </div>

        <div v-show="visaoAtiva === 'visao-lexemas'" class="tabela-container">
          <h3 style="padding: 20px; color: var(--texto-cinza);">Tabela de Lexemas em construção...</h3>
        </div>

        <div v-show="visaoAtiva === 'visao-arvore'" style="height: 100%; border: 1px solid var(--borda); border-radius: 12px; overflow: hidden; background: white;">
          <VueFlow :nodes="nosDaArvore" :edges="linhasDaArvore">
            <Background pattern-color="#aaa" gap="8" />
            <Controls />
          </VueFlow>
        </div>

      </div>

      <div class="console-panel">
        <div class="tabs">
          <button class="tab" :class="{ active: abaAtiva === 'tab-erros' }" @click="mudarAba('tab-erros')">Erros ({{ erros.length }})</button>
          <button class="tab" :class="{ active: abaAtiva === 'tab-logs' }" @click="mudarAba('tab-logs')">Logs</button>
        </div>

        <div v-show="abaAtiva === 'tab-erros'" class="tab-content active">
          <div v-if="erros.length === 0" style="color: var(--texto-cinza); text-align: center; margin-top: 20px;">Nenhum erro encontrado.</div>
          <div v-else>
            <div v-for="(erro, index) in erros" :key="index" class="log-entry erro">{{ erro }}</div>
          </div>
        </div>

        <div v-show="abaAtiva === 'tab-logs'" class="tab-content active">
          <div v-for="(log, index) in logs" :key="index" class="log-entry" :class="{ sucesso: log.includes('sucesso'), erro: log.includes('fatal') }">
            {{ log }}
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'

import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

// === VARIÁVEIS DE ESTADO DA INTERFACE ===
const codigoFonte = ref('program ola_mundo;\nvar a: int;\nbegin\n   a := 10;\nend.')
const visaoAtiva = ref('visao-editor') // Controla a tela principal (editor, lexemas, arvore)
const abaAtiva = ref('tab-logs')       // Controla o console inferior
const menuTemaAberto = ref(false)
const temaAtual = ref('claro')
const linhasDigitadas = ref(5)

// === VARIÁVEIS DO COMPILADOR ===
const erros = ref([])
const logs = ref(['Aguardando compilação...'])
const nosDaArvore = ref([])
const linhasDaArvore = ref([])
const totalNos = ref(0)

// === FUNÇÕES DA INTERFACE ===
function mudarVisao(novaVisao) {
  visaoAtiva.value = novaVisao
}

function mudarAba(novaAba) {
  abaAtiva.value = novaAba
}

function mudarTema(novoTema) {
  temaAtual.value = novoTema
  document.documentElement.setAttribute('data-theme', novoTema)
  menuTemaAberto.value = false
}

function atualizarLinhas() {
  // Conta quantas quebras de linha existem no texto
  linhasDigitadas.value = codigoFonte.value.split('\n').length
}

// === FUNÇÕES DE ARQUIVO (Simples) ===
function novoArquivo() {
  codigoFonte.value = ''
  atualizarLinhas()
  logs.value.push("Novo arquivo criado.")
}

// === INTEGRAÇÃO COM O BACKEND (JAVA) ===
async function compilar() {
  logs.value.push("Iniciando compilação...")
  mudarAba('tab-logs')

  try {
    const resposta = await fetch('http://localhost:8080/api/compilar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ codigo: codigoFonte.value })
    })

    const dados = await resposta.json()

    if (dados.sucesso) {
      erros.value = []
      logs.value.push("✅ Compilação finalizada com sucesso!")
      processarArvore(dados.arvoreSintatica)

      // Muda automaticamente para a aba da árvore para mostrar o resultado!
      mudarVisao('visao-arvore')
    } else {
      erros.value = dados.erros
      logs.value.push("❌ Erros encontrados durante a compilação.")
      nosDaArvore.value = []
      linhasDaArvore.value = []
      totalNos.value = 0
      mudarAba('tab-erros') // Abre a aba de erros automaticamente
    }
  } catch (error) {
    logs.value.push("❌ Erro fatal: Não foi possível conectar ao backend (Spring Boot).")
    mudarAba('tab-logs')
  }
}

function processarArvore(noRaiz) {
  let nodes = []
  let edges = []
  let idContador = 1

  function percorrer(no, idDoPai, nivelX, nivelY) {
    if (!no) return;
    let meuId = `node_${idContador++}`

    nodes.push({
      id: meuId,
      position: { x: nivelX * 150, y: nivelY * 100 },
      data: { label: no.valor ? `${no.nome}\n(${no.valor})` : no.nome },
      style: { backgroundColor: '#4ade80', color: 'black', fontWeight: 'bold', borderRadius: '8px' }
    })

    if (idDoPai) {
      edges.push({ id: `e_${idDoPai}-${meuId}`, source: idDoPai, target: meuId })
    }

    if (no.filhos && no.filhos.length > 0) {
      no.filhos.forEach((filho, index) => {
        let deslocamentoX = index === 0 ? -1 : 1;
        percorrer(filho, meuId, nivelX + deslocamentoX, nivelY + 1)
      })
    }
  }

  percorrer(noRaiz, null, 2, 0)
  nosDaArvore.value = nodes
  linhasDaArvore.value = edges
  totalNos.value = nodes.length
}

// Inicializa o tema ao carregar a página
onMounted(() => {
  document.documentElement.setAttribute('data-theme', temaAtual.value)
  atualizarLinhas()
})
</script>

<style>

@import url('https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500&family=Inter:wght@400;500;600&display=swap');

#app {
  display: flex;
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

/*esuqemas de cores */
[data-theme="claro"] {
  --bg-main: #fcf8f7;
  --bg-sidebar: #ffffff;
  --bg-card: #ffffff;
  --bg-card-destaque: #ffc4be;
  --cor-primaria: #f4a397;
  --cor-primaria-hover: #e88d80;
  --texto-escuro: #4a3f3f;
  --texto-cinza: #8c8282;
  --borda: #f0e6e5;
  --erro-bg: #ffe6e6;
  --erro-texto: #d32f2f;
  --sucesso-bg: #e8f5e9;
  --sucesso-texto: #2e7d32;
  --hover-tabela: #f5eeed;
}

[data-theme="escuro"] {
  --bg-main: #1e1e1e;
  --bg-sidebar: #252526;
  --bg-card: #2d2d30;
  --bg-card-destaque: #094771;
  --cor-primaria: #0e639c;
  --cor-primaria-hover: #1177bb;
  --texto-escuro: #d4d4d4;
  --texto-cinza: #858585;
  --borda: #3e3e42;
  /*--erro-bg: #5a1d1d;*/
  /*--erro-texto: #f48771;*/
  /*--sucesso-bg: #1e4620;*/
  /*--sucesso-texto: #89d185;*/
  --erro-bg: #3a1515;
  --erro-texto: #fca5a5;
  --sucesso-bg: #1e4620;
  --sucesso-texto: #89d185;
  --hover-tabela: #2a2d2e;
}

[data-theme="dracula"] {
  --bg-main: #282a36;
  --bg-sidebar: #21222c;
  --bg-card: #44475a;
  --bg-card-destaque: #6272a4;
  --cor-primaria: #bd93f9;
  --cor-primaria-hover: #ff79c6;
  --texto-escuro: #f8f8f2;
  --texto-cinza: #6272a4;
  --borda: #21222c;
  /*--erro-bg: #ff555540;*/
  /*--erro-texto: #ff5555;*/
  /*--sucesso-bg: #50fa7b40;*/
  /*--sucesso-texto: #50fa7b;*/
  --erro-bg: rgba(255, 85, 85, 0.15);
  --erro-texto: #ffb8b8;
  --sucesso-bg: #50fa7b40;
  --sucesso-texto: #50fa7b;
  --hover-tabela: #44475a;
}

/* Transição suave para todas as mudanças de cor */
* {
  transition: background-color 0.3s, color 0.3s, border-color 0.3s;
}


/* reset e base */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Inter', sans-serif;
  background-color: var(--bg-main);
  color: var(--texto-escuro);
  display: flex;
  height: 100vh;
  overflow: hidden;
}

button {
  cursor: pointer;
  font-family: inherit;
  border: none;
  background: none;
}

/* layout principal */
.sidebar {
  width: 70px;
  background-color: var(--bg-sidebar);
  border-right: 1px solid var(--borda);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.sidebar-item {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 15px;
  color: var(--texto-cinza);
  font-size: 11px;
  font-weight: 500;
  transition: 0.2s;
}

.sidebar-item.active {
  background-color: var(--bg-card-destaque);
  color: var(--texto-escuro);
}

.sidebar-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.app-content {
  flex: 1;
  display: flex;
  padding: 20px;
  gap: 20px;
  overflow: hidden;
}

/* coluna esquerda (dashoboard) */
.dashboard-col {
  width: 350px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  overflow-y: auto;
  padding-right: 5px;
}

.section-title {
  font-size: 14px;
  color: var(--texto-cinza);
  font-weight: 600;
  margin: 10px 0 5px 0;
  text-align: center;
}

.card {
  background-color: var(--bg-card);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  position: relative;
}

.card.destaque {
  background-color: var(--bg-card-destaque);
}

.card-clickable {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, border 0.2s;
  border: 2px solid transparent;
}

.card-clickable:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.05);
}

.card {
  background-color: var(--bg-card);
}

.card.destaque {
  background-color: var(--bg-card-destaque);
  border-color: var(--cor-primaria);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  font-weight: 600;
  font-size: 18px;
}

.card-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.card-number {
  font-size: 42px;
  font-weight: 400;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.card-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--texto-cinza);
}

.row-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

/* coluna direita (editor e console) */
.workspace-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 10px;
}

.menu-actions {
  display: flex;
  gap: 20px;
  color: var(--texto-escuro);
  font-size: 14px;
  font-weight: 500;
}

.menu-actions span {
  cursor: pointer;
  transition: color 0.2s;
}

.menu-actions span:hover {
  color: var(--cor-primaria-hover);
}

.btn-compilar {
  background-color: var(--cor-primaria);
  color: var(--texto-escuro);
  padding: 10px 24px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 10px rgba(244, 163, 151, 0.3);
  transition: 0.2s;
}

.btn-compilar:hover {
  background-color: var(--cor-primaria-hover);
  transform: translateY(-1px);
}

.btn-hint {
  font-size: 11px;
  opacity: 0.7;
  font-weight: 400;
}

.editor-container {
  flex: 1;
  background-color: var(--bg-main);
  border: 1px solid var(--borda);
  border-radius: 12px;
  display: flex;
  overflow: hidden;
  position: relative;
}

.line-numbers {
  padding: 15px 10px;
  text-align: right;
  color: #bcaeaa;
  background-color: var(--bg-main);
  font-family: 'Fira Code', monospace;
  font-size: 15px;
  line-height: 1.5;
  user-select: none;
  border-right: 1px solid var(--borda);
}

#codigo-fonte {
  flex: 1;
  padding: 15px;
  border: none;
  background: transparent;
  color: var(--texto-escuro);
  font-family: 'Fira Code', monospace;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  outline: none;
  white-space: pre;
}

/* painel inferior (Abas) */
.console-panel {
  height: 250px;
  background-color: var(--bg-main);
  border-top: 2px solid var(--borda);
  display: flex;
  flex-direction: column;
}

.tabs {
  display: flex;
  border-bottom: 1px solid var(--borda);
}

.tab {
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--texto-cinza);
  border-bottom: 2px solid transparent;
  transition: 0.2s;
}

.tab.active {
  color: var(--texto-escuro);
  border-bottom-color: var(--texto-escuro);
}

.tab:hover {
  color: var(--texto-escuro);
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: none;
}

.tab-content.active {
  display: block;
}

.log-entry {
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
  border-left: 4px solid var(--borda);
}

.log-entry.erro {
  background-color: var(--erro-bg);
  color: var(--erro-texto);
  border-left-color: var(--erro-texto);
}

.log-entry.sucesso {
  background-color: var(--sucesso-bg);
  color: var(--sucesso-texto);
  border-left-color: var(--sucesso-texto);
}

/* tabela */
table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13px;
}

th {
  color: var(--texto-cinza);
  font-weight: 600;
  padding: 10px;
  border-bottom: 1px solid var(--borda);
  position: sticky;
  top: 0;
  background: var(--bg-main);
}

td {
  padding: 10px;
  border-bottom: 1px solid var(--borda);
  font-family: 'Fira Code', monospace;
}

tr:hover {
  background-color: var(--hover-tabela);
}

.token-tag {
  background: #eef2ff;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/*customziação da scrollbar*/
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d9cfcd;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #bcaeaa;
}

/* troca de views */
.main-display-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Formatação do container das tabelas quando elas estão ativas */
.tabela-container {
  flex: 1;
  background-color: var(--bg-main);
  border: 1px solid var(--borda);
  border-radius: 12px;
  overflow-y: auto;
}

/* garante que o cabeçalho da tabela fique fixo no topo ao rolar para baixo */
th {
  position: sticky;
  top: 0;
  background: var(--bg-main);
  z-index: 10;
  box-shadow: 0 1px 0 var(--borda);
}

.theme-container {
  position: relative;
  display: flex;
  width: 100%;
  justify-content: center;
}

.theme-menu {
  display: none; /*escondido por padrão*/
  position: absolute;
  top: 0;
  left: 70px; /*faz o menu aparecer fora da barra lateral*/
  background-color: var(--bg-card);
  border: 1px solid var(--borda);
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  width: 170px;
  z-index: 100;
  overflow: hidden;
}

.theme-menu.show {
  display: block;
  animation: fadeIn 0.2s ease-out;
}

.theme-option {
  padding: 12px 15px;
  font-size: 13px;
  color: var(--texto-escuro);
  cursor: pointer;
  transition: background 0.2s;
  font-weight: 500;
}

.theme-option:hover {
  background-color: var(--bg-main);
  color: var(--cor-primaria);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>