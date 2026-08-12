package br.unesp.compilerLALG.core.parser;

import java.util.Arrays;
import java.util.List;

public class GramaticaLALG {

    public static TabelaSintatica criarTabela() {
        TabelaSintatica tabela = new TabelaSintatica();
        carregarRegras(tabela);
        return tabela;
    }

    private static void carregarRegras(TabelaSintatica t) {

        // ---------------------------------------------------------
        // 1. PROGRAMA E BLOCO
        // ---------------------------------------------------------
        t.adicionarRegra("<programa>", "program",
                Arrays.asList("program", "<identificador>", ";", "<bloco>", "."));

        t.adicionarRegra("<bloco>", "int",
                Arrays.asList("<parte_de_declarações_de_variáveis>", "<parte_de_declarações_de_subrotinas>", "<comando_composto>"));
        t.adicionarRegra("<bloco>", "boolean",
                Arrays.asList("<parte_de_declarações_de_variáveis>", "<parte_de_declarações_de_subrotinas>", "<comando_composto>"));
        t.adicionarRegra("<bloco>", "procedure",
                Arrays.asList("<parte_de_declarações_de_variáveis>", "<parte_de_declarações_de_subrotinas>", "<comando_composto>"));
        t.adicionarRegra("<bloco>", "begin",
                Arrays.asList("<parte_de_declarações_de_variáveis>", "<parte_de_declarações_de_subrotinas>", "<comando_composto>"));

        // ---------------------------------------------------------
        // 2. DECLARAÇÕES DE VARIÁVEIS
        // ---------------------------------------------------------
        t.adicionarRegra("<parte_de_declarações_de_variáveis>", "int",
                Arrays.asList("<declaração_de_variáveis>", ";", "<declaração_de_variáveis'>"));
        t.adicionarRegra("<parte_de_declarações_de_variáveis>", "boolean",
                Arrays.asList("<declaração_de_variáveis>", ";", "<declaração_de_variáveis'>"));
        t.adicionarRegra("<parte_de_declarações_de_variáveis>", "procedure",
                Arrays.asList("EPSILON"));
        t.adicionarRegra("<parte_de_declarações_de_variáveis>", "begin",
                Arrays.asList("EPSILON"));

        t.adicionarRegra("<declaração_de_variáveis'>", "int",
                Arrays.asList("<declaração_de_variáveis>", "<declaração_de_variáveis'>", ";"));
        t.adicionarRegra("<declaração_de_variáveis'>", "boolean",
                Arrays.asList("<declaração_de_variáveis>", "<declaração_de_variáveis'>", ";"));
        t.adicionarRegra("<declaração_de_variáveis'>", "procedure",
                Arrays.asList("EPSILON"));
        t.adicionarRegra("<declaração_de_variáveis'>", "begin",
                Arrays.asList("EPSILON"));

        t.adicionarRegra("<declaração_de_variáveis>", "int",
                Arrays.asList("<tipo>", "<lista_de_identificadores>"));
        t.adicionarRegra("<declaração_de_variáveis>", "boolean",
                Arrays.asList("<tipo>", "<lista_de_identificadores>"));

        t.adicionarRegra("<tipo>", "int", Arrays.asList("int"));
        t.adicionarRegra("<tipo>", "boolean", Arrays.asList("boolean"));

        t.adicionarRegra("<lista_de_identificadores>", "identificador",
                Arrays.asList("<identificador>", "<lista_de_identificadores'>"));

        t.adicionarRegra("<lista_de_identificadores'>", ",",
                Arrays.asList(",", "<identificador>", "<lista_de_identificadores'>"));
        t.adicionarRegra("<lista_de_identificadores'>", ";", Arrays.asList("EPSILON"));
        t.adicionarRegra("<lista_de_identificadores'>", ":", Arrays.asList("EPSILON"));

        // ---------------------------------------------------------
        // 3. SUBROTINAS (PROCEDURES)
        // ---------------------------------------------------------
        t.adicionarRegra("<parte_de_declarações_de_subrotinas>", "procedure",
                Arrays.asList("<declaração_de_procedimento>", ";", "<declaração_de_procedimento'>"));
        t.adicionarRegra("<parte_de_declarações_de_subrotinas>", "begin",
                Arrays.asList("EPSILON"));

        t.adicionarRegra("<declaração_de_procedimento'>", "procedure",
                Arrays.asList("<declaração_de_procedimento>", "<declaração_de_procedimento'>", ";"));
        t.adicionarRegra("<declaração_de_procedimento'>", "begin",
                Arrays.asList("EPSILON"));

        t.adicionarRegra("<declaração_de_procedimento>", "procedure",
                Arrays.asList("procedure", "<identificador>", "<parâmetros_formais>", ";", "<bloco>"));

        t.adicionarRegra("<parâmetros_formais>", "(",
                Arrays.asList("(", "<seção_de_parâmetros_formais>", "<parâmetros_formais'>", ")"));
        t.adicionarRegra("<parâmetros_formais>", ";", Arrays.asList("EPSILON"));

        t.adicionarRegra("<parâmetros_formais'>", ";",
                Arrays.asList(";", "<seção_de_parâmetros_formais>", "<parâmetros_formais'>"));
        t.adicionarRegra("<parâmetros_formais'>", ")", Arrays.asList("EPSILON"));

        t.adicionarRegra("<seção_de_parâmetros_formais>", "var",
                Arrays.asList("<var>", "<lista_de_identificadores>", ":", "<tipo>"));
        t.adicionarRegra("<seção_de_parâmetros_formais>", "identificador",
                Arrays.asList("<var>", "<lista_de_identificadores>", ":", "<tipo>"));

        t.adicionarRegra("<var>", "var", Arrays.asList("var"));
        t.adicionarRegra("<var>", "identificador", Arrays.asList("EPSILON"));

        // ---------------------------------------------------------
        // 4. COMANDOS
        // ---------------------------------------------------------
        t.adicionarRegra("<comando_composto>", "begin",
                Arrays.asList("begin", "<comando>", "<comando_composto'>", "end"));

        t.adicionarRegra("<comando_composto'>", ";",
                Arrays.asList(";", "<comando>", "<comando_composto'>"));
        t.adicionarRegra("<comando_composto'>", "end", Arrays.asList("EPSILON"));

        t.adicionarRegra("<comando>", "identificador", Arrays.asList("<identificador>", "<comando'>"));
        t.adicionarRegra("<comando>", "if", Arrays.asList("<comando_condicional_1>"));
        t.adicionarRegra("<comando>", "while", Arrays.asList("<comando_repetitivo_1>"));
        t.adicionarRegra("<comando>", "begin", Arrays.asList("<comando_composto>"));

        t.adicionarRegra("<comando'>", ":=", Arrays.asList(":=", "<expressão>"));
        t.adicionarRegra("<comando'>", "(", Arrays.asList("(", "<chamada_de_procedimento'>"));

        t.adicionarRegra("<atribuição>", "identificador", Arrays.asList("<variável>", ":=", "<expressão>"));
        t.adicionarRegra("<chamada_de_procedimento>", "identificador", Arrays.asList("<identificador>", "<chamada_de_procedimento'>"));

        t.adicionarRegra("<chamada_de_procedimento'>", "+", Arrays.asList("<lista_de_expressões>", ")"));
        t.adicionarRegra("<chamada_de_procedimento'>", "-", Arrays.asList("<lista_de_expressões>", ")"));
        t.adicionarRegra("<chamada_de_procedimento'>", "identificador", Arrays.asList("<lista_de_expressões>", ")"));
        t.adicionarRegra("<chamada_de_procedimento'>", "numero", Arrays.asList("<lista_de_expressões>", ")"));
        t.adicionarRegra("<chamada_de_procedimento'>", "(", Arrays.asList("<lista_de_expressões>", ")"));
        t.adicionarRegra("<chamada_de_procedimento'>", "not", Arrays.asList("<lista_de_expressões>", ")"));
        t.adicionarRegra("<chamada_de_procedimento'>", ";", Arrays.asList("EPSILON"));
        t.adicionarRegra("<chamada_de_procedimento'>", "end", Arrays.asList("EPSILON"));
        t.adicionarRegra("<chamada_de_procedimento'>", "else", Arrays.asList("EPSILON"));

        t.adicionarRegra("<comando_condicional_1>", "if",
                Arrays.asList("if", "<expressão>", "then", "<comando>", "<else>"));

        t.adicionarRegra("<else>", "else", Arrays.asList("else", "<comando>"));
        t.adicionarRegra("<else>", ";", Arrays.asList("EPSILON"));
        t.adicionarRegra("<else>", "end", Arrays.asList("EPSILON"));

        t.adicionarRegra("<comando_repetitivo_1>", "while",
                Arrays.asList("while", "<expressão>", "do", "<comando>"));

        // ---------------------------------------------------------
        // 5. EXPRESSÕES
        // ---------------------------------------------------------
        t.adicionarRegra("<expressão>", "+", Arrays.asList("<expressão_simples>", "<expressão'>"));
        t.adicionarRegra("<expressão>", "-", Arrays.asList("<expressão_simples>", "<expressão'>"));
        t.adicionarRegra("<expressão>", "identificador", Arrays.asList("<expressão_simples>", "<expressão'>"));
        t.adicionarRegra("<expressão>", "numero", Arrays.asList("<expressão_simples>", "<expressão'>"));
        t.adicionarRegra("<expressão>", "(", Arrays.asList("<expressão_simples>", "<expressão'>"));
        t.adicionarRegra("<expressão>", "not", Arrays.asList("<expressão_simples>", "<expressão'>"));

        t.adicionarRegra("<expressão'>", "=", Arrays.asList("<relação>", "<expressão_simples>"));
        t.adicionarRegra("<expressão'>", "<>", Arrays.asList("<relação>", "<expressão_simples>"));
        t.adicionarRegra("<expressão'>", "<", Arrays.asList("<relação>", "<expressão_simples>"));
        t.adicionarRegra("<expressão'>", "<=", Arrays.asList("<relação>", "<expressão_simples>"));
        t.adicionarRegra("<expressão'>", "=>", Arrays.asList("<relação>", "<expressão_simples>"));
        t.adicionarRegra("<expressão'>", ">", Arrays.asList("<relação>", "<expressão_simples>"));
        t.adicionarRegra("<expressão'>", "]", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão'>", ")", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão'>", ",", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão'>", ";", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão'>", "end", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão'>", "else", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão'>", "then", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão'>", "do", Arrays.asList("EPSILON"));

        t.adicionarRegra("<relação>", "=", Arrays.asList("="));
        t.adicionarRegra("<relação>", "<>", Arrays.asList("<>"));
        t.adicionarRegra("<relação>", "<", Arrays.asList("<"));
        t.adicionarRegra("<relação>", "<=", Arrays.asList("<="));
        t.adicionarRegra("<relação>", "=>", Arrays.asList("=>"));
        t.adicionarRegra("<relação>", ">", Arrays.asList(">"));

        t.adicionarRegra("<expressão_simples>", "+", Arrays.asList("<op>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples>", "-", Arrays.asList("<op>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples>", "identificador", Arrays.asList("<op>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples>", "numero", Arrays.asList("<op>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples>", "(", Arrays.asList("<op>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples>", "not", Arrays.asList("<op>", "<termo>", "<expressão_simples'>"));

        t.adicionarRegra("<op>", "+", Arrays.asList("+"));
        t.adicionarRegra("<op>", "-", Arrays.asList("-"));
        t.adicionarRegra("<op>", "identificador", Arrays.asList("EPSILON"));
        t.adicionarRegra("<op>", "numero", Arrays.asList("EPSILON"));
        t.adicionarRegra("<op>", "(", Arrays.asList("EPSILON"));
        t.adicionarRegra("<op>", "not", Arrays.asList("EPSILON"));

        t.adicionarRegra("<expressão_simples'>", "+", Arrays.asList("<op2>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples'>", "-", Arrays.asList("<op2>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples'>", "or", Arrays.asList("<op2>", "<termo>", "<expressão_simples'>"));
        t.adicionarRegra("<expressão_simples'>", "=", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "<>", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "<", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "<=", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "=>", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", ">", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "]", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", ")", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", ",", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", ";", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "end", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "else", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "then", Arrays.asList("EPSILON"));
        t.adicionarRegra("<expressão_simples'>", "do", Arrays.asList("EPSILON"));

        t.adicionarRegra("<op2>", "+", Arrays.asList("+"));
        t.adicionarRegra("<op2>", "-", Arrays.asList("-"));
        t.adicionarRegra("<op2>", "or", Arrays.asList("or"));

        t.adicionarRegra("<termo>", "identificador", Arrays.asList("<fator>", "<termo'>"));
        t.adicionarRegra("<termo>", "numero", Arrays.asList("<fator>", "<termo'>"));
        t.adicionarRegra("<termo>", "(", Arrays.asList("<fator>", "<termo'>"));
        t.adicionarRegra("<termo>", "not", Arrays.asList("<fator>", "<termo'>"));

        t.adicionarRegra("<termo'>", "*", Arrays.asList("<op3>", "<fator>", "<termo'>"));
        t.adicionarRegra("<termo'>", "div", Arrays.asList("<op3>", "<fator>", "<termo'>"));
        t.adicionarRegra("<termo'>", "and", Arrays.asList("<op3>", "<fator>", "<termo'>"));
        t.adicionarRegra("<termo'>", "+", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "-", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "or", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "=", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "<>", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "<", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "<=", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "=>", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", ">", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "]", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", ")", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", ",", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", ";", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "end", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "else", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "then", Arrays.asList("EPSILON"));
        t.adicionarRegra("<termo'>", "do", Arrays.asList("EPSILON"));

        t.adicionarRegra("<op3>", "*", Arrays.asList("*"));
        t.adicionarRegra("<op3>", "div", Arrays.asList("div"));
        t.adicionarRegra("<op3>", "and", Arrays.asList("and"));

        t.adicionarRegra("<fator>", "identificador", Arrays.asList("<variável>"));
        t.adicionarRegra("<fator>", "numero", Arrays.asList("<número>"));
        t.adicionarRegra("<fator>", "(", Arrays.asList("(", "<expressão>", ")"));
        t.adicionarRegra("<fator>", "not", Arrays.asList("not", "<fator>"));

        t.adicionarRegra("<variável>", "identificador", Arrays.asList("<identificador>", "<variável'>"));

        t.adicionarRegra("<variável'>", "[", Arrays.asList("[", "<expressão>", "]"));
        t.adicionarRegra("<variável'>", "(", Arrays.asList("(", "<lista_de_expressões>", ")"));
        t.adicionarRegra("<variável'>", "*", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "div", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "and", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "+", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "-", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "or", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "=", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "<>", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "<", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "<=", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "=>", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", ">", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "]", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", ")", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", ",", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", ";", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "end", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "else", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "then", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", "do", Arrays.asList("EPSILON"));
        t.adicionarRegra("<variável'>", ":=", List.of("EPSILON"));

        t.adicionarRegra("<lista_de_expressões>", "+", Arrays.asList("<expressão>", "<lista_de_expressões'>"));
        t.adicionarRegra("<lista_de_expressões>", "-", Arrays.asList("<expressão>", "<lista_de_expressões'>"));
        t.adicionarRegra("<lista_de_expressões>", "identificador", Arrays.asList("<expressão>", "<lista_de_expressões'>"));
        t.adicionarRegra("<lista_de_expressões>", "numero", Arrays.asList("<expressão>", "<lista_de_expressões'>"));
        t.adicionarRegra("<lista_de_expressões>", "(", Arrays.asList("<expressão>", "<lista_de_expressões'>"));
        t.adicionarRegra("<lista_de_expressões>", "not", Arrays.asList("<expressão>", "<lista_de_expressões'>"));

        t.adicionarRegra("<lista_de_expressões'>", ",", Arrays.asList(",", "<expressão>", "<lista_de_expressões'>"));
        t.adicionarRegra("<lista_de_expressões'>", ")", Arrays.asList("EPSILON"));

        t.adicionarRegra("<número>", "numero", Arrays.asList("numero"));
        t.adicionarRegra("<identificador>", "identificador", Arrays.asList("identificador"));
    }
}