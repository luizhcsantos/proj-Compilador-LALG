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
      <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-editor' }"
           @click="mudarVisao('visao-editor')">
        <div class="card-header">
          <div class="card-icon" style="background: #e28e83;">&lt;&gt;</div>
          Código-fonte
        </div>
        <div class="card-number">{{ linhasDigitadas }} <span class="card-label">linhas digitadas</span></div>
      </div>

      <div class="section-title">Análise</div>

      <div class="row-cards">
        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-lexemas' }"
             @click="mudarVisao('visao-lexemas')">
          <div class="card-header" style="font-size: 15px;">
            <div class="card-icon" style="background: #a390eb;">🏷️</div>
            Léxica
          </div>
          <div class="card-number">{{ listaTokens.length }} <span class="card-label">tokens</span></div>
        </div>

        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-simbolos' }"
             @click="mudarVisao('visao-simbolos')">
          <div class="card-header" style="font-size: 15px;">
            <div class="card-icon" style="background: #8aa9e8;">☰</div>
            Símbolos
          </div>
          <div class="card-number">{{ listaSimbolos.length }} <span class="card-label">símbolos</span></div>
        </div>
      </div>

      <div class="row-cards">
        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-arvore' }"
             @click="mudarVisao('visao-arvore')">
          <div class="card-header">
            <div class="card-icon" style="background: #a5d688;">🧩</div>
            Sintática (AST)
          </div>
          <div class="card-number">{{ totalNos }} <span class="card-label">nós</span></div>
        </div>

        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-tabela-ll1' }"
             @click="mudarVisao('visao-tabela-ll1')">
          <div class="card-header" style="font-size: 14px;">
            <div class="card-icon" style="background: #38bdf8;">📊</div>
            Tabela LL(1)
          </div>
          <div class="card-number" style="font-size: 18px;">Matriz preditiva</div>
        </div>
      </div>

      <div class="section-title">Geração de código</div>
      <div class="row-cards">

        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-codigo' }"
             @click="mudarVisao('visao-codigo')">
          <div class="card-header">
            <div class="card-icon" style="background: #fbbf24;">⚙️</div>
            Geração de código
          </div>
          <div class="card-number" style="font-size: 16px;">Bytecode <span class="card-label"></span></div>
        </div>

        <div class="card card-clickable" :class="{ destaque: visaoAtiva === 'visao-mepa' }"
             @click="mudarVisao('visao-mepa')">
          <div class="card-header">
            <div class="card-icon" style="background: #f43f5e;">💻</div>
            MEPA
          </div>
          <div class="card-number" style="font-size: 16px;">Simulador de pilha</div>
        </div>

      </div>

    </div>

    <div class="workspace-col">

      <div class="toolbar">
        <div class="menu-actions">
          <span @click="novoArquivo">Novo</span>
          <span @click="abrirArqiuvo">Abrir</span>

          <input
              type="file"
              ref="inputArquivoEscondido"
              @change="lerConteudoARquivo"
              accept=".txt, .lalg, .pas, .fct"
              style="display: none;"
          />

          <span @click="salvarARquivo">Salvar</span>


        </div>
        <button class="btn-compilar" @click="compilar">Compilar</button>
      </div>

      <div class="main-display-area">

        <div v-if="visaoAtiva === 'visao-editor'" style="display: flex; height: 100%;" class="editor-container">
          <codemirror
              v-model="codigoFonte"
              placeholder="Digite seu código LALG aqui..."
              :style="{ height: '100%', width: '100%', fontSize: '14px'}"
              :autofocus="true"
              :indent-with-tab="true"
              :tab-size="3"
              :extensions="extensions"
          />
        </div>

        <div v-show="visaoAtiva === 'visao-lexemas'" class="tabela-container">
          <table>
            <thead>
            <tr>
              <th>Lexema</th>
              <th>Token</th>
              <th>Linha</th>
              <th>Col. Inicial</th>
              <th>Col. Final</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="listaTokens.length === 0">
              <td colspan="5" style="text-align: center; color: var(--texto-cinza); padding: 20px;">
                Compile o código para gerar lexemas.
              </td>
            </tr>

            <tr v-for="(tk, index) in listaTokens" :key="index">
              <td><strong>{{ tk.lexema }}</strong></td>
              <td><span class="token-tag">{{ tk.token }}</span></td>
              <td>{{ tk.linha }}</td>
              <td>{{ tk.colunaInicial }}</td>
              <td>{{ tk.colunaFinal }}</td>
            </tr>
            </tbody>
          </table>
        </div>

        <div v-show="visaoAtiva === 'visao-simbolos'" class="tabela-container" style="height: 100%; overflow: auto; background: white; padding: 15px; border-radius: 12px; border: 1px solid var(--borda);">
          <h3 style="margin-bottom: 15px; color: #1e293b;">Tabela de Símbolos (Análise Semântica)</h3>

          <table class="tabela-ll1" style="width: 100%; border-collapse: collapse; text-align: left;">
            <thead style="background: #1e293b; color: white;">
            <tr>
              <th style="padding: 10px; border: 1px solid #cbd5e1;">Identificador</th>
              <th style="padding: 10px; border: 1px solid #cbd5e1;">Tipo</th>
              <th style="padding: 10px; border: 1px solid #cbd5e1;">Categoria</th>
              <th style="padding: 10px; border: 1px solid #cbd5e1;">Nível de Escopo</th>
              <th style="padding: 10px; border: 1px solid #cbd5e1;">Endereço (MEPA)</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="listaSimbolos.length === 0">
              <td colspan="5" style="text-align: center; padding: 20px; color: #64748b;">
                Nenhum símbolo gerado. Verifique se há erros sintáticos ou compile o código primeiro.
              </td>
            </tr>

            <tr v-for="(simbolo, index) in listaSimbolos" :key="index" style="border-bottom: 1px solid #e2e8f0; background: #f8fafc;">
              <td style="padding: 10px; font-weight: bold; color: #0f172a;">{{ simbolo.nome }}</td>
              <td style="padding: 10px; color: #2563eb; font-weight: 500;">{{ simbolo.tipo }}</td>
              <td style="padding: 10px; color: #475569;">
                <span style="background: #e2e8f0; padding: 3px 8px; border-radius: 4px; font-size: 12px;">{{ simbolo.categoria }}</span>
              </td>
              <td style="padding: 10px; text-align: center;">{{ simbolo.nivelEscopo }}</td>
              <td style="padding: 10px; font-family: monospace; color: #16a34a; text-align: center; font-weight: bold;">
                {{ simbolo.enderecoRelativo !== -1 ? simbolo.enderecoRelativo : '-' }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div v-show="visaoAtiva === 'visao-arvore'"
             class="arvore-container"
             :class="{ 'maximized-view': arvoreMaximizada }"
             style="display: flex; flex-direction: column; height: 100%; border: 1px solid var(--borda); border-radius: 12px; overflow: hidden; background: white;">

          <div style="display: flex; gap: 15px; padding: 10px 15px; background: var(--bg-main); border-bottom: 1px solid var(--borda); align-items: center;">
            <button class="btn-action-arvore" @click="arvoreMaximizada = !arvoreMaximizada">
              {{ arvoreMaximizada ? '🗗 Restaurar' : '🗖 Maximizar' }}
            </button>

            <div style="width: 1px; background: var(--borda); height: 20px;"></div>
            <button @click="mostrarArvoreCompleta" :style="{ fontWeight: !modoPassoPasso ? 'bold' : 'normal' }">
              🌳 Completa
            </button>
            <button @click="iniciarPassoAPasso" :style="{ fontWeight: modoPassoPasso ? 'bold' : 'normal' }">
              👣 Passo a Passo
            </button>

            <div v-show="modoPassoPasso" style="display: flex; gap: 10px; margin-left: auto; align-items: center;">
              <button @click="passoAnterior" :disabled="passoAtual === 0"
                      style="background: var(--bg-card); padding: 4px 8px; border-radius: 4px;">⏪ Voltar
              </button>
              <span style="font-size: 13px; font-weight: bold;">{{ passoAtual }} / {{ totalPAssos }}</span>
              <button @click="proximoPasso" :disabled="passoAtual === totalPAssos"
                      style="background: var(--bg-card); padding: 4px 8px; border-radius: 4px;">Avançar ⏩
              </button>
            </div>
          </div>

          <div style="flex: 1; width: 100%; height: 100%; min-height: 0; position: relative;">
            <VueFlow :nodes="nosDaArvore"
                     :edges="linhasDaArvore"
                     style="width: 100%; height: 100%;">
              <Background pattern-color="#aaa" gap="8"/>
              <Controls/>
            </VueFlow>
          </div>
        </div>

        <div v-show="visaoAtiva === 'visao-tabela-ll1'" class="tabela-container" style="height: 100%; overflow: auto; background: white; padding: 15px; border-radius: 12px; border: 1px solid var(--borda);">
          <h3 style="margin-bottom: 15px; color: #1e293b;">Matriz de Análise Preditiva LL(1)</h3>
          <div class="scroll-wrapper" style="overflow-x: auto; max-height: 100%;">
            <table class="tabela-ll1" style="width: 100%; border-collapse: collapse; white-space: nowrap; font-size: 13px;">
              <thead>
              <tr>
                <th class="celula-fixa" style="background: #1e293b; color: white; padding: 8px; border: 1px solid #cbd5e1; position: sticky; left: 0; z-index: 10;">Não-Terminal \ Terminal</th>
                <th v-for="terminal in colunasTerminais" :key="terminal" style="background: #1e293b; color: #38bdf8; padding: 8px; border: 1px solid #cbd5e1; font-family: monospace;">
                  {{ terminal }}
                </th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="nt in linhasNaoTerminais" :key="nt">
                <td class="celula-fixa non-terminal" style="background: #f1f5f9; font-weight: bold; padding: 8px; border: 1px solid #cbd5e1; position: sticky; left: 0; z-index: 5; text-align: right;">{{ nt }}</td>
                <td v-for="terminal in colunasTerminais" :key="terminal"
                    :style="{ background: !matrizSintatica[nt]?.[terminal] ? '#fafafa' : '#dcfce7', color: !matrizSintatica[nt]?.[terminal] ? '#cbd5e1' : '#166534', padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center' }">
              <span v-if="matrizSintatica[nt]?.[terminal]">
                {{ formatarRegraTabela(nt, matrizSintatica[nt][terminal]) }}
              </span>
                  <span v-else>-</span>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-show="visaoAtiva === 'visao-codigo'" style="height: 100%; background: #0f172a; color: #38bdf8; padding: 20px; border-radius: 12px; font-family: monospace; overflow-y: auto;">
          <h3 style="color: #f8fafc; margin-bottom: 15px; font-family: sans-serif;">Código Intermediário Gerado (Bytecode MEPA)</h3>
          <pre style="line-height: 1.5; font-size: 14px;">{{ codigoMepaGerado || '; Nenhum código gerado ainda. Compile um programa válido.' }}</pre>
        </div>

        <div v-show="visaoAtiva === 'visao-mepa'" style="height: 100%; background: white; padding: 20px; border-radius: 12px; border: 1px solid var(--borda); display: flex; flex-direction: column;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
            <h3 style="color: #1e293b; margin: 0;">Simulador da Máquina Virtual MEPA</h3>
            <button @click="iniciarExecucaoMEPA" :disabled="isExecutando" style="background: #2563eb; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-weight: bold;">
              ▶ Iniciar Execução Visual
            </button>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; flex: 1;">

            <div style="background: #f8fafc; padding: 15px; border-radius: 8px; border: 1px solid #e2e8f0; overflow-y: auto; max-height: 400px;">
              <h4 style="margin-bottom: 10px; color: #0f172a;">Instruções</h4>
              <div v-for="(linha, index) in instrucoesMepa" :key="index"
                   :style="{ padding: '4px 8px', fontFamily: 'monospace', fontSize: '13px',
                             backgroundColor: index === ponteiroInstrucao ? '#fef08a' : 'transparent',
                             borderLeft: index === ponteiroInstrucao ? '4px solid #eab308' : '4px solid transparent',
                             fontWeight: index === ponteiroInstrucao ? 'bold' : 'normal'}">
                {{ index }}: {{ linha }}
              </div>
            </div>

            <div style="display: flex; flex-direction: column; gap: 15px;">

              <div style="background: #f8fafc; padding: 15px; border-radius: 8px; border: 1px solid #e2e8f0; flex: 1; display: flex; flex-direction: column;">
                <h4 style="margin-bottom: 10px; color: #0f172a;">Pilha de Dados (M)</h4>
                <div style="font-family: monospace; background: #0f172a; color: #4ade80; padding: 10px; border-radius: 4px; flex: 1; display: flex; flex-direction: column-reverse; overflow-y: auto;">
                  <div v-if="memoriaMepa.length === 0" style="color: #64748b; text-align: center;">[ VAZIO ]</div>
                  <div v-for="(valor, index) in memoriaMepa" :key="index" style="padding: 4px 0; border-bottom: 1px dashed #334155;">
                    <span style="color: #94a3b8; font-size: 11px; margin-right: 8px;">Endereço {{ index }}</span>
                    <span style="font-weight: bold;">{{ valor }}</span>
                  </div>
                </div>
              </div>

              <div style="background: #f8fafc; padding: 15px; border-radius: 8px; border: 1px solid #e2e8f0; height: 120px; display: flex; flex-direction: column;">
                <h4 style="margin-bottom: 10px; color: #0f172a;">Terminal (Comando write)</h4>
                <div style="font-family: monospace; background: black; color: white; padding: 10px; border-radius: 4px; flex: 1; overflow-y: auto;">
                  <div v-for="(log, i) in consoleSaida" :key="i">> {{ log }}</div>
                </div>
              </div>

            </div>
          </div>
        </div>

      </div>

      <div class="console-panel" :class="{ 'console-minimizado': !consoleAberto }">

        <div
            style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--borda);">
          <div class="tabs" style="border-bottom: none;">
            <button class="tab" :class="{ active: abaAtiva === 'tab-erros' }" @click="mudarAba('tab-erros')">
              Erros ({{ listaErros?.length || 0 }})
            </button>
            <button class="tab" :class="{ active: abaAtiva === 'tab-logs' }" @click="mudarAba('tab-logs')">Logs</button>
          </div>

          <button @click="alternaConsole" class="btn-toggle-console" style="margin-right: 15px;">
            {{ consoleAberto ? '▼ Esconder' : '▲ Mostrar' }}
          </button>
        </div>

        <div class="console-body" v-show="consoleAberto">

          <div v-show="abaAtiva === 'tab-erros'" class="tab-content active">
            <div v-if="!listaErros || listaErros.length === 0"
                 style="color: var(--texto-cinza); text-align: center; margin-top: 20px;">Nenhum erro encontrado.
            </div>
            <div v-else>
              <div v-for="(erro, index) in listaErros" :key="index" class="log-entry erro">{{ erro }}</div>
            </div>
          </div>

          <div v-show="abaAtiva === 'tab-logs'" class="tab-content active">
            <div v-for="(log, index) in logs" :key="index" class="log-entry"
                 :class="{ sucesso: log.includes('sucesso'), erro: log.includes('fatal') }">
              {{ log }}
            </div>
          </div>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, computed} from 'vue';
import {VueFlow} from '@vue-flow/core';
import {Background} from '@vue-flow/background';
import {Controls} from '@vue-flow/controls';
import {Codemirror} from 'vue-codemirror';
import {StreamLanguage} from '@codemirror/language';
import {pascal} from '@codemirror/legacy-modes/mode/pascal';
import { useVueFlow } from '@vue-flow/core';

import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

const extensions = [StreamLanguage.define(pascal)];

const codigoFonte = ref('program ola_mundo;\nint a, b, soma; \nbegin\n   a := 10;\n   b := 5;\n   soma := a + b\nend.');
const visaoAtiva = ref('visao-editor'); // Controla a tela principal (editor, lexemas, arvore)
const abaAtiva = ref('tab-logs');   // Controla o console inferior
const menuTemaAberto = ref(false);
const temaAtual = ref('claro');
const linhasDigitadas = ref(0);

const listaErros = ref([]);
const logs = ref(['Aguardando compilação...']);
const listaTokens = ref([]);

const arvoreCompleta = ref(null);
const nosDaArvore = ref([]);
const linhasDaArvore = ref([]);
const totalNos = ref(0);

const modoPassoPasso = ref(false);
const passoAtual = ref(0);
const totalPAssos = ref(0);

const consoleAberto = ref(true);
const arvoreMaximizada = ref(false);
const inputArquivoEscondido = ref(null);

const listaSimbolos = ref([]);
const matrizSintatica = ref({});
const colunasTerminais = ref([]);
const linhasNaoTerminais = ref([]);

const codigoMepaGerado = ref([]);
const instrucoesMepa = ref([]);
const memoriaMepa = ref([]);    // pilha de dados (M)
const consoleSaida = ref([]);   // saída do comando IMPR
const ponteiroInstrucao = ref(0); // instruction pointer (IP)
const isExecutando = ref(false);  // trava para não rodar várias vezes
const { fitView } = useVueFlow();

// Funções da interface
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
  if (!codigoFonte) return;
}

function alternaConsole() {
  consoleAberto.value = !consoleAberto.value;
}

// funções de arqiuvos
function abrirArqiuvo() {
  inputArquivoEscondido.value.click();
}

function novoArquivo() {
  if (confirm("Deseja apagar o código atual e criar um novo?")) {
    codigoFonte.value = ''
    //atualizarLinhas()
    nosDaArvore.value = []
    linhasDaArvore.value = []
    listaErros.value = []
    logs.value = ["Novo arquivo criado."]
  }
}

function lerConteudoARquivo(event) {
  const arquivo = event.target.files[0];

  if (!arquivo) return;

  const leitor = new FileReader();

  leitor.onload = (e) => {
    codigoFonte.value = e.target.result;

    listaErros.value = [];
    logs.value = [`✅ Arquivo '${arquivo.name}' carregado com sucesso.`];
    atualizarLinhas();
  }

  leitor.readAsText(arquivo);

  event.target.value = '';
}

function salvarARquivo() {
  if (!codigoFonte.value || codigoFonte.value.trim === '') {
    alert("O editor está vazio. Não há nada para salvar.");
    return;
  }

  const blob = new Blob([codigoFonte.value], {type: 'text/plain;charset=utf-8'});

  const urlTemporaria = URL.createObjectURL(blob);

  // link <a> invisível
  const linkInvisivel = document.createElement('a');
  linkInvisivel.href = urlTemporaria;

  linkInvisivel.download = 'meu_programa.txt';

  // pendura o link na página, clica nele e depois o destrói
  document.body.appendChild(linkInvisivel);
  linkInvisivel.click();
  document.body.removeChild(linkInvisivel);

  // libera a memória da URL temporária
  URL.revokeObjectURL(urlTemporaria);

  logs.value.push("💾 Arquivo salvo com sucesso!");
}


// integração com o backend
async function compilar() {
  logs.value.push("Iniciando compilação...")
  mudarAba('tab-logs')

  try {
    const resposta = await fetch('http://localhost:8080/api/compilar', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({codigo: codigoFonte.value})
    })

    if (!resposta.ok) {
      throw new Error(`Erro do Servidor HTTP: ${resposta.status}`);
    }

    const dados = await resposta.json()

    if (dados.sucesso) {

      listaErros.value = []
      logs.value.push("✅ Compilação finalizada com sucesso!")
      listaTokens.value = dados.tokens || []
      logs.value.push("✅ Análise léxica concluída com sucesso!")
      mudarVisao('visao-lexemas')
      linhasDigitadas.value = codigoFonte.value.split('\n').length;
      arvoreCompleta.value = dados.arvoreSintatica;
      listaSimbolos.value = dados.tabelaSimbolos || [];
      codigoMepaGerado.value = dados.codigoMepaGerado || '';
      modoPassoPasso.value = false;

      processarArvore(dados.arvoreSintatica)
      // mudarVisao('visao-arvore')  // Abre a aba da arvore automaticamente
    } else {
      listaErros.value = dados.erros || []
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
  if (!noRaiz) {
    nosDaArvore.value = []
    linhasDaArvore.value = []
    setTimeout(() => {
      fitView();
    }, 1000);
    totalNos.value = 0
    return
  }

  let nodes = []
  let edges = []
  let idContador = 1

  // contador global que avança sempre que desenha uma folha
  // garante que uma folha nunca caia no mesmo espaço de outra
  let posicaoFolhaX = 0;

  function calcularPosicoes(no, nivelY) {
    if (!no) return null;

    let meuId = `node_${idContador++}`
    let meuX = 0;

    let ehTerminal = !no.filhos || no.filhos.length === 0;

    if (ehTerminal) {
      meuX = posicaoFolhaX; // se for folha, ganha a próxima posição X livre na tela
      posicaoFolhaX += 1;
    } else {
      let somaPosicaoFilhos = 0;
      let totalFilhosValidos = 0;

      no.filhos.forEach((filho) => {
        let infoFilho = calcularPosicoes(filho, nivelY + 1);
        if (infoFilho) {
          somaPosicaoFilhos += infoFilho.x;
          totalFilhosValidos++;

          edges.push({
            id: `e_${meuId}-${infoFilho.id}`,
            source: meuId,
            target: infoFilho.id,
            type: 'smoothstep',
            style: {strokeWidth: 2, stroke: '#000'}
          });
        }
      });

      if (totalFilhosValidos > 0) {
        meuX = somaPosicaoFilhos / totalFilhosValidos;
      } else {
        meuX = posicaoFolhaX;
        posicaoFolhaX += 1;
      }
    }
    // --- Definição dos Rótulos (Labels) ---
    let nodeStyle = {};
    let nodeLAbel = '';
    let simboloDoNo = no.simbolo || 'N/A';
    // Só utiliza o lexema se ele existir e for diferente do próprio símbolo para evitar duplicação
    let lexemaDoNo = (no.lexema && no.lexema.trim() !== '' && no.lexema !== simboloDoNo) ? no.lexema : '';

    // Se o símbolo não começar com "<", assume que é um terminal / folha
    let ehSimboloTerminal = !simboloDoNo.startsWith("<");

    if (simboloDoNo === 'EPSILON') {
      // Estilo específico para o EPSILON
      nodeLAbel = 'ε (Vazio)';
      nodeStyle = {
        backgroundColor: '#f1f5f9',
        color: '#94a3b8',
        border: '1px dashed #cbd5e1',
        borderRadius: '50%',
        padding: '10px',
        textAlign: 'center',
        fontSize: '12px',
        width: '80´px',
      };
    } else if (ehTerminal || ehSimboloTerminal) {
      if (lexemaDoNo) {
        nodeLAbel = `${lexemaDoNo}`;
      } else {
        nodeLAbel = simboloDoNo;
      }

      nodeStyle = {
        backgroundColor: '#fef08a',
        color: '#854d0e',
        fontWeight: 'bold',
        borderRadius: '8px',
        border: '2px solid #eab308',
        padding: '10px 15px',
        textAlign: 'center',
        fontSize: '14px'
      };
    } else {
      // Regras Não-Terminais da Gramática (ex: <comando>)
      nodeLAbel = simboloDoNo;
      nodeStyle = {
        backgroundColor: '#e0e7ff',
        color: '#1e40af',
        fontWeight: 'bold',
        borderRadius: '4px',
        border: '1px solid #93c5fd',
        padding: '8px 12px',
        textAlign: 'center',
        fontSize: '13px',
        maxWidth: '140px',
        whiteSpace: 'normal',
        wordBreak: 'break-word',
        overflowWrap: 'anywhere'
      };
    }

    nodes.push({
      id: meuId,
      position: {x: meuX * 160, y: nivelY * 130},
      data: {label: nodeLAbel},
      style: nodeStyle
    });

    return {id: meuId, x: meuX};

  }
  calcularPosicoes(noRaiz, 0);
  nosDaArvore.value = nodes;
  linhasDaArvore.value = edges;
  totalNos.value = nodes.length;
}

function podarArvore(noOriginal, limitePassos, contador = {atual: 0}) {

  if (!noOriginal || contador.atual > limitePassos) return null;

  const noClonado = {simbolo: noOriginal.simbolo, lexema: noOriginal.lexema, filhos: []};

  contador.atual++;

  if (noOriginal.filhos && noOriginal.filhos.length > 0) {
    for (let filho of noOriginal.filhos) {

      if (contador.atual > limitePassos) break;

      const filhoPodado = podarArvore(filho, limitePassos, contador);
      if (filhoPodado) {
        noClonado.filhos.push(filhoPodado);
      }
    }
  }
  return noClonado;
}

function contarNos(no) {
  if (!no) return 0;
  let contagem = 1;
  if (no.filhos) {
    no.filhos.forEach(filho => {
      contagem += contarNos(filho);
    })
  }
  return contagem;
}

function mostrarArvoreCompleta() {
  if (!arvoreCompleta.value) return;
  modoPassoPasso.value = false;
  processarArvore(arvoreCompleta.value);
}

function iniciarPassoAPasso() {
  if (!arvoreCompleta.value) return;
  modoPassoPasso.value = true;
  passoAtual.value = 0;

  totalPAssos.value = contarNos(arvoreCompleta.value) - 1;
  atualizarDEsenhoPasso();
}

function proximoPasso() {
  if (passoAtual.value < totalPAssos.value) {
    passoAtual.value++;
    atualizarDEsenhoPasso();
  }
}

function passoAnterior() {
  if (passoAtual.value > 0) {
    passoAtual.value--;
    atualizarDEsenhoPasso();
  }
}

function atualizarDEsenhoPasso() {
  const arvoreParcial = podarArvore(arvoreCompleta.value, passoAtual.value, {atual: 0});

  processarArvore(arvoreParcial);
}


// Função para formatar a regra visualmente (ex: <comando> -> if ...)
function formatarRegra(naoTerminal, producao) {
  if (!producao) return '';
  if (producao[0] === 'EPSILON') return `${naoTerminal} → ε`;
  return `${naoTerminal} → ${producao.join(' ')}`;
}

function formatarRegraTabela(naoTerminal, producao) {
  if (!producao) return '';
  if (producao[0] === 'EPSILON') return `${naoTerminal} → ε`;
  return `${naoTerminal} → ${producao.join(' ')}`;
}

async function carregarTabelaSintaticaAPI() {
  try {
    const resposta = await fetch('http://localhost:8080/api/tabela-sintatica');
    const dados = await resposta.json();
    matrizSintatica.value = dados;

    linhasNaoTerminais.value = Object.keys(dados);

    const setTerminais = new Set();
    for (const nt in dados) {
      for (const terminal in dados[nt]) {
        setTerminais.add(terminal);
      }
    }
    colunasTerminais.value = Array.from(setTerminais);
  } catch (error) {
    console.error("Erro ao carregar a tabela sintática:", error);
  }
}


function iniciarExecucaoMEPA() {
  if (!codigoMepaGerado.value) {
    alert("Compile o código primeiro para gerar o Bytecode MEPA.");
    return;
  }

  // divide o texto num array de linhas
  instrucoesMepa.value = codigoMepaGerado.value.split('\n').filter(linha => linha.trim() !== '');

  // limpa a máquina
  memoriaMepa.value = [];
  consoleSaida.value = [];
  ponteiroInstrucao.value = 0;
  isExecutando.value = true;

  // executa até encontrar PARA
  executarProximoPasso();
}

function executarProximoPasso() {
  if (ponteiroInstrucao.value >= instrucoesMepa.value.length || !isExecutando.value) {
    isExecutando.value = false;
    return;
  }

  let linhaAtual = instrucoesMepa.value[ponteiroInstrucao.value].trim();

  // se a linha for um rótulo ignora a operação e avança
  if (linhaAtual.includes(": NADA")) {
    ponteiroInstrucao.value++;
    setTimeout(executarProximoPasso, 1000);
    return;
  }

  let partes = linhaAtual.split(' ');
  let comando = partes[0];
  let parametro = partes.length > 1 ? parseInt(partes[1]) : null;

  let s = memoriaMepa.value.length - 1; // topo da Pilha (S)

  switch (comando) {
    case "INPP":
      break;

    case "AMEM":
      for (let i = 0; i < parametro; i++) {
        memoriaMepa.value.push(0); // inicializa com lixo (0)
      }
      break;

    case "CRCT":
      memoriaMepa.value.push(parametro);
      break;

    case "CRVL":
      memoriaMepa.value.push(memoriaMepa.value[parametro]);
      break;

    case "ARMZ":
      memoriaMepa.value[parametro] = memoriaMepa.value.pop();
      break;

    case "SOMA":
      memoriaMepa.value[s - 1] = memoriaMepa.value[s - 1] + memoriaMepa.value.pop();
      break;

    case "SUBT":
      memoriaMepa.value[s - 1] = memoriaMepa.value[s - 1] - memoriaMepa.value.pop();
      break;

    case "MULT":
      memoriaMepa.value[s - 1] = memoriaMepa.value[s - 1] * memoriaMepa.value.pop();
      break;

    case "DIVI":
      memoriaMepa.value[s - 1] = Math.floor(memoriaMepa.value[s - 1] / memoriaMepa.value.pop());
      break;

    case "CMAI":
      memoriaMepa.value[s - 1] = memoriaMepa.value[s - 1] > memoriaMepa.value.pop() ? 1 : 0;
      break;

    case "CMEN":
      memoriaMepa.value[s - 1] = memoriaMepa.value[s - 1] < memoriaMepa.value.pop() ? 1 : 0;
      break;

    case "CMIG":
      memoriaMepa.value[s - 1] = memoriaMepa.value[s - 1] === memoriaMepa.value.pop() ? 1 : 0;
      break;

    case "DSVF":
      let condicaoFalsa = memoriaMepa.value.pop() === 0;
      if (condicaoFalsa) {
        // encontra a linha que tem o rótulo
        let rotulo = partes[1];
        let indexAlvo = instrucoesMepa.value.findIndex(l => l.startsWith(rotulo + ":"));
        if (indexAlvo !== -1) {
          ponteiroInstrucao.value = indexAlvo; // pula o IP para lá
          setTimeout(executarProximoPasso, 1000);
          return; // retorna para evitar o IP++ em baixo
        }
      }
      break;

    case "DSVS":
      let rotuloSalto = partes[1];
      let indexSalto = instrucoesMepa.value.findIndex(l => l.startsWith(rotuloSalto + ":"));
      if (indexSalto !== -1) {
        ponteiroInstrucao.value = indexSalto;
        setTimeout(executarProximoPasso, 1000);
        return;
      }
      break;

    case "LEIT":
      let valorEntrada = prompt("Programa solicitou entrada de dados (READ):", "0");
      memoriaMepa.value.push(parseInt(valorEntrada) || 0);
      break;

    case "IMPR":
      consoleSaida.value.push(memoriaMepa.value.pop());
      break;

    case "PARA":
      isExecutando.value = false;
      return;
  }

  ponteiroInstrucao.value++;
  setTimeout(executarProximoPasso, 1000);
}


// Inicializa o tema ao carregar a página
onMounted(async () => {
  document.documentElement.setAttribute('data-theme', temaAtual.value)
  atualizarLinhas()
  await carregarTabelaSintaticaAPI();

});
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
  --texto-principal: #1e293b;
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
  --texto-principal: #f8fafc;
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
  --btn-action-arvore: #5555;
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
  --texto-principal: #f8fafc;
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
  --btn-floating-action: #5555;
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

.btn-compilar .btn-floating-action {
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


.editor-container .cm-editor {
  height: 100%;
  width: 100%;
  outline: none !important; /* Remove a borda azul/preta de foco padrão */
  border: 1px solid var(--borda); /* Usa a variável do tema */
  border-radius: 8px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
}

/* barra lateral esquerda (calha de números) */
.cm-gutters {
  background-color: #f8fafc !important;
  border-right: 1px solid var(--borda) !important;
  color: #94a3b8 !important;
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

/* destaque da linha ativa - onde o cursor está */
.cm-activeLine {
  background-color: rgba(226, 232, 240, 0.4) !important;
}

.cm-activeLineGutter {
  background-color: #e2e8f0 !important;
  color: #1e293b !important;
  font-weight: bold;
}

/* comportamento do scroll interno do editor */
.cm-scroller {
  overflow: auto !important;
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

.btn-toggle-console {
  background: transparent;
  border: 1px solid #ccc;
  color: #555;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.btn-toggle-console:hover {
  background: #e2e8f0;
}

.btn-floating-action {

}

/* quando o console está minimizado, ele fica só com a barra do título */
.console-minimizado {
  height: 40px !important; /* altura apenas para caber o botão e o título */
  overflow: hidden;
}


.main-display-area {
  flex-grow: 1;
  overflow: hidden; /* evita que a árvore vaze se crescer demais */
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

/* Container padrão da árvore */
.arvore-container {
  position: relative;
  height: 100%;
  border: 1px solid var(--borda);
  border-radius: 12px;
  overflow: hidden;
  background: white;
  transition: all 0.3s ease;
}

/* estado maximizado */
.arvore-container.maximized-view {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999; /* Fica acima de tudo (sidebar, toolbar, console) */
  border-radius: 0;
}

/* Botão flutuante dentro da árvore */
.btn-floating-action {
  position: absolute;
  top: 15px;
  right: 60px; /* Posicionado para não colidir com a barra lateral */
  z-index: 100;
  padding: 8px 16px;
  background: white;
  border: 1px solid var(--borda);
  border-radius: 6px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  font-weight: bold;
  color: var(--texto-principal);
}

.btn-floating-action:hover {
  background: #f8fafc;
  border-color: #94a3b8;
}

.tabela-container {
  margin-top: 2rem;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  padding: 1rem;
}

.scroll-wrapper {
  overflow-x: auto; /* Permite rolagem horizontal! */
  max-height: 500px; /* Limita a altura com rolagem vertical */
  overflow-y: auto;
}

.tabela-ll1 {
  width: 100%;
  border-collapse: collapse;
  white-space: nowrap; /* Impede a quebra de texto das regras */
  font-size: 13px;
}

.tabela-ll1 th, .tabela-ll1 td {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
  text-align: center;
}

.tabela-ll1 thead th {
  background-color: #1e293b;
  color: #f8fafc;
  position: sticky;
  top: 0;
  z-index: 10;
}

.celula-fixa {
  background-color: #f1f5f9;
  font-weight: bold;
  position: sticky;
  left: 0; /* Trava a primeira coluna na rolagem */
  z-index: 5;
  border-right: 2px solid #cbd5e1 !important;
}

.non-terminal {
  color: #0f172a;
  text-align: right !important;
}

.cabecalho-token {
  color: #38bdf8;
  font-family: monospace;
}

.regra-texto {
  background-color: #dcfce7;
  color: #166534;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.celula-vazia {
  background-color: #fafafa;
  color: #cbd5e1;
}

</style>