package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.ASTnode;
import br.unesp.compilerLALG.core.parser.ast.BinOpNode;
import br.unesp.compilerLALG.core.parser.ast.NumNode;
import br.unesp.compilerLALG.exception.CompilerException;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class Parser {

    private final List<Token> tokens;
    private int posicaoAtual;
    private Token tokenAtual;
    private int pos = 0;
    private Stack<String> pilha;
    private Map<String, Map<String, List<String>>> tabelaParse; // Tabela de Análise Sintática (LL(1))

    // Lista para guardar os erros sintáticos (Panic Mode)
    private final List<CompilerException.SyntaxException> listaErrosSintaticos = new ArrayList<>();


    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.posicaoAtual = 0;
        if (!tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        }
        inicializarTabela();

    }

    private void inicializarTabela() {

    }

    private void avancar() {
        posicaoAtual++;
        if (posicaoAtual < tokens.size()) {
            tokenAtual = tokens.get(posicaoAtual);
        } else {
            tokenAtual = new Token("EOF", "EOF", 0, -1, -1); // Marca o fim dos tokens
        }
    }

    public void analisar() {
        while (!pilha.isEmpty()) {
            String topo =  pilha.peek();
            String token = tokenAtual.getToken();

            if (topo.equals(token)) {
                pilha.pop();
                avancar();
            } else if (isTerminal(topo)) {
                tratarErro();
            } else {
                List<String> producao = consultarTabela(topo, token);
                if (producao != null) {
                    pilha.pop();
                    empilhaReverso(producao);
                } else {
                    tratarErro();
                }
            }

        }
    }

    private void empilhaReverso(List<String> producao) {

    }

    private List<String> consultarTabela(String topo, String token) {
        return null;
    }

    private void tratarErro() {

    }

    private boolean isTerminal(String topo) {
        return false;
    }

    private void match(String tipoEsperado) {
        if (tokenAtual.getToken().equals(tipoEsperado)) {
            avancar(); // se for o tipo correto, consome o token e avança
        } else {
            // Regista o erro
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    tipoEsperado,
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()
            ));
        }

    }

    private void sincronizar(@NonNull Set<String> followSet) {
        while (!followSet.contains(tokenAtual.getToken()) && !tokenAtual.getToken().equals("EOF")) {
            avancar();
        }
    }




    public ASTnode parse() {
        return expressao();
    }


    public ASTnode fator() {
        Token tokenAtual = tokens.get(pos);
        switch (tokenAtual.getToken()) {
            case "EOF" -> throw new RuntimeException("Expressão incompleta. Faltou um número.");
            case "NUM" -> {
                pos++; // "come" o número

                // Verifica se o Lexer mandou um texto vazio antes de converter
                String textoDoNumero = tokenAtual.getLexema();
                if (textoDoNumero == null || textoDoNumero.trim().isEmpty()) {
                    throw new RuntimeException("Erro interno no Lexer: Um número vazio foi gerado.");
                }

                try {
                    return new NumNode(Double.parseDouble(textoDoNumero));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Erro ao converter para número: '" + textoDoNumero + "'");
                }
            }
            case "AP" -> {
                pos++; // "come" o parêntese de abertura


                // resolve tudo que está dentro do oarenteses
                ASTnode expressaoInterna = expressao();

                // verifica se fehcou o parêntese corretamente
                if (pos < tokens.size() && tokens.get(pos).getToken().equals("FP")) {
                    pos++; // "come" o parêntese de fechamento
                    return expressaoInterna;
                } else {
                    throw new RuntimeException("Erro de Sintaxe: Esperado ')' na coluna " + tokenAtual.getColunaFinal());
                }
            }
        }
        throw new RuntimeException("Erro de Sintaxe: Token inesperado '" + tokenAtual.getLexema());
    }

    // resolve multiplicação e divisão
    public ASTnode termo() {
        ASTnode noEsquerda = fator();

        while (pos < tokens.size() && (tokens.get(pos).getToken().equals("OPMUL") || tokens.get(pos).getToken().equals("OPDIV"))) {

            String operador = tokens.get(pos).getLexema();
            pos++;

            ASTnode noDireita = fator();

            noEsquerda = new BinOpNode(noEsquerda, operador, noDireita);

        }
        // Se entrou no while, devolve a árvore de multiplicação.
        // Se não entrou, devolve o número puro intacto.
        return noEsquerda;
    }

    // resolve adição e subtração
    public ASTnode expressao() {
        ASTnode noEsquerda = termo();

        while (pos < tokens.size() && (tokens.get(pos).getToken().equals("OPSOMA") || tokens.get(pos).getToken().equals("OPSUB"))) {

            String operador = tokens.get(pos).getLexema();
            pos++;

            ASTnode noDireita = termo();

            noEsquerda = new BinOpNode(noEsquerda, operador, noDireita);
        }
        return noEsquerda;
    }

    public List<CompilerException.SyntaxException> getErros() {
        return listaErrosSintaticos;
    }

    public boolean temErros() {
        return !listaErrosSintaticos.isEmpty();
    }
}
