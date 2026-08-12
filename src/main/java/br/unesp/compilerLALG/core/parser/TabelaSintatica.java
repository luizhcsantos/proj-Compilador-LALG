package br.unesp.compilerLALG.core.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabelaSintatica {

    // M[Não-Terminal][Terminal] = Lista de símbolos da produção
    private final Map<String, Map<String, List<String>>> tabela;

    public TabelaSintatica() {
        // Inicializar a tabela sintática aqui
        this.tabela = new HashMap<>();
    }

    public void adicionarRegra(String naoTerminal, String terminal, List<String> producao) {
        // Se o Não-Terminal ainda não existe na tabela, cria um novo mapa interno para ele
        tabela.computeIfAbsent(naoTerminal, k -> new HashMap<>())
                .put(terminal, producao);
    }

    public List<String> obterRegra(String naoTerminal, String terminal) {
        if (tabela.containsKey(naoTerminal.toLowerCase())) {
            return tabela.get(naoTerminal).get(terminal); // Retorna a regra ou null se estiver vazia
        }
        return null; // Célula vazia (indica um ERRO SINTÁTICO)
    }


    // Método utilitário para debug
    public boolean contemRegra(String naoTerminal, String terminal) {
        return obterRegra(naoTerminal, terminal) != null;
    }
}
