package br.unesp.compilerLALG.core.parser.ast;

import java.util.ArrayList;
import java.util.List;

public class noArvoreDTO {

    private String nome;
    private String valor;
    private int linha;
    private List<noArvoreDTO> filhos = new ArrayList<>();

    public noArvoreDTO(String nome, String valor, int linha) {
        this.nome = nome;
        this.valor = valor;
        this.filhos = new ArrayList<>();
        this.linha = linha;
    }

    public noArvoreDTO(String nome, String valor) {
       this(nome, valor, -1);
    }

    public void addFilho(noArvoreDTO filho) {
        if(filho != null) {
            this.filhos.add(filho);
        }
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public int getLinha() {
        return linha;
    }

    public List<noArvoreDTO> getFilhos() {
        return filhos;
    }

}
