package br.unesp.compilerLALG.core.lexer;

public class Token {

    private String token;
    private String lexema;
    private int linha;
    private int colunaInicial;
    private int colunaFinal;

    public Token(String tipo, String value, int linha, int colunaInicial, int colunaFinal) {
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

    public int getLinha() {
        return linha;
    }

    public void setLinha(int linha) {
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

    public boolean getType() {
        return token.equals(TipoToken.NUM.name()) ||
                token.equals(TipoToken.OPSOMA.name()) ||
                token.equals(TipoToken.OPSUB.name()) ||
                token.equals(TipoToken.OPMUL.name()) ||
                token.equals(TipoToken.OPDIV.name());
    }
}
