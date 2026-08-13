package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.exception.CompilerException;

import java.util.*;

public class Parser {

    private final List<Token> tokens;
    private Token tokenAtual;
    private noArvore raizArvore;

    private final TabelaSintatica tabelaSintatica;
    private final Stack<String> pilha;


    // Lista para guardar os erros sintáticos (Panic Mode)
    private final List<CompilerException.SyntaxException> listaErrosSintaticos;


    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        if (!tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        }
        this.listaErrosSintaticos = new ArrayList<>();
        this.tabelaSintatica = GramaticaLALG.criarTabela();
        this.pilha = new Stack<>();

    }

    public noArvore analisar() {
        // Implementação da análise sintática
        Stack<noArvore> pilha = new Stack<>();

        noArvore noEOF = new noArvore("EOF");
        raizArvore = new noArvore("<programa>");

        pilha.push(noEOF); // Símbolo de fim de entrada
        pilha.push(raizArvore); // Símbolo inicial da gramática

        int ponteiroToken = 0;
        Token tokenAtual = tokens.get(ponteiroToken);

//        System.out.println("DEBUG - Topo da pilha: " + pilha.peek());
//        System.out.println("DEBUG - Primeiro Token [Tipo: '" + tokenAtual.getTipo() + "' | Lexema: '" + tokenAtual.getLexema() + "']");

        while (!pilha.isEmpty() && !pilha.peek().getSimbolo().equals("EOF")) {
            noArvore X =pilha.peek();
            String simboloX = X.getSimbolo();
            String a = traduzirToken(tokenAtual); // Tipo do token atual

            //System.out.println("DEBUG - Pilha: " + simboloX + " | Lexer enviou: [Tipo: " + tokenAtual.getTipo() + ", Lexema: '" + tokenAtual.getLexema() + "'] | Traduzido para: " + a);

            if (simboloX.startsWith("<") && simboloX.endsWith(">")) {
                // X é um não-terminal
                List<String> regra = tabelaSintatica.obterRegra(simboloX, a);
                if (regra != null) {
                    pilha.pop(); // Remove o não-terminal da pilha

                    if (!regra.get(0).equals("EPSILON")) {
                        noArvore[] arrayFilhos = new noArvore[regra.size()];
                        // cria os nós e pendura no pai (mantendo a ordem correta da esquerda pra direita)
                        for (int i = 0; i < regra.size(); i++) {
                            arrayFilhos[i] = new noArvore(regra.get(i));
                            X.addFilho(arrayFilhos[i]);
                        }
                        // empilha de trás pra frente para o parser ler da esquerda pra direita
                        for (int i = regra.size() - 1; i >= 0; i--) {
                            pilha.push(arrayFilhos[i]);
                        }
                    } else {
                        X.addFilho(new noArvore("EPSILON"));
                    }
                } else {
                    listaErrosSintaticos.add(new CompilerException.SyntaxException(
                            "Símbolo inesperado ('" + tokenAtual.getLexema() + "') ao construir a regra " + simboloX,
                            tokenAtual.getLinha(),
                            tokenAtual.getColunaFinal()
                    ));

                    // MODO PÂNICO (Recuperação de Erro - Aula 07)
                    Set<String> follow = ConjuntosLALG.obterFollow(simboloX);

                    if (follow.contains(a)) {
                        // O token atual está no FOLLOW de X.
                        // Ação SINC: Remove X da pilha e tenta continuar
                        System.err.println("-> Ação SINC: Ignorando construção de " + simboloX);
                        pilha.pop();
                    } else {
                        // O token atual é lixo perdido. Pula o token da entrada.
                        System.err.println("-> Descartando token: " + a);
                        if (a.equals("EOF")) break;
                        ponteiroToken++;
                        if(ponteiroToken < tokens.size()) tokenAtual = tokens.get(ponteiroToken);
                    }
                }
            } else {
                // é um terminal - tenta fazer match com o token atual
                if (simboloX.equals(a)) {
                    // guarda o texto digitado dentro da folha da arvore
                    X.setLexema(tokenAtual.getLexema());
                    X.setLinha(tokenAtual.getLinha());

                    pilha.pop(); // Remove o terminal da pilha
                    ponteiroToken++;
                    if (ponteiroToken < tokens.size()) tokenAtual = tokens.get(ponteiroToken);
                } else {
                    listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                            simboloX, a, tokenAtual.getLexema(), tokenAtual.getLinha(), tokenAtual.getColunaFinal()
                    ));
                    // MODO PÂNICO
                    pilha.pop(); // Remove o terminal da pilha, tentando sincronizar
                }
            }
        }
        if (tokenAtual.getTipo().equals("EOF") && traduzirToken(tokenAtual).equals("EOF")) {
            listaErrosSintaticos.add(new CompilerException.CodigoExtraException(
                    tokenAtual.getLexema(), tokenAtual.getLinha(), tokenAtual.getColunaFinal()
            ));
        } else {
            System.out.println("Compilação finalizada com erros sintáticos.");
        }
        return raizArvore;
    }

    public boolean temErros() {
        System.out.println("DEBUG: Análise sintática encontrou " + listaErrosSintaticos.size() + " erros.");
        return listaErrosSintaticos.isEmpty();
    }

    public List<CompilerException.SyntaxException> getErros() {
        return listaErrosSintaticos;
    }

    private String traduzirToken(Token token) {
        String tipo = token.getTipo();
        String lexema = token.getLexema().toLowerCase();

        // Tratamento para Fim de Arquivo
        if (tipo.equals("EOF") || tipo.equals("$")) return "EOF";

        // Identificadores pré-declarados da LALG
        // Transforma essas palavras-chave de volta em identificadores para a tabela
        if (lexema.equals("read") || lexema.equals("write") ||
                lexema.equals("true") || lexema.equals("false")) {
            return "identificador";
        }

        // Tratamento para Identificadores
        if (tipo.equals("IDENTIFICADOR") || tipo.equals("ID")) return "identificador";

        // Tratamento para Números
        if (tipo.equals("NUMERO") || tipo.equals("NUM")) {
            return "numero";
        }

        // 5. Para o resto (if, begin, program, ;, :=)
        return lexema;
    }

    public noArvore getRaizArvore() {
        return raizArvore;
    }
}