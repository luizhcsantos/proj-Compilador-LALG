package br.unesp.compilerLALG.core.semantic;

public class Simbolo {

    private String nome;
    private String tipo;
    private String categoria;
    private int escopo;

    public Simbolo() {
    }

    public Simbolo(String nome, String tipo, String categoria, int escopo) {
        this.nome = nome;
        this.tipo = tipo;
        this.categoria = categoria;
        this.escopo = escopo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getEscopo() {
        return escopo;
    }

    public void setEscopo(int escopo) {
        this.escopo = escopo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
