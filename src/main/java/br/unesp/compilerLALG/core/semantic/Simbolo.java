package br.unesp.compilerLALG.core.semantic;

public class Simbolo {

    private String simbolo;
    private String tipo;
    private String categoria;
    private String valor;
    private String passadaComo;
    private boolean usada;
    private int nivelLexico;
    private String escopo;

    public Simbolo() {
    }

    public Simbolo(String simbolo, String tipo, String categoria, String escopo) {
        this.simbolo = simbolo;
        this.tipo = tipo;
        this.categoria = categoria;
        this.escopo = escopo;
    }

    public Simbolo(String nomeVar, String tipo, String categoria, String valor,
                   String passadaComo, boolean usada, int nivelLexico, String escopo) {
        this.simbolo = nomeVar;
        this.tipo = tipo;
        this.categoria = categoria;
        this.valor = valor;
        this.passadaComo = passadaComo;
        this.usada = usada;
        this.nivelLexico = nivelLexico;
        this.escopo = escopo;
    }

    public Simbolo(String nomeVar, String tipoVariavel, String variavel,
                   int nivelLexicoAtual, String escopoAtual) {
        this.simbolo = nomeVar;
        this.tipo = tipoVariavel;
        this.categoria = variavel;
        this.nivelLexico = nivelLexicoAtual;
        this.escopo = escopoAtual;
        this.usada = false;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEscopo() {
        return escopo;
    }

    public void setEscopo(String escopo) {
        this.escopo = escopo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getPassadaComo() {
        return passadaComo;
    }

    public void setPassadaComo(String passadaComo) {
        this.passadaComo = passadaComo;
    }

    public boolean isUsada() {
        return usada;
    }

    public void setUsada(boolean usada) {
        this.usada = usada;
    }

    public int getNivelLexico() {
        return nivelLexico;
    }

    public void setNivelLexico(int nivelLexico) {
        this.nivelLexico = nivelLexico;
    }

    @Override
    public String toString() {
        return String.format(
                "Símbolo: %-10s | Tipo: %-10s | Categoria: %-10s | Passada: %-10s | Usada: %-5s | Nível: %d | Escopo: %s",
                simbolo, tipo, categoria, passadaComo, usada, nivelLexico, escopo
        );
    }
}
