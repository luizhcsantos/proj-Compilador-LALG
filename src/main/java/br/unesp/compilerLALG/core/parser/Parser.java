package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.ASTnode;
import br.unesp.compilerLALG.core.parser.ast.BinOpNode;
import br.unesp.compilerLALG.core.parser.ast.NumNode;

import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int posicaoAtual;
    private Token tokenAtual;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.posicaoAtual = 0;
        if (!tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        }
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
        try {
            parsePrograma();
            //System.out.println("Análise sintática concluída com sucesso!");

            // Se terminou de analisar o programa, o próximo token DEVE ser o fim do arquivo.
            if (!tokenAtual.getToken().equals("EOF")) {
                throw new RuntimeException("Erro Sintático: Código extra após o fim do programa ('." + "') na linha " + tokenAtual.getLinha());
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }

    private void match(String tipoEsperado) {
        if (tokenAtual.getToken().equals(tipoEsperado)) {
            avancar(); // se for o tipo correto, consome o token e avança
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual.getLinha() +
                    ": Esperava '" + tipoEsperado + "', mas encontrou '" + tokenAtual.getLexema() + "'");
        }

    }

    // Programa ::= PROGRAM <identificador> ; <bloco> .
    public void parsePrograma() {
        match("PROGRAM");
        match("IDENTIFICADOR");
        match("PONTOVIRGULA");

        //parseBloco();

        match("PONTO");
    }

    // <comando> ::= <comando_atribuicao> | <comando_leitura> | <comando_escrita>
    public void parseComando() {
        // Se o token for 'read'...
        if (tokenAtual.getToken().equals("READ")) {
            //parseComandoLeitura();
        }
        // Se o token for 'write'...
        else if (tokenAtual.getToken().equals("WRITE")) {
            //parseComandoEscrita();
        }
        // Se for um identificador, é uma atribuição (ex: x := 10)
        else if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            //parseComandoAtribuicao();
        }
        else {
            throw new RuntimeException("Erro Sintático: Comando inválido na linha " + tokenAtual.getLinha());
        }
    }

    // <lista_comandos> ::= <comando> { ; <comando> }
    public void parseListaComandos() {
        parseComando(); // Tem que ter pelo menos um

        // Enquanto o próximo token for um ponto e vírgula...
        while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
            match("PONTOVIRGULA");
            parseComando();
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

}
