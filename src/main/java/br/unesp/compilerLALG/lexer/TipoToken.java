package br.unesp.compilerLALG.lexer;

import java.util.regex.Pattern;

public enum TipoToken {
    // 1. Palavras Reservadas
    PROGRAM("program\\b"), BEGIN("begin\\b"), END("end\\b"),
    PROCEDURE("procedure\\b"), VAR("var\\b"), IF("if\\b"),
    THEN("then\\b"), ELSE("else\\b"), WHILE("while\\b"),
    DO("do\\b"), INT("int\\b"), BOOLEAN("boolean\\b"),
    READ("read\\b"), WRITE("write\\b"), TRUE("true\\b"),
    FALSE("false\\b"), OPDIV("div\\b"), OPAND("and\\b"),
    OPOR("or\\b"), OPNOT("not\\b"),

    // 2. Identificadores e Números
    IDENTIFICADOR("[a-zA-Z_][a-zA-Z0-9_]*"),
    NUM("[0-9]+"),

    // 3. Símbolos Compostos (Ganha prioridade de leitura)
    ATRIBUICAO(":="),
    OPDIF("<>"),
    OPMENORIGUAL("<="),
    OPMAIORIGUAL(">="),

    // 4. Símbolos Simples
    OPMENOR("<"),
    OPMAIOR(">"),
    OPIGUAL("="),
    OPSOMA("\\+"),
    OPSUB("-"),
    OPMUL("\\*"),
    PONTOVIRGULA(";"),
    DOISPONTOS(":"),
    VIRGULA(","),
    PONTO("\\."),
    ABREPAR("\\("),
    FECHAPAR("\\)");

    private final Pattern padrao;

    TipoToken(String regex) {
        this.padrao = Pattern.compile("^(" + regex + ")");
    }

    public Pattern getPadrao() {
        return padrao;
    }
}