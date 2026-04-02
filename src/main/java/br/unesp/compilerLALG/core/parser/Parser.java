package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.ASTnode;
import br.unesp.compilerLALG.exception.CompilerException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

public class Parser {

    private final List<Token> tokens;
    private int posicaoAtual;
    private Token tokenAtual;

    // A Pilha Sintática e a Tabela M
    private final Stack<String> pilha = new Stack<>();
    private Map<String, Map<String, List<String>>> tabelaSintatica;

    // Lista para guardar os erros sintáticos
    private final List<CompilerException.SyntaxException> listaErrosSintaticos = new ArrayList<>();

    // Conjunto de todos os Terminais da linguagem LALG (Os tokens gerados pelo Lexer)
    private final Set<String> TERMINAIS = Set.of(
            "PROGRAM", "IDENTIFICADOR", "PONTOVIRGULA", "PONTO", "INT", "BOOLEAN",
            "PROCEDURE", "BEGIN", "END", "VAR", "DOISPONTOS", "VIRGULA", "READ",
            "WRITE", "ATRIBUICAO", "IF", "THEN", "ELSE", "WHILE", "DO", "NUM",
            "ABREPAR", "FECHAPAR", "OPSOMA", "OPSUB", "OPMUL", "OPDIV", "OPAND",
            "OPOR", "OPNOT", "TRUE", "FALSE", "OPIGUAL", "OPDIF", "OPMENOR",
            "OPMENORIGUAL", "OPMAIOR", "OPMAIORIGUAL", "EOF"
    );

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.posicaoAtual = 0;

        if (!tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        } else {
            this.tokenAtual = new Token("EOF", "EOF", 0, -1, -1);
        }

        carregarTabelaDoJson();

        // Inicializa a Pilha: Fundo é EOF, Topo é o símbolo inicial da gramática
        pilha.push("EOF");
        pilha.push("PROGRAMA"); // Certifique-se que no seu JSON o ponto de partida chama-se "PROGRAMA"
    }

    private void carregarTabelaDoJson() {
        try {
            // Lê o arquivo tabela.json da pasta src/main/resources
            InputStream is = getClass().getResourceAsStream("/tabela.json");
            if (is == null) {
                throw new RuntimeException("Arquivo tabela.json não encontrado na pasta resources.");
            }

            ObjectMapper mapper = new ObjectMapper();
            tabelaSintatica = mapper.readValue(is, new TypeReference<Map<String, Map<String, List<String>>>>() {});

            // System.out.println("Tabela sintática carregada com sucesso!");
        } catch (Exception e) {
            throw new RuntimeException("Erro fatal ao carregar a tabela sintática: " + e.getMessage());
        }
    }

    private void avancar() {
        posicaoAtual++;
        if (posicaoAtual < tokens.size()) {
            tokenAtual = tokens.get(posicaoAtual);
        } else {
            tokenAtual = new Token("EOF", "EOF", 0, -1, -1);
        }
    }

    private boolean isTerminal(String simbolo) {
        return TERMINAIS.contains(simbolo);
    }

    // ==========================================
    // O MOTOR PRINCIPAL (Analisador Não-Recursivo)
    // ==========================================
    public ASTnode analisar() {
        // O algoritmo base de Tabela e Pilha
        while (!pilha.isEmpty()) {
            String topoPilha = pilha.peek();
            String tipoTokenAtual = tokenAtual.getToken();

            // 1. Condição de Vazio (Epsilon)
            // Se a regra mandou empilhar EPSILON, nós apenas desempilhamos e seguimos a vida.
            if (topoPilha.equals("EPSILON")) {
                pilha.pop();
                continue;
            }

            // 2. O Topo casou perfeitamente com o Token Atual (Sucesso!)
            if (topoPilha.equals(tipoTokenAtual)) {
                pilha.pop(); // Remove da pilha
                avancar();   // Lê o próximo token
            }
            // 3. O Topo é um Terminal, mas não é o que está no Token Atual (Erro Sintático)
            else if (isTerminal(topoPilha)) {
                registrarErroTerminalFaltando(topoPilha, tipoTokenAtual);

                // Panic Mode simples: Arranca o terminal problemático da pilha para tentar continuar
                pilha.pop();
            }
            // 4. O Topo é um Não-Terminal. Vamos consultar a Tabela M!
            else {
                Map<String, List<String>> linhaDaTabela = tabelaSintatica.get(topoPilha);

                // Existe uma transição válida para este token?
                if (linhaDaTabela != null && linhaDaTabela.containsKey(tipoTokenAtual)) {
                    List<String> producao = linhaDaTabela.get(tipoTokenAtual);

                    pilha.pop(); // Tira o Não-Terminal antigo
                    empilharReverso(producao); // Empilha a nova regra de trás para frente
                }
                else {
                    // Célula Vazia na Tabela! Erro Sintático Grave.
                    registrarErroComandoInvalido(tipoTokenAtual);

                    // Panic Mode simples: Avança o token para ver se o próximo encaixa em algo
                    avancar();
                }
            }
        }

        // Se a pilha esvaziou, mas ainda tem código não lido
        if (!tokenAtual.getToken().equals("EOF")) {
            listaErrosSintaticos.add(new CompilerException.CodigoExtraException(
                    tokenAtual.getLexema(), tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
        }

        return null; // O motor de Tabela e Pilha puro não gera AST diretamente, retorna null por enquanto.
    }

    private void empilharReverso(List<String> producao) {
        // Empilha de trás para a frente. Exemplo: regra A -> B C, empilha C, depois B.
        for (int i = producao.size() - 1; i >= 0; i--) {
            pilha.push(producao.get(i));
        }
    }

    // ==========================================
    // MÉTODOS DE ERRO E STATUS
    // ==========================================
    private void registrarErroTerminalFaltando(String esperado, String encontrado) {
        listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                esperado,
                encontrado,
                tokenAtual.getLexema(),
                tokenAtual.getLinha(),
                tokenAtual.getColunaInicial()
        ));
    }

    private void registrarErroComandoInvalido(String tokenInvalido) {
        listaErrosSintaticos.add(new CompilerException.ComandoInvalidoException(
                tokenAtual.getLexema(),
                tokenAtual.getLinha(),
                tokenAtual.getColunaInicial()
        ));
    }

    public List<CompilerException.SyntaxException> getErros() {
        return listaErrosSintaticos;
    }

    public boolean temErros() {
        return !listaErrosSintaticos.isEmpty();
    }
}