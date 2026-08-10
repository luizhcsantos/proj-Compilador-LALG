package br.unesp.compilerLALG.core.semantic;

import java.util.*;

public class TabelaSimbolos {

    // O topo da pilha (último elemento inserido) é o escopo atual -mais interno.
    private final Deque<Map<String, Simbolo>> pilhaEscopos;

    // Ajuda a saber a profundidade do escopo atual
    // 0 = Global, 1 = Local de um procedimento, etc
    private int nivelLexicoAtual;

    public TabelaSimbolos() {
        this.pilhaEscopos = new ArrayDeque<>();
        this.nivelLexicoAtual = -1;
    }

    /**
     * 1. Entrar num Novo Escopo
     * Chamado ao entrar no programa principal ou em procedimentos.
     */
    public void entrarEscopo() {
        // LinkedHashMap preserva a ordem exata em que as variáveis foram declaradas
        pilhaEscopos.push(new LinkedHashMap<>());
        nivelLexicoAtual++;
    }

    /**
     * 2. Sair do Escopo Atual
     * Chamado ao encontrar o 'end' de um procedimento. Destrói as variáveis locais.
     */
    public void sairEscopo() {
        if (!pilhaEscopos.isEmpty()) {
            pilhaEscopos.pop();
            nivelLexicoAtual--;
        }
    }

    /**
     * 3. Inserir um Símbolo no Escopo Atual
     * Retorna false se o identificador já existir NESTE mesmo escopo (Erro de redeclaração).
     */
    public boolean inserir(String nome, String tipo, String categoria, int linha) {
        if (pilhaEscopos.isEmpty()) {
            entrarEscopo(); // garante que existe pelo menos o escopo global
        }

        Map<String, Simbolo> escopoAtual = pilhaEscopos.peek(); // Pega a Tabela do topo da pilha

        // Não pode declarar duas variáveis com o mesmo nome no MESMO escopo
        if (escopoAtual.containsKey(nome)) {
            return false;
        }

        // O deslocamento é quantos símbolos já existem neste escopo
        int deslocamento = escopoAtual.size();
        // Instancia o símbolo já com o nível léxico atual
        Simbolo novoSimbolo = new Simbolo(nome, tipo, categoria, linha, nivelLexicoAtual, false, deslocamento);
        escopoAtual.put(nome, novoSimbolo);
        return true;
    }

    /**
     * 4. Buscar Símbolo (Regra de Sobrescrita de Escopo)
     * Procura do escopo mais interno (Local) para o mais externo (Global).
     */
    public Simbolo buscar(String nome) {
        // A pilha itera naturalmente do Topo (Local) para a Base (Global)
        for (Map<String, Simbolo> escopo : pilhaEscopos) {
            if (escopo.containsKey(nome)) {
                return escopo.get(nome);
            }
        }
        return null; // Não existe em nenhum escopo ativo
    }

    /**
     * Achatar todos os símbolos numa lista única
     */
    public List<Simbolo> getTodosSimbolos() {
        List<Simbolo> listaFlat = new ArrayList<>();

        // Itera do Global) para o Local, para a tabela aparecer na ordem certa na tela
        Iterator<Map<String, Simbolo>> iterador = pilhaEscopos.descendingIterator();
        while (iterador.hasNext()) {
            listaFlat.addAll(iterador.next().values());
        }

        return listaFlat;
    }

    public Deque<Map<String, Simbolo>> getPilhaEscopos() {
        return pilhaEscopos;
    }

    public int getNivelLexicoAtual() {
        return nivelLexicoAtual;
    }

    public int getEscopoAtual() {
        return nivelLexicoAtual;
    }
}