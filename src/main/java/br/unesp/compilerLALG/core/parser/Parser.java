package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.noArvoreDTO;
import br.unesp.compilerLALG.exception.CompilerException;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Parser {

    private final List<Token> tokens;
    private int posicaoAtual;
    private Token tokenAtual;
    private int pos = 0;
    private noArvoreDTO raizArvore;

    // Lista para guardar os erros sintáticos (Panic Mode)
    private final List<CompilerException.SyntaxException> listaErrosSintaticos = new ArrayList<>();

    // Conjuntos First (Para escolher qual caminho seguir na EBNF)

    // <bloco>
    private final Set<String> FIRST_BLOCO = Set.of(
            "INT", "BOOLEAN", "PROCEDURE", "BEGIN"
            // Nota: EPSILON é tratado na lógica dos IFs
    );

    // <parte_de_declarações_de_variáveis> e afins
    private final Set<String> FIRST_DECL_VAR = Set.of(
            "INT", "BOOLEAN"
    );

    // <parte_de_declarações_de_subrotinas>
    private final Set<String> FIRST_DECL_PROC = Set.of(
            "PROCEDURE"
    );

    // <comando>
    private final Set<String> FIRST_COMANDO = Set.of(
            "IDENTIFICADOR", "READ", "WRITE", "IF", "WHILE", "BEGIN"
    );

    // <expressão> e <lista_de_expressões>
    // Engloba sinais (+, -), identificadores, números, '(', 'not', 'true', 'false'
    private final Set<String> FIRST_EXPRESSAO = Set.of(
            "OPSOMA", "OPSUB", "IDENTIFICADOR", "NUM", "ABREPAR", "OPNOT", "TRUE", "FALSE"
    );

    // <termo> e <fator>
    private final Set<String> FIRST_FATOR = Set.of(
            "IDENTIFICADOR", "NUM", "ABREPAR", "OPNOT", "TRUE", "FALSE"
    );

    // <relação>
    private final Set<String> FIRST_RELACAO = Set.of(
            "OPIGUAL", "OPDIF", "OPMENOR", "OPMENORIGUAL", "OPMAIOR", "OPMAIORIGUAL"
    );

    // <op> e <op2> e <op3> (Operadores Matemáticos e Lógicos)
    private final Set<String> FIRST_OP_SOMA_SUB = Set.of("OPSOMA", "OPSUB");
    private final Set<String> FIRST_OP_MUL_DIV = Set.of("OPMUL", "OPDIV", "OPAND");

    // Conjuntos Follow (Para Sincronização / Panic Mode)

    // FOLLOW(<programa>)
    private final Set<String> FOLLOW_PROGRAMA = Set.of(
            "EOF"
    );

    // FOLLOW(<bloco>)
    private final Set<String> FOLLOW_BLOCO = Set.of(
            "PONTO", "PONTOVIRGULA", "PROCEDURE", "BEGIN"
    );

    // FOLLOW(<declaração_de_variáveis>) e <parte_de_declarações...>
    private final Set<String> FOLLOW_DECL_VAR = Set.of(
            "PONTOVIRGULA", "INT", "BOOLEAN", "PROCEDURE", "PONTO", "BEGIN"
    );

    // FOLLOW(<declaração_de_procedimento>)
    private final Set<String> FOLLOW_DECL_PROC = Set.of(
            "PONTOVIRGULA", "PROCEDURE", "BEGIN"
    );

    // FOLLOW(<comando>)
    // Usado para recuperar de erros ao escrever if, while, atribuições, etc.
    private final Set<String> FOLLOW_COMANDO = Set.of(
            "PONTOVIRGULA", "PONTO", "PROCEDURE", "BEGIN", "END", "ELSE"
    );

    // FOLLOW(<expressão>), <termo> e <fator>
    // Usado para recuperar erros no meio de uma conta matemática ou relação lógica
    private final Set<String> FOLLOW_EXPRESSAO = Set.of(
            "PONTOVIRGULA", "PONTO", "PROCEDURE", "BEGIN", "END", "ELSE",
            "THEN", "DO", "FECHAPAR", "VIRGULA"
    );

    // FOLLOW(<lista_de_identificadores>)
    // pode ser seguida por ; (declaração normal), : (parametros) ou ) (leitura)
    private final Set<String> FOLLOW_LISTA_ID = Set.of(
            "PONTOVIRGULA", "INT", "BOOLEAN", "PROCEDURE", ".", "BEGIN", "DOISPONTOS"
    );



    /* TODO:
        - Crie Listas de Strings (ou Set<String>) no topo da classe Parser
            contendo os conjuntos First e Follow mais importantes.
        - Sempre que o símbolo na EBNF for |, use um switch com os Firsts.
        - Sempre que o símbolo for [ ] (Opcional), use um if com os Firsts.
        - Sempre que houver um catch de erro, use um while de sincronização usando os Follows.
        - Corrija o ABREPAR e FECHAPAR no fator() para evitar bugs na matemática.
        - Adicione o metodo sincronizar() e comece a colocar blocos try-catch chamando ele dentro de blocos maiores.
        - Preencha os métodos vazios (parseDeclaracoesVariaveis, parseComandoIf, parseComandoWhile)
            traduzindo linha a linha da sua EBNF usando o match() e o avancar().
        -
     */

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.posicaoAtual = 0;
        if (!tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        }

    }

    private void avancar() {
        posicaoAtual++;
        if (posicaoAtual < tokens.size()) {
            tokenAtual = tokens.get(posicaoAtual);
        } else {
            tokenAtual = new Token("EOF", "EOF", 0, -1, -1); // Marca o fim dos tokens
        }
    }

    public void analisar() {
        try {
            parsePrograma();
            System.out.println("Análise sintática concluída com sucesso!");

            // Se terminou de analisar o programa, o próximo token DEVE ser o fim do arquivo.
            if (!tokenAtual.getToken().equals("EOF")) {
                throw new RuntimeException("Erro Sintático: Código extra após o fim do programa ('." + "') na linha " + tokenAtual.getLinha());
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }

    private void match(String tipoEsperado) {
        if (tokenAtual.getToken().equals(tipoEsperado)) {
            avancar(); // se for o tipo correto, consome o token e avança
        } else {
            // Regista o erro
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    tipoEsperado,
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()
            ));
        }

    }

    private void sincronizar(@NonNull Set<String> tokensSeguros) {
        // Continua consumindo tokens até achar um que pertença ao conjunto seguro,
        // ou até o arquivo acabar (EOF).
        while (!tokensSeguros.contains(tokenAtual.getToken()) && !tokenAtual.getToken().equals("EOF")) {
            avancar(); // Pega o próximo token do Lexer e joga o atual fora
        }
    }

    // Programa ::= PROGRAM <identificador> ; <bloco> .
    public void parsePrograma() {
        match("PROGRAM");

        String nomePrograma = tokenAtual.getLexema();
        match("IDENTIFICADOR");
        match("PONTOVIRGULA");

        raizArvore = new noArvoreDTO("Programa", nomePrograma);

        noArvoreDTO noBloco = parseBloco();
        if (noBloco != null) { raizArvore.addFilhos(noBloco); }

        match("PONTO");
    }

    private noArvoreDTO parseBloco() {

        noArvoreDTO bloco = new noArvoreDTO("Bloco", "");

        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            parseParteDeclaracaoVariaveis();
        }

        if (FIRST_DECL_PROC.contains(tokenAtual.getToken())) {
            parseDeclaracaoProcedimentos();
        }

        noArvoreDTO noComandos = parseComandoComposto();
        if (noComandos != null) {
            bloco.addFilhos(noComandos);
        }

        return bloco;

    }

    private void parseDeclaracaoProcedimentos() {

    }

    private void parseParteDeclaracaoVariaveis() {

        parseDeclaracaoVariaveis();
        match("PONTOVIRGULA");
        while (tokenAtual.getToken().equals("INT") || tokenAtual.getToken().equals("BOOLEAN")) {
            parseDeclaracaoVariaveis();
            match("PONTOVIRGULA");
        }
        ;
    }

    private void parseDeclaracaoVariaveis() {

        if (tokenAtual.getToken().equals("INT")) {
            match("INT");
        } else if (tokenAtual.getToken().equals("BOOLEAN")) {
            match("BOOLEAN");
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "INT ou BOOLEAN",
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()
            ));

            // PANIC MODE
            // Sincroniza usando o FIRST do próximo elemento (que é IDENTIFICADOR)
            // somado ao FOLLOW da regra atual (PONTOVIRGULA, BEGIN, PROCEDURE)
            Set<String> syncSet = new HashSet<>();
            syncSet.add("IDENTIFICADOR");
            syncSet.addAll(FOLLOW_DECL_VAR);

            sincronizar(syncSet);
        }

        // Se o Panic Mode funcionou, ele parou em cima de um Identificador ou de um ;
        // Chama a lista de identificadores apenas se estiver num token válido
        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            parseListaIdentificadores();
        }
    }

    private void parseListaIdentificadores() {
        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            match("IDENTIFICADOR");
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR",
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()
            ));
            // Se nem o primeiro ID veio certo, sincroniza e aborta a lista
            sincronizar(FOLLOW_LISTA_ID);
            return;
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            match("VIRGULA");

            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                match("IDENTIFICADOR");
            } else {
                listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                        "IDENTIFICADOR",
                        tokenAtual.getToken(),
                        tokenAtual.getLexema(),
                        tokenAtual.getLinha(),
                        tokenAtual.getColunaInicial()
                ));
                Set<String> syncSet = new HashSet<>(FOLLOW_LISTA_ID);
                syncSet.add("VIRGULA");
                sincronizar(syncSet);
            }
        }
    }

    // <comando> ::= <comando_atribuicao> | <comando_leitura> | <comando_escrita>
    public noArvoreDTO parseComando() {

        noArvoreDTO noComando = new noArvoreDTO("Comando", "");

        //
        if (!FIRST_COMANDO.contains(tokenAtual.getToken())) {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "Início de comando válido (IDENTIFICADOR, READ, WRITE, IF, WHILE, BEGIN)",
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_COMANDO);
            return noComando; // Sai do metodo para evitar cascata de erros
        }

        switch (tokenAtual.getToken()) {
            case "IDENTIFICADOR" -> {
                String nomeVariavelOuProcedimento = tokenAtual.getLexema();
                match("IDENTIFICADOR");

                if (tokenAtual.getToken().equals("ATRIBUICAO")) {
                    return parseComandoAtribuicao(nomeVariavelOuProcedimento);
                } else if (tokenAtual.getToken().equals("ABREPAR")) {
                    match("ABREPAR");
                    // parseListaExpressoes();
                    match("FECHAPAR");
                } else {
                    // Se não for := nem (, é uma chamada de procedimento sem parâmetros (ex: limpar_tela)
                    // O identificador já foi consumido, então não precisamos fazer nada!
                }
            }
            case "READ" -> parseComandoLeitura();
            case "WRITE" -> parseComandoEscrita();
            case "BEGIN" -> {
                noComando = parseComandoComposto();

            }
            case "IF" -> parseComandoIf();
            case "WHILE" -> parseComandoWhile();
        }

        return noComando;

    }

    private noArvoreDTO parseComandoComposto() {

        noArvoreDTO noComando = new noArvoreDTO("Composto", "");

        match("BEGIN");
        noComando = parseListaComandos();
        match("END");

        return noComando;
    }

    private void parseComandoWhile() {

    }

    private void parseComandoIf() {


    }

    private void parseComandoAtribuicao() {
        try {
            noArvoreDTO variavelEsquerda = new noArvoreDTO("Variável destino", "a");
            match("ATRIBUICAO"); // :=

            noArvoreDTO resultadoMatematica = expressao();
            noArvoreDTO noAtribuicao = new noArvoreDTO("Atribuicao", ":=");
            noAtribuicao.addFilhos(variavelEsquerda);
            noAtribuicao.addFilhos(resultadoMatematica);
        } catch (CompilerException.SyntaxException e) {
            sincronizar(FOLLOW_COMANDO);
        }
    }

    private void parseComandoEscrita() {

    }

    private void parseComandoLeitura() {

    }

    // <lista_comandos> ::= <comando> { ; <comando> }
    public noArvoreDTO parseListaComandos() {

        noArvoreDTO lista = new noArvoreDTO("Lista de Comandos", "");

        noArvoreDTO cmd = parseComando();
        if (cmd != null) { lista.addFilhos(cmd); }

//        try {
        parseComando();

        while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
            match("PONTOVIRGULA");

            if (tokenAtual.getToken().equals("END")) break;

            noArvoreDTO proximoCmd = parseComando();
            if (proximoCmd != null) { lista.addFilhos(proximoCmd); }
//            }
//        } catch (RuntimeException e) {
            // anota o erro (no futuro, pode adicionar numa lista de erros sintáticos)
//            System.err.println(e.getMessage());

            // aciona o Panic Mode para pular até o fim do comando problemático
//            sincronizar(FOLLOW_COMANDO);
        }
        return lista;
        }

        private noArvoreDTO expressao () {
            if (!FIRST_EXPRESSAO.contains(tokenAtual.getToken())) {
                listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                        "Inicio de expressão válido (Numero,variável ou parenteses)",
                        tokenAtual.getToken(),
                        tokenAtual.getLexema(),
                        tokenAtual.getLinha(),
                        tokenAtual.getColunaInicial()
                ));
                sincronizar(FOLLOW_EXPRESSAO);
                return null;
            }
            noArvoreDTO noEsquerda = termo();
            while (tokenAtual.getToken().equals("OPSOMA")) {
                String operador = tokenAtual.getLexema();
                match("SOMA");

                noArvoreDTO noDireita = termo();

                noArvoreDTO noPai = new noArvoreDTO("Soma", operador);
                noPai.addFilhos(noEsquerda);
                noEsquerda.addFilhos(noDireita);

                noEsquerda = noPai;
            }

            return noEsquerda;
        }

        public noArvoreDTO termo () {
            // Mock rápido: Lê apenas identificadores ou números
            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                noArvoreDTO no = new noArvoreDTO("Variável", tokenAtual.getLexema());
                match("IDENTIFICADOR");
                return no;
            } else if (tokenAtual.getToken().equals("NUM")) {
                noArvoreDTO no = new noArvoreDTO("Número", tokenAtual.getLexema());
                match("NUM");
                return no;
            }

            // Se cair aqui, era algo inválido no meio da conta
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "Tipo esperado: IDENTIFICADOR ou NUM",
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()));
            sincronizar(FOLLOW_EXPRESSAO);
            return null;
        }


        public List<CompilerException.SyntaxException> getErros () {
            return listaErrosSintaticos;
        }

        public boolean temErros () {
            return !listaErrosSintaticos.isEmpty();
        }

        public noArvoreDTO getRaizArvore () {
            return raizArvore;
        }
    }
