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

    /* TODO
        1. Expressões Relacionais (<, >, <=, >=, =, <>) e Lógicas (and, or, not) -- feito
        2. Comandos de ControlE (IF e WHILE) -- if feito, while necessita de atenção
        3. Comandos de Entrada/Saída (READ e WRITE)
        4. Procedimentos
        */

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

        raizArvore = new noArvoreDTO("programa", nomePrograma);

        noArvoreDTO noBloco = parseBloco();
        if (noBloco != null) {
            raizArvore.addFilho(noBloco);
        }

        match("PONTO");
    }

    private noArvoreDTO parseBloco() {

        noArvoreDTO bloco = new noArvoreDTO("bloco", "");

        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            parseParteDeclaracaoVariaveis();
        }

        if (FIRST_DECL_PROC.contains(tokenAtual.getToken())) {
            parseDeclaracaoProcedimentos();
        }

        noArvoreDTO noComandos = parseComandoComposto();
        if (noComandos != null) {
            bloco.addFilho(noComandos);
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

        noArvoreDTO noComando = new noArvoreDTO("comando", "");

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
            return noComando; // sai do metodo para evitar cascata de erros
        }

        switch (tokenAtual.getToken()) {
            case "IDENTIFICADOR" -> {
                // salva o nome da variável antes de consumir o token
                String nomeVariavelOuProcedimento = tokenAtual.getLexema();
                match("IDENTIFICADOR");

                if (tokenAtual.getToken().equals("ATRIBUICAO")) { // :=
                    return parseComandoAtribuicao(nomeVariavelOuProcedimento);
                } else if (tokenAtual.getToken().equals("ABREPAR")) {
                    match("ABREPAR");
                    // noArvoreDTO parametros = parseListaExpressoes();
                    match("FECHAPAR");

                    // Retorna um nó de Chamada de Procedimento (no futuro os parâmetros serão "pendurados" nele)
                    return new noArvoreDTO("chamada procedimento", nomeVariavelOuProcedimento);
                } else {
                    // Se não for := nem (, é uma chamada de procedimento sem parâmetros
                    return new noArvoreDTO("chamada procedimento", nomeVariavelOuProcedimento);
                }
            }
            case "READ" -> {
                return parseComandoLeitura();
            }
            case "WRITE" -> {
                return parseComandoEscrita();
            }
            case "BEGIN" -> {
                return parseComandoComposto();
            }
            case "IF" -> {
                return parseComandoIf();
            }
            case "WHILE" -> {
                return parseComandoWhile();
            }

            default -> {
                return null;
            }
        }
    }

    private noArvoreDTO parseComandoComposto() {

        noArvoreDTO noComando = new noArvoreDTO("Composto", "");
        match("BEGIN");

        noArvoreDTO lista = parseListaComandos();
        if (lista != null) {
            noComando.addFilho(lista);
        }
        match("END");
        return noComando;
    }

    // <comando repetitivo 1> ::= while <expressão> do <comando>
    private noArvoreDTO parseComandoWhile() {

        noArvoreDTO noWhile = new noArvoreDTO("Comando repetitivo 1", "while");

        match("WHILE");

        noArvoreDTO noCondicao = new noArvoreDTO("Condição", "");
        noArvoreDTO expCondicao = expressao();
        if  (expCondicao != null) { noCondicao.addFilho(expCondicao); }
        noWhile.addFilho(noCondicao);

        match("DO");
        noArvoreDTO noCorpo = new noArvoreDTO("Corpo do while (do)", "");
        noArvoreDTO cmdCorpo = parseComando();
        if (cmdCorpo != null) { noCorpo.addFilho(cmdCorpo); }
        noWhile.addFilho(noCorpo);

        return noWhile;
    }

    // <comando condicional1> ::= if <expressão> then <comando> [ else <comando> ]
    private noArvoreDTO parseComandoIf() {

        noArvoreDTO noIf = new noArvoreDTO("Comando condicional 1", "if");

        match("IF");

        // condição
        noArvoreDTO noCondicao = new noArvoreDTO("Condição", "");
        noArvoreDTO expCondicao = expressao();
        if (expCondicao != null) { noCondicao.addFilho(expCondicao); }
        noIf.addFilho(noCondicao);

        match("THEN");

        // verdadeiro
        noArvoreDTO noVerdadeiro =  new noArvoreDTO("Verdadeiro (then)", "");
        noArvoreDTO cmdVerdadeiro = parseComando();
        if (cmdVerdadeiro != null) { noVerdadeiro.addFilho(cmdVerdadeiro); }
        noIf.addFilho(noVerdadeiro);

        // else (opcional)
        if (tokenAtual.getToken().equals("ELSE")) {
            match("ELSE");

            noArvoreDTO noFalso = new noArvoreDTO("Falso (else)", "");
            noArvoreDTO cmdFalso = parseComando();
            if (cmdFalso != null) { noFalso.addFilho(cmdFalso); }
            noIf.addFilho(noFalso);
        }

        return noIf;
    }

    private noArvoreDTO parseComandoAtribuicao(String nomeVariavel) {
        // nó pai
        noArvoreDTO noAtribuicao = new noArvoreDTO("Atribuição", "");

        // filho esquerdo
        noArvoreDTO terminalVar = new noArvoreDTO("Variável", nomeVariavel);
        noAtribuicao.addFilho(terminalVar);

        // filho central
        match("ATRIBUICAO");
        noArvoreDTO terminalSinal = new noArvoreDTO("Símbolo", ":=");
        noAtribuicao.addFilho(terminalSinal);

        // filho direito
        noArvoreDTO resultadoMatematica = expressao();
        if (resultadoMatematica != null) {
            noAtribuicao.addFilho(resultadoMatematica);
        }

        return noAtribuicao;
    }

    private noArvoreDTO parseComandoEscrita() {

        noArvoreDTO noWrite = new noArvoreDTO("Comando", "WRITE");
        match("WRITE");   // <-- SE ESTA LINHA NÃO RODAR, O COMPILADOR TRAVA!
        match("ABREPAR");

        noArvoreDTO expressaoImpressa = expressao();
        if (expressaoImpressa != null) {
            noWrite.addFilho(expressaoImpressa);
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            match("VIRGULA");
            noArvoreDTO proximaExpressao = expressao();
            if (proximaExpressao != null) {
                noWrite.addFilho(proximaExpressao);
            }
        }

        match("FECHAPAR");
        return noWrite;
    }

    private noArvoreDTO parseComandoLeitura() {

        noArvoreDTO noRead = new noArvoreDTO("Comando", "READ");
        match("READ");    // <-- A MESMA COISA AQUI!
        match("ABREPAR");

        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            noRead.addFilho(new noArvoreDTO("Variável Lida", tokenAtual.getLexema()));
            match("IDENTIFICADOR");
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            match("VIRGULA");
            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                noRead.addFilho(new noArvoreDTO("Variável Lida", tokenAtual.getLexema()));
                match("IDENTIFICADOR");
            }
        }

        match("FECHAPAR");
        return noRead;
    }

    // <lista_comandos> ::= <comando> { ; <comando> }
    public noArvoreDTO parseListaComandos() {

        noArvoreDTO lista = new noArvoreDTO("lista de comandos", "");

        noArvoreDTO cmd = parseComando();
        if (cmd != null) {
            lista.addFilho(cmd);
        }


        while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
            match("PONTOVIRGULA");

            if (tokenAtual.getToken().equals("END")) break;

            noArvoreDTO proximoCmd = parseComando();
            if (proximoCmd != null) {
                lista.addFilho(proximoCmd);
            }

//            sincronizar(FOLLOW_COMANDO);
        }
        return lista;
    }

    private noArvoreDTO expressaoSimples() {
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
        while (tokenAtual.getToken().equals("OPSOMA") ||
                tokenAtual.getToken().equals("OPSUB") ||
                tokenAtual.getToken().equals("OPOR")) {

            String operador = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();// o sinal de +
            match(tokenDoOperador); // consome +, - ou or

            noArvoreDTO noDireita = termo();

            noArvoreDTO noPai = new noArvoreDTO("Expressão simples", operador);

            // "pendura" a matemática na ordem exata: esquerda, meio(+), direita
            noPai.addFilho(noEsquerda);

            noArvoreDTO terminalOperador = new noArvoreDTO("Operador", operador);
            noPai.addFilho(terminalOperador);

            noPai.addFilho(noDireita);

            noEsquerda = noPai;
        }

        return noEsquerda;
    }

    public noArvoreDTO expressao() {
        noArvoreDTO noEsquerda = expressaoSimples();

        if (FIRST_RELACAO.contains(tokenAtual.getToken())) {

            String operadorRelacional = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();

            // consome o sinal
            match(tokenDoOperador);

            noArvoreDTO noDireita = expressaoSimples();


            noArvoreDTO noRelacao = new noArvoreDTO("Expressão relacional", operadorRelacional);

            if (noEsquerda != null) {
                noRelacao.addFilho(noEsquerda);
            }

            noArvoreDTO terminalOperador = new noArvoreDTO("Operador Relacional", operadorRelacional);
            noRelacao.addFilho(terminalOperador);

            if (noDireita != null) {
                noRelacao.addFilho(noDireita);
            }

            return noRelacao;
        }
        return noEsquerda;
    }

    public noArvoreDTO termo() {
        noArvoreDTO noEsquerda = fator();

        while (tokenAtual.getToken().equals("OPMUL") ||
                tokenAtual.getToken().equals("OPDIV") ||
                tokenAtual.getToken().equals("OPAND")) {

            String operador = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();
            match(tokenDoOperador); // consome *, / ou and

            noArvoreDTO noDireita = fator();

            noArvoreDTO noPai = new noArvoreDTO("Termo", operador);
            noPai.addFilho(noEsquerda);
            noPai.addFilho(noDireita);

            noEsquerda = noPai;
        }


        return noEsquerda;
    }

    public noArvoreDTO fator() {

        switch (tokenAtual.getToken()) {
            case "IDENTIFICADOR" -> {
                String nomeIdentificador = tokenAtual.getLexema();
                match("IDENTIFICADOR");

                if (tokenAtual.getToken().equals("ABRECOLCHETE")) {
                    match("ABRECOLCHETE");
                    noArvoreDTO indiceVetor = expressaoSimples();
                    match("FECHACOLCHETE");

                    noArvoreDTO noVEtor = new noArvoreDTO("Vetor", nomeIdentificador);
                    if (indiceVetor != null) {
                        noVEtor.addFilho(indiceVetor);
                    }
                    return noVEtor;
                } else { // se não tem colchete, é uma variável simples (folha)
                    return new noArvoreDTO("Variável", nomeIdentificador);
                }
            }
            case "NUM" -> {
                noArvoreDTO noNum = new noArvoreDTO("Número", tokenAtual.getLexema());
                match("NUM");

                return noNum;
            }
            case "TRUE", "FALSE" -> {
                noArvoreDTO noBool = new noArvoreDTO("Booleano", tokenAtual.getLexema());
                match(tokenAtual.getToken());

                return noBool;
            }
            case "ABREPAR" -> {
                match("ABREPAR");

                noArvoreDTO noExpressaoInterna = expressaoSimples();
                match("FECHAPAR");
                return noExpressaoInterna;
            }
            case "OPNOT" -> {
                match("OPNOT");

                noArvoreDTO noFatorNEgado = fator();
                noArvoreDTO noNot = new noArvoreDTO("Operador Unário", "not");
                if (noFatorNEgado != null) {
                    noNot.addFilho(noFatorNEgado);
                }
                return noNot;
            }
            default -> {
                listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                        "Início de fator válido (Identificador, Número, '(', 'not', 'true', 'false')",
                        tokenAtual.getToken(),
                        tokenAtual.getLexema(),
                        tokenAtual.getLinha(),
                        tokenAtual.getColunaInicial()
                ));

                // Joga foras os tokens até encontrar um ponto seguro da matemática
                sincronizar(FOLLOW_EXPRESSAO);
                return null;
            }
        }

    }


    public List<CompilerException.SyntaxException> getErros() {
        return listaErrosSintaticos;
    }

    public boolean temErros() {
        return !listaErrosSintaticos.isEmpty();
    }

    public noArvoreDTO getRaizArvore() {
        return raizArvore;
    }
}
