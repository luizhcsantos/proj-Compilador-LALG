package br.unesp.compilerLALG.core.parser.ast;

import java.util.ArrayList;
import java.util.List;

public abstract class ASTnode {

    private String nome;
    private String valor;
    private List<ASTnode> filhos = new ArrayList<>();

    public ASTnode(String nome, String valor) {
        this.nome = nome;
        this.valor = valor;
        this.filhos = new ArrayList<>();
    }

    public void addFilhos(ASTnode filho) {
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

    public List<ASTnode> getFilhos() {
        return filhos;
    }

}
