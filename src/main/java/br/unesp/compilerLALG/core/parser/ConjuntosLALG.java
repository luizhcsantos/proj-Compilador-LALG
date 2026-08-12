package br.unesp.compilerLALG.core.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConjuntosLALG {

    // Conjunto de tokens que podem vir LOGO APÓS um Comando
    public static final Set<String> FOLLOW_COMANDO = new HashSet<>(Arrays.asList(
            ";", "end", "else"
    ));

    // Conjunto de tokens que podem vir LOGO APÓS uma Expressão
    public static final Set<String> FOLLOW_EXPRESSAO = new HashSet<>(Arrays.asList(
            "]", ")", ",", ";", "end", "else", "then", "do"
    ));

    // Conjunto de tokens que podem vir LOGO APÓS uma Declaração
    public static final Set<String> FOLLOW_DECLARACAO = new HashSet<>(Arrays.asList(
            "procedure", "begin"
    ));

    // Método utilitário para descobrir qual conjunto usar com base no topo da pilha
    public static Set<String> obterFollow(String naoTerminal) {
        if (naoTerminal.contains("<comando>")) return FOLLOW_COMANDO;
        if (naoTerminal.contains("<expressão>")) return FOLLOW_EXPRESSAO;
        if (naoTerminal.contains("declaração")) return FOLLOW_DECLARACAO;

        // Conjunto SINC genérico (ponto e vírgula costuma salvar a maioria dos erros)
        return new HashSet<>(Arrays.asList(";"));
    }
}