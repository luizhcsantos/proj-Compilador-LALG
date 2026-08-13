package br.unesp.compilerLALG.core.parser.ast;

import java.util.ArrayList;
import java.util.List;

public class noArvore {

    private final String simbolo; // "<comando>", "identificador", "if", etc
    private String lexema;  // "soma", "10", ":=" (Só preenchido nos terminais)
    private final List<noArvore> filhos;
    private int linha;

    public noArvore(String simbolo) {
        this.simbolo = simbolo;
        this.lexema = "";
        this.filhos = new ArrayList<>();
    }

    public void addFilho(noArvore filho) {
        this.filhos.add(filho);
    }

    public String getSimbolo() { return simbolo; }
    public String getLexema() { return lexema; }
    public void setLexema(String lexema) { this.lexema = lexema; }
    public int getLinha() { return linha; }
    public void setLinha(int linha) { this.linha = linha; }
    public List<noArvore> getFilhos() { return filhos; }

}
