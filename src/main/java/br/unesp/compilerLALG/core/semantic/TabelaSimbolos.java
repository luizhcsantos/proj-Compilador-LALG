package br.unesp.compilerLALG.core.semantic;

import java.util.*;

public class TabelaSimbolos {

    // Uma pilha de mapas. Cada mapa representa um nível de escopo.
    private final Stack<Map<String, Simbolo>> escopos;
    private int nivelAtual;

    public TabelaSimbolos() {
        this.escopos = new Stack<>();
        this.nivelAtual = 0;
        entrarEscopo(); // Cria o escopo global (nível 0)
    }

    public void entrarEscopo() {
        escopos.push(new HashMap<>());
        nivelAtual++;
    }

    public void sairEscopo() {
        if (!escopos.isEmpty()) {
            escopos.pop();
            nivelAtual--;
        }
    }

    public boolean inserir(String nome, String tipo, String categoria) {
        Map<String, Simbolo> escopoAtual = escopos.peek();

        // Verifica se a variável já existe NO MESMO ESCOPO
        if (escopoAtual.containsKey(nome)) {
            return false; // Erro: Variável já declarada
        }

        escopoAtual.put(nome, new Simbolo(nome, tipo, categoria, nivelAtual));
        return true;
    }

    public Simbolo buscar(String nome) {
        // Procura do escopo mais interno (topo da pilha) para o mais externo (global)
        for (int i = escopos.size() - 1; i >= 0; i--) {
            Map<String, Simbolo> escopo = escopos.get(i);
            if (escopo.containsKey(nome)) {
                return escopo.get(nome);
            }
        }
        return null; // Erro: Variável não declarada
    }

    public List<Simbolo> getTodosSimbolos() {
        List<Simbolo> todos = new ArrayList<>();
        // Percorre todos os mapas da pilha
        for (Map<String, Simbolo> escopo : escopos) {
            todos.addAll(escopo.values());
        }
        return todos;
    }
}