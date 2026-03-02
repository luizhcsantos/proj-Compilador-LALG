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
                return new NumNode();
            } else if (tokenAtual.getToken().equals("AP")) {
                tokenAtual = tokens.get(tokens.indexOf(tokenAtual) + 1);
                expressao();
                tokenAtual = tokens.get(tokens.indexOf(tokenAtual) + 1);
                if (tokenAtual.getToken().equals("FP")) {
                    return new NumNode();
                } else {
                    // Lógica para lidar com erros de sintaxe
                }

            }
        }
        return new NumNode();
    }

    public BinOpNode termo() {
        NumNode fator = fator();
        while (tokens.stream().anyMatch(token -> token.getToken().equals("OPMUL") || token.getToken().equals("OPDIV"))) {
            // Lógica para lidar com multiplicação e divisão
            fator = fator();
        }
        return new BinOpNode();
    }

    public BinOpNode expressao() {
        BinOpNode termo = termo();
        while (tokens.stream().anyMatch(token -> token.getToken().equals("OPSOMA") || token.getToken().equals("OPSUB"))) {
            // Lógica para lidar com adição e subtração
            termo = termo();
        }
        return termo;
    }

    private void eat(TipoToken tipoEsperado) {
        if (pos < tokens.size() && tokens.get(pos).getToken().equals(tipoEsperado.name())) {
            pos++;
        } else {
            // Lógica para lidar com erros de sintaxe
        }
    }
}
