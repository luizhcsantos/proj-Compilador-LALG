package br.unesp.compilerLALG.lexer;

public class Token {

    private String token;
    private String lexema;
    private String linha;
    private int colunaInicial;
    private int colunaFinal;

    public Token(String tipo, String value, String linha, int colunaInicial, int colunaFinal) {
        this.token = tipo;
        this.lexema = value;
        this.linha = linha;
        this.colunaInicial = colunaInicial;
        this.colunaFinal = colunaFinal;
    }

    public Token(TipoToken tipoToken, String value) {
        this.token = tipoToken.name();
        this.lexema = value;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLexema() {
        return lexema;
    }

    public void setLexema(String lexema) {
        this.lexema = lexema;
    }

    public String getLinha() {
        return linha;
    }

    public void setLinha(String linha) {
        this.linha = linha;
    }

    public int getColunaFinal() {
        return colunaFinal;
    }

    public void setColunaFinal(int colunaFinal) {
        this.colunaFinal = colunaFinal;
    }

    public int getColunaInicial() {
        return colunaInicial;
    }

    public void setColunaInicial(int colunaInicial) {
        this.colunaInicial = colunaInicial;
    }
}
