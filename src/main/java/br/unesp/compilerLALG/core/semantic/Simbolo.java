package br.unesp.compilerLALG.core.semantic;

public class Simbolo {

    private String nome;
    private String tipo;      // Ex: "int", "boolean"
    private String categoria; // Ex: "variavel", "procedimento", "parametro"
    private int linhaDeclaracao;
    private int escopo;
    private int nivelLexico;
    private boolean usada;
    private int deslocamento; // Deslocamento na memória (em bytes) para variáveis

    // Construtor
    public Simbolo(String nome, String tipo, String categoria,
                   int linhaDeclaracao, int nivelLexico, boolean usada, int deslocamento) {
        this.nome = nome;
        this.tipo = tipo;
        this.categoria = categoria;
        this.linhaDeclaracao = linhaDeclaracao;
        this.nivelLexico = nivelLexico;
        this.usada = usada;
        this.deslocamento = deslocamento;
    }

    public String getNome() {
        return nome;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getLinhaDeclaracao() {
        return linhaDeclaracao;
    }

    public int getLinha() { return linhaDeclaracao;  } // simbolo.linha

    public int getNivelLexico() {
        return nivelLexico;
    }

    public boolean isUsada() {
        return usada;
    }

    public int getDeslocamento() {
        return deslocamento;
    }

    public String getEscopo() {
        return nivelLexico == 0 ? "Global (0)" : "Local (" + nivelLexico + ")";
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setLinhaDeclaracao(int linhaDeclaracao) {
        this.linhaDeclaracao = linhaDeclaracao;
    }

    public void setUsada(boolean usada) {
        this.usada = usada;
    }

    public void setNivelLexico(int nivelLexico) {
        this.nivelLexico = nivelLexico;
    }

    public void setDeslocamento(int deslocamento) {
        this.deslocamento = deslocamento;
    }
}