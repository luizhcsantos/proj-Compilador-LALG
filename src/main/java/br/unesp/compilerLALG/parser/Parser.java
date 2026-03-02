package br.unesp.compilerLALG.parser;

import br.unesp.compilerLALG.lexer.TipoToken;
import br.unesp.compilerLALG.lexer.Token;
import br.unesp.compilerLALG.parser.ast.BinOpNode;
import br.unesp.compilerLALG.parser.ast.NumNode;

import java.util.List;

public class Parser {

    private List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void parse() {
        // Implementar a lógica de parsing aqui
        // Exemplo: criar uma AST a partir dos tokens
    }

    public NumNode fator() {

        for (Token tokenAtual : tokens) {
            if (tokenAtual.getToken().equals("NUM")) {
                pos++;
                return new NumNode(Double.parseDouble(tokenAtual.getLexema()));
            } else if (tokenAtual.getToken().equals("AP")) {
                tokenAtual = tokens.get(pos + 1);
                BinOpNode expressao = expressao();
                pos++;
                tokenAtual = tokens.get(tokens.indexOf(tokenAtual) + 1);
                if (tokenAtual.getToken().equals("FP")) {
                    return new NumNode();
                } else {
                    // Lógica para lidar com erros de sintaxe
                }

            }
        }
        return new NumNode(Double.parseDouble(tokens.get(tokens.size()).getLexema()));
    }

    public BinOpNode termo() {
        NumNode noEsquerda = fator();
        BinOpNode noAtual;
        while (tokens.stream().anyMatch(token -> token.getToken().equals("OPMUL") || token.getToken().equals("OPDIV"))) {
            String operador = tokens.get(pos).getToken();
            pos++;
            NumNode noDireita = fator();
            noAtual = new BinOpNode(noEsquerda, operador, noDireita);
            noEsquerda = noAtual;

        }
        return noAtual;
    }

    public BinOpNode expressao() {
        BinOpNode noEsquerda = termo();
        BinOpNode noAtual;
        while (tokens.stream().anyMatch(token -> token.getToken().equals("OPSOMA") || token.getToken().equals("OPSUB"))) {
            String operador = tokens.get(pos).getToken();
            pos++;
            BinOpNode noDireita = termo();
            noEsquerda = new BinOpNode(noEsquerda, operador, noDireita);
            noEsquerda = noAtual;
        }
        return noAtual;
    }

    private void eat(TipoToken tipoEsperado) {
        if (pos < tokens.size() && tokens.get(pos).getToken().equals(tipoEsperado.name())) {
            pos++;
        } else {
            // Lógica para lidar com erros de sintaxe
        }
    }
}
