// Função para atualizar as linhas do editor em tempo real
function atualizarEditor() {
    const textarea = document.getElementById('codigo-fonte');
    const lines = textarea.value.split('\n').length;
    const lineNumbers = document.getElementById('line-numbers');
    const lblLinhas = document.getElementById('lbl-linhas');

    // Atualiza os números à esquerda
    lineNumbers.innerHTML = Array(lines).fill(0).map((_, i) => i + 1).join('<br>');
    // Atualiza o cartão do Dashboard com o núemro de linhas digitadas
    lblLinhas.innerHTML = `${lines} <span class="card-label">linhas digitadas</span>`;

}

// Sincroniza a rolagem dos números com o texto
function sincronizarScroll() {
    document.getElementById('line-numbers').scrollTop = document.getElementById('codigo-fonte').scrollTop;
}

// Sistema de abas inferior
function mudarAba(tabId) {
    // Desativa todas as abas e conteúdos
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));


    // Mostra o conteúdo da aba solicitada
    document.getElementById(tabId).classList.add('active');

    // Procura o botão da aba correspondente e ativa-o visualmente
    const botaoAba = document.querySelector(`.tab[onclick*="${tabId}"]`);
    if (botaoAba) {
        botaoAba.classList.add('active');
    }
}

// Atalho de teclado Ctrl + Enter
document.addEventListener('keydown', function (e) {

    if (e.ctrlKey && e.key === 'Enter') {
        compilar().then(() => {
        }).catch(err => console.error(err));

    }
});

// Chamada à API Spring Boot
async function compilar() {
    const codigoTexto = document.getElementById('codigo-fonte').value;
    const painelLogs = document.getElementById('painel-logs');
    const painelErros = document.getElementById('painel-erros');
    const tabelaCorpo = document.getElementById('tabela-corpo');
    const lblTokens = document.getElementById('lbl-tokens');

    // Reset UI
    painelLogs.innerHTML = '<div class="log-entry">Conectando com o compilador...</div>';
    painelErros.innerHTML = '<div style="color: var(--texto-cinza); text-align: center;"></div>';
    tabelaCorpo.innerHTML = '';
    lblTokens.innerHTML = `0 <span class="card-label">tokens</span>`;

    try {
        // Comunica com a porta 8080 do Spring Boot
        const resposta = await fetch('http://localhost:8080/api/compilar', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({codigo: codigoTexto})
        });

        const dados = await resposta.json();

        if (dados.sucesso) {
            painelLogs.innerHTML = `<div class="log-entry sucesso">✓ ${dados.mensagem}</div>`;
            //mudarAba('tab-logs'); // Foca nos logs

            if (dados.tokens) {
                // Remove o EOF da contagem visual para o usuário
                const tokensReais = dados.tokens.filter(t => t.token !== 'EOF');

                // Atualiza o Dashboard
                lblTokens.innerHTML = `${tokensReais.length} <span class="card-label">tokens</span>`;

                // Preenche a Tabela de lexemas
                tokensReais.forEach(t => {
                    tabelaCorpo.innerHTML += `
                        <tr>
                            <td style="font-weight: 600;">${t.lexema}</td>
                            <td><span class="token-tag">${t.token}</span></td>
                            <td>${t.linha}</td>
                            <td>${t.colunaInicial}</td>
                            <td>${t.colunaFinal}</td>
                        </tr>`;
                });

                const tabelaSimbolosCorpo = document.getElementById('tabela-simbolos-corpo');
                tabelaSimbolosCorpo.innerHTML = '';
                const identificadoresUnicos = new Set();
                const simbolos = [];

                tokensReais.forEach(t => {
                    if (t.token === "IDENTIFICADOR" && !identificadoresUnicos.has(t.lexema)) {
                        identificadoresUnicos.add(t.lexema);
                        simbolos.push(t.lexema);
                    }
                });
                document.getElementById('lbl-simbolos').innerHTML = `${simbolos.length} <span class="card-label">símbolos</span>`;

                // Desenha os identificadores na Tabela de Símbolos
                if (simbolos.length > 0) {
                    simbolos.forEach(simbolo => {
                        tabelaSimbolosCorpo.innerHTML += `
                            <tr>
                                <td style="font-weight: 600; color: var(--cor-primaria);">${simbolo}</td>
                                <td><span style="color: var(--texto-cinza); font-style: italic;">Aguardando Parser...</span></td>
                                <td><span style="color: var(--texto-cinza); font-style: italic;">Aguardando Parser...</span></td>
                            </tr>`;
                    });
                } else {
                    tabelaSimbolosCorpo.innerHTML = `<tr><td colspan="3" style="text-align: center; color: var(--texto-cinza);">Nenhum identificador encontrado no código.</td></tr>`;
                }
            }
        } else {
            // Trata o Erro Léxico
            dados.lsitaErros.forEach((erro) => {
                console.log(erro)
                painelErros.innerHTML += `<div class="log-entry erro">✕ ${erro}</div>`
            })
            // painelErros.innerHTML = `<div class="log-entry erro">✕ ${dados.mensagem}</div>`;
            // painelLogs.innerHTML = `<div class="log-entry erro">Compilação abortada com erros.</div>`;
            mudarAba('tab-erros'); // Foca na aba de erros automaticamente

            // Força a tela a voltar para o código-fonte para o utilizador corrigir o erro
            mostrarVisao('visao-editor', 'card-editor');
        }
    } catch (erro) {
        painelErros.innerHTML = `<div class="log-entry erro">✕ Erro de Conexão: O servidor Spring Boot está ligado? (${erro.message})</div>`;
        mudarAba('tab-erros');
        mostrarVisao('visao-editor', 'card-editor');
    }
}

// Inicializa o contador de linhas ao carregar a página
window.onload = atualizarEditor;

function novoArquivo() {

    if (confirm("Deseja criar um novo arquivo? O código atual não salvo será perdido")) {
        document.getElementById('codigo-fonte').value = '';
        atualizarEditor();

        // Limpa os painéis e o dashboard
        // Limpa os logs e tabelas
        document.getElementById('painel-logs').innerHTML = '<div class="log-entry">Novo arquivo criado.</div>';
        document.getElementById('painel-erros').innerHTML = '<div style="color: var(--texto-cinza); text-align: center;"></div>';
        document.getElementById('tabela-corpo').innerHTML = '';
        document.getElementById('lbl-tokens').innerHTML = `0 <span class="card-label">tokens</span>`;
        mudarAba('tab-logs');
    }
}

function abrirArquivo(event) {

    const arquivo = event.target.files[0];
    if (!arquivo) return;

    const leitor = new FileReader();

    leitor.onload = function (e) {
        document.getElementById('codigo-fonte').value = e.target.result;
        atualizarEditor();
        document.getElementById('painel-logs').innerHTML = `<div class="log-entry sucesso">✓ Arquivo '${arquivo.name}' carregado com sucesso.</div>`;
        //mudarAba('tab-logs');
    };

    leitor.readAsText(arquivo);

    event.target.value = '';
}

function salvarArquivo() {
    const texto = document.getElementById('codigo-fonte').value;

    if (texto.trim === '') {
        alert("O editor está vazio");
        return;
    }

    // Cria um blob com o texto
    const blob = new Blob([texto], {type: 'text/plain'});

    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'meu_programa.lalg';

    document.body.appendChild(link);
    link.click();

    // Limpa a memória
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
}

// controle de visualização principal (Views)
function mostrarVisao(visaoId, cardId) {
    // Esconde todas as visões da área principal
    document.getElementById('visao-editor').style.display = 'none';
    document.getElementById('visao-lexemas').style.display = 'none';
    document.getElementById('visao-simbolos').style.display = 'none';

    // Mostra apenas a visão solicitada
    if(visaoId === 'visao-editor') {
        document.getElementById(visaoId).style.display = 'flex';
    } else {
        document.getElementById(visaoId).style.display = 'block';
    }

    // Remove a classe 'destaque' de todos os cartões do dashboard
    document.querySelectorAll('.card-clickable').forEach(card => {
        card.classList.remove('destaque');
    });

    // Pinta o cartão que acabou de ser clicado
    document.getElementById(cardId).classList.add('destaque');
}

// Escolha de temas de cores
const iconesTemas = {
    'claro': '☀️',
    'escuro': '🌙',
    'dracula': '🧛'
};


// Mostra ou esconde o menu
function toggleMenuTemas(event) {
    event.stopPropagation(); // Impede que o clique feche o menu imediatamente
    document.getElementById('menu-temas').classList.toggle('show');
}

function setTema(nomeTema) {
    document.documentElement.setAttribute('data-theme', nomeTema);
    document.getElementById('icone-tema').innerText = iconesTemas[nomeTema];

    localStorage.setItem('temaCompiladorLALG', nomeTema);

    document.getElementById('menu-temas').classList.remove('show');
}

// fecha o menu se o utilizador clicar em qqr outro lugar
document.addEventListener('click', function (event) {
    const menu = document.getElementById('menu-temas');
    if (menu.classList.contains('show') && !event.target.closest('.theme-container')) {
        menu.classList.remove('show');
    }
});

// carrega automaticamente o tema salvo ao abrir a página
document.addEventListener('DOMContentLoaded', () => {
    let temaSalvo = localStorage.getItem('temaCompiladorLALG');
    if (!temaSalvo) temaSalvo = 'claro';

    document.documentElement.setAttribute('data-theme', temaSalvo);
    document.getElementById('icone-tema').innerText = iconesTemas[temaSalvo];

});