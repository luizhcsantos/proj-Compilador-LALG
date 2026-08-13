package br.unesp.compilerLALG.core.semantic;

public class Simbolo {
    private String nome;
    private String tipo;      // "int", "boolean", "procedure"
    private String categoria; // "var", "param", "proc"
    private int nivelEscopo;
    private int enderecoRelativo;

    public Simbolo(String nome, String tipo, String categoria, int nivelEscopo) {
        this.nome = nome;
        this.tipo = tipo;
        this.categoria = categoria;
        this.nivelEscopo = nivelEscopo;
        this.enderecoRelativo = -1; // Inicialmente, o endereço relativo é -1 - significa que não foi alocado
    }


    public void setEnderecoRelativo(int enderecoRelativo) { this.enderecoRelativo = enderecoRelativo; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public int getNivelEscopo() { return nivelEscopo; }
    public int getEnderecoRelativo() { return enderecoRelativo; }

}