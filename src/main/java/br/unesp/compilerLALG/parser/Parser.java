package br.unesp.compilerLALG.parser;

import br.unesp.compilerLALG.lexer.TipoToken;
import br.unesp.compilerLALG.lexer.Token;
import br.unesp.compilerLALG.parser.ast.ASTnode;
import br.unesp.compilerLALG.parser.ast.BinOpNode;
import br.unesp.compilerLALG.parser.ast.NumNode;

import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        System.out.println("Tokens recebidos para parsing:");
        for (Token token : tokens) {
            System.out.println("Token: " + token.getLexema() + " Tipo: " + token.getToken());
        }   
    }

    public ASTnode parse() {
        return expressao();
    }


    public ASTnode fator() {
        Token tokenAtual = tokens.get(pos);
        if (tokenAtual.getToken().equals("NUM")) {
            pos++; // "come" o número
            return new NumNode(Double.parseDouble(tokenAtual.getLexema()));
        } else if (tokenAtual.getToken().equals("AP")) {
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
        throw new RuntimeException("Erro de Sintaxe: Token inesperado '" + tokenAtual.getLexema());
    }

    // resolve multiplicação e divisão
    public ASTnode termo() {
        ASTnode noEsquerda = fator();

        while (pos < tokens.size() && (tokens.get(pos).getToken().equals("OPMULT") || tokens.get(pos).getToken().equals("OPDIV"))) {

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

        while (pos < tokens.size() && (tokens.get(pos).getToken().equals("OPADD") || tokens.get(pos).getToken().equals("OPSUB"))) {

            String operador = tokens.get(pos).getLexema();
            pos++;

            ASTnode noDireita = termo();

            noEsquerda = new BinOpNode(noEsquerda, operador, noDireita);
        }
        return noEsquerda;
    }

    private void eat(TipoToken tipoEsperado) {
        if (pos < tokens.size() && tokens.get(pos).getToken().equals(tipoEsperado.name())) {
            pos++;
        } else {
            // Lógica para lidar com erros de sintaxe
        }
    }
}
