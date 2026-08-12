package br.unesp.compilerLALG.core.lexer;

public class Token {

    private String tipo;
    private String lexema;
    private int linha;
    private int colunaInicial;
    private int colunaFinal;

    public Token(String tipo, String value, int linha, int colunaInicial, int colunaFinal) {
        this.tipo = tipo;
        this.lexema = value;
        this.linha = linha;
        this.colunaInicial = colunaInicial;
        this.colunaFinal = colunaFinal;
    }

    public Token(TipoToken tipoToken, String value) {
        this.tipo = tipoToken.name();
        this.lexema = value;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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
        return tipo.equals(TipoToken.NUM.name()) ||
                tipo.equals(TipoToken.OPSOMA.name()) ||
                tipo.equals(TipoToken.OPSUB.name()) ||
                tipo.equals(TipoToken.OPMUL.name()) ||
                tipo.equals(TipoToken.OPDIV.name());
    }


}
