package br.unesp.compilerLALG.core.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class TabelaSimbolos {

    private final Deque<Map<String, Simbolo>> pilhaEscopos = new ArrayDeque<>();
    private int nivelLexicoAtual = 0;
    private String escopoAtual = "global";

    public TabelaSimbolos() {
        entrarEScopo("global");
    }

    public void entrarEScopo(String nomeEscopo) {
        pilhaEscopos.push(new HashMap<>());
        this.escopoAtual = nomeEscopo;
        this.nivelLexicoAtual = pilhaEscopos.size() - 1;
    }

    public void sairEscopo() {

        if (pilhaEscopos.size() > 1) {
            for (Map<String, Simbolo> escopo : pilhaEscopos) {
                for (Simbolo simbolo : escopo.values()) {
                    if (!simbolo.isUsada()) {
                        System.out.println("Aviso: a variável '" + simbolo.getSimbolo()+ "' foi declarada no escopo mas nunca foi usada\"");
                    }
                }
            }
            pilhaEscopos.pop();
            this.nivelLexicoAtual = pilhaEscopos.size() - 1;
            this.escopoAtual = pilhaEscopos.isEmpty() ? "global" : "escopo_" + nivelLexicoAtual;
        }

    }

    public void adicionarSimbolo(Simbolo simbolo) throws Exception {

        Map<String, Simbolo> escopoTopo =  pilhaEscopos.peek();

        if (escopoTopo != null) {
            if (escopoTopo.containsKey(simbolo.getSimbolo())) {
                throw new Exception("Erro: a variável '" + simbolo.getSimbolo() + "' já foi declarada no escopo atual.");
            } else {
                escopoTopo.put(simbolo.getSimbolo(), simbolo);
            }
        }
    }

    public Simbolo buscarSimbolo(String simbolo) {
        for (Map<String, Simbolo> escopo : pilhaEscopos) {
            if (escopo.containsKey(simbolo)) {
                return escopo.get(simbolo);
            }
        }
        return null;
    }

    public int getNivelLexicoAtual() {
        return nivelLexicoAtual;
    }

    public String getEscopoAtual() {
        return escopoAtual;
    }

    public Deque<Map<String, Simbolo>> getPilhaEscopos() {
        return pilhaEscopos;
    }
}
