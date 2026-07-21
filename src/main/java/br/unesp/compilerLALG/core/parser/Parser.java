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
    private final Set<String> FIRST_OP_SOMA_SUB = Set.of("OPSOMA", "OPSUB", "OPOR");
    private final Set<String> FIRST_OP_MUL_DIV = Set.of("OPMUL", "OPDIV", "OPAND");

    // Conjuntos Follow (Para Sincronização / Panic Mode)

    // FOLLOW(<programa>)
    private final Set<String> FOLLOW_PROGRAMA = Set.of(
            "EOF"
    );

    // FOLLOW(<bloco>)
    private final Set<String> FOLLOW_BLOCO = Set.of(
            "PONTO", "PONTOVIRGULA"
    );

    // FOLLOW(<declaração_de_variáveis>) e <parte_de_declarações...>
    private final Set<String> FOLLOW_DECL_VAR = Set.of(
            "PONTOVIRGULA", "PROCEDURE", "BEGIN"
    );

    // FOLLOW(<declaração_de_procedimento>)
    private final Set<String> FOLLOW_DECL_PROC = Set.of(
            "PONTOVIRGULA", "BEGIN"
    );

    // FOLLOW(<comando>)
    // Usado para recuperar de erros ao escrever if, while, atribuições, etc.
    private final Set<String> FOLLOW_COMANDO = Set.of(
            "PONTOVIRGULA", "END", "ELSE"
    );

    // FOLLOW(<expressão>), <termo> e <fator>
    // Usado para recuperar erros no meio de uma conta matemática ou relação lógica
    private final Set<String> FOLLOW_EXPRESSAO = Set.of(
            "PONTOVIRGULA", "THEN", "DO",
            "FECHAPAR", "FECHACOL", "VIRGULA", "END", "ELSE"
    );

    // FOLLOW(<lista_de_identificadores>)
    // pode ser seguida por ; (declaração normal), : (parametros) ou ) (leitura)
    private final Set<String> FOLLOW_LISTA_ID = Set.of(
            "PONTOVIRGULA", "DOISPONTOS", "FECHAPAR"
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
            if (!FOLLOW_PROGRAMA.contains(tokenAtual.getToken())) {
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
        // Continua consumindo tokens até achar
        // um que pertença ao conjunto seguro,
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

        if(FIRST_BLOCO.contains(tokenAtual.getToken())) {
            noArvoreDTO noBloco = parseBloco();
            if (noBloco != null) {
                raizArvore.addFilho(noBloco);
            }
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                "Início de bloco válido",
                tokenAtual.getToken(),
                tokenAtual.getLexema(),
                tokenAtual.getLinha(),
                tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_BLOCO);
        }

        match("PONTO");
    }

    private noArvoreDTO parseBloco() {

        noArvoreDTO noBloco = new noArvoreDTO("Bloco", "");

        // tenta ler a seção de variáveis locais
        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            noArvoreDTO declaracoesVars = parseDeclaracaoVariaveis();
            if (declaracoesVars != null) noBloco.addFilho(declaracoesVars);
        }


        if (tokenAtual.getToken().equals("PROCEDURE")) {
            noArvoreDTO subrotinas = parseDeclaracoesSubrotinas();
            if (subrotinas != null) noBloco.addFilho(subrotinas);
        }

        // corpo principal (Begin ... End.)
        noArvoreDTO comandos = parseComandoComposto();
        if (comandos != null) { noBloco.addFilho(comandos); }

        return noBloco;

    }

    private noArvoreDTO parseDeclaracoesSubrotinas() {
        noArvoreDTO noSubrotinas = new noArvoreDTO("subrotinas", "");

        while (FIRST_DECL_PROC.contains(tokenAtual.getToken())) {
            noArvoreDTO proc = parseDeclaracaoProcedimentos();
            if (proc != null) { noSubrotinas.addFilho(proc); }
            match("PONTOVIRGULA");
        }
        return noSubrotinas.getFilhos().isEmpty() ? null : noSubrotinas; // se não tem subrotinas, retorna null para não poluir a árvore
    }

    private noArvoreDTO parseDeclaracaoProcedimentos() {

        noArvoreDTO noProc = new noArvoreDTO("Procedimento", "");

        // consome a palavra reservada 'procedure'
        // consome 'identificador' - nome do procedimento
        // lista de palametros formais - pode ser opcional
        //      ( seção de parametros formais - parametros formais' )
        //          [ var ] <lista de identificadores> : <tipo> { ; <lista de identificadores> : <tipo> }
        //              lista de identificadores ::= identificador { , identificador }
        // consome ';'
        // comando / comando composto
        match("PROCEDURE");
        String nomeProcedure = tokenAtual.getLexema();
        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            noProc.addFilho(new noArvoreDTO("Nome", nomeProcedure));
            match("IDENTIFICADOR");
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_DECL_PROC);
        }

        if (tokenAtual.getToken().equals("ABREPAR")) {
            noArvoreDTO params = parseParametrosFormais();
            if (params != null) {
                noProc.addFilho(params);
            }
        }
        match("PONTOVIRGULA");

        noArvoreDTO bloco = parseBloco();
        if (bloco != null) { noProc.addFilho(bloco); }

        return noProc;
    }

    private noArvoreDTO parseParametrosFormais() {
        noArvoreDTO noParams = new noArvoreDTO("Parametros Formais", "");
        match("ABREPAR");

        noArvoreDTO secao = parseSEcaoParametrosFormais();
        if (secao != null) {
            noParams.addFilho(secao);
        }

        while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
            match("PONTOVIRGULA");
            noArvoreDTO secao2 = parseSEcaoParametrosFormais();
            if (secao2 != null) {
                noParams.addFilho(secao2);
            }
        }
        match("FECHAPAR");
        return noParams;
    }

    private noArvoreDTO parseSEcaoParametrosFormais() {

        noArvoreDTO noSecao = new noArvoreDTO("Seção", "");

        if (tokenAtual.getToken().equals("VAR")) {
            noSecao.addFilho(new noArvoreDTO("Modificador", "var"));
            match("VAR");
        }

        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            noSecao.addFilho(new noArvoreDTO("Identificador", tokenAtual.getLexema()));
            match("IDENTIFICADOR");
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            match("VIRGULA");
            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                noSecao.addFilho(new noArvoreDTO("Identificador", tokenAtual.getLexema()));
                match("IDENTIFICADOR");
            }
        }
        match("DOISPONTOS");

        if (tokenAtual.getToken().equals("INT") || tokenAtual.getToken().equals("BOOLEAN")) {
            noSecao.addFilho(new noArvoreDTO("Tipo", tokenAtual.getLexema()));
            match(tokenAtual.getToken());
        } else if (tokenAtual.getToken().equals("IDENTIFICADOR")) { // Algumas variantes de LALG aceitam identificador de tipo
            noSecao.addFilho(new noArvoreDTO("Tipo", tokenAtual.getLexema()));
            match("IDENTIFICADOR");
        }
        return noSecao;
    }

    private void parseParteDeclaracaoVariaveis() {

        parseDeclaracaoVariaveis();
        match("PONTOVIRGULA");
        while (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            parseDeclaracaoVariaveis();
            match("PONTOVIRGULA");
        }
        ;
    }

    private noArvoreDTO parseDeclaracaoVariaveis() {

        noArvoreDTO noDeclaracoes = new noArvoreDTO("Declaração de Variáveis", "");

        // O laço continua enquanto o token atual for um tipo válido ('int' ou 'boolean')
        while (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {

            String tipoVar = tokenAtual.getLexema(); // Lê a palavra 'int' ou 'boolean'
            String tokenTipo = tokenAtual.getToken();

            noArvoreDTO noTipo = new noArvoreDTO("Tipo", tipoVar);
            match(tokenTipo); // Consome o 'int' ou 'boolean'

            // lê a primeira variável (obrigatória)
            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                noTipo.addFilho(new noArvoreDTO("Variável", tokenAtual.getLexema()));
                match("IDENTIFICADOR");
            } else {
                listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                        "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                        tokenAtual.getLinha(), tokenAtual.getColunaInicial()
                ));
            }

            // lê as restantes variáveis se houver vírgulas
            while (tokenAtual.getToken().equals("VIRGULA")) {
                match("VIRGULA");
                if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                    noTipo.addFilho(new noArvoreDTO("Variável", tokenAtual.getLexema()));
                    match("IDENTIFICADOR");
                } else {
                    listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                            "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                            tokenAtual.getLinha(), tokenAtual.getColunaInicial()
                    ));
                    sincronizar(FOLLOW_DECL_VAR);
                    return noDeclaracoes;
                }
            }

            match("PONTOVIRGULA");

            // "pendura" este bloco de tipo no nó principal de declarações
            noDeclaracoes.addFilho(noTipo);
        }

        // Se por algum motivo o nó estiver vazio, devolvemos null para manter a árvore limpa
        return noDeclaracoes.getFilhos().isEmpty() ? null : noDeclaracoes;
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

                    noArvoreDTO chamadaNode = new noArvoreDTO("chamada procedimento", nomeVariavelOuProcedimento);
                    noArvoreDTO parametros = parseListaExpressoes();

                    if (parametros != null) { chamadaNode.addFilho(parametros); }
                    match("FECHAPAR");

                    return chamadaNode;
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
        noArvoreDTO expCondicao = parseExpressao();
        if (expCondicao != null) {
            noCondicao.addFilho(expCondicao);
        }
        noWhile.addFilho(noCondicao);

        match("DO");
        noArvoreDTO noCorpo = new noArvoreDTO("Corpo do while (do)", "");
        noArvoreDTO cmdCorpo = parseComando();
        if (cmdCorpo != null) {
            noCorpo.addFilho(cmdCorpo);
        }
        noWhile.addFilho(noCorpo);

        return noWhile;
    }

    // <comando condicional1> ::= if <expressão> then <comando> [ else <comando> ]
    private noArvoreDTO parseComandoIf() {

        noArvoreDTO noIf = new noArvoreDTO("Comando condicional 1", "if");

        match("IF");

        // condição
        noArvoreDTO noCondicao = new noArvoreDTO("Condição", "");
        noArvoreDTO expCondicao = parseExpressao();
        if (expCondicao != null) {
            noCondicao.addFilho(expCondicao);
        }
        noIf.addFilho(noCondicao);

        match("THEN");

        // verdadeiro
        noArvoreDTO noVerdadeiro = new noArvoreDTO("Verdadeiro (then)", "");
        noArvoreDTO cmdVerdadeiro = parseComando();
        if (cmdVerdadeiro != null) {
            noVerdadeiro.addFilho(cmdVerdadeiro);
        }
        noIf.addFilho(noVerdadeiro);

        // else (opcional)
        if (tokenAtual.getToken().equals("ELSE")) {
            match("ELSE");

            noArvoreDTO noFalso = new noArvoreDTO("Falso (else)", "");
            noArvoreDTO cmdFalso = parseComando();
            if (cmdFalso != null) {
                noFalso.addFilho(cmdFalso);
            }
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
        if (FIRST_EXPRESSAO.contains(tokenAtual.getToken())) {
            noArvoreDTO resultadoMatematica = parseExpressao();
            if (resultadoMatematica != null) {
                noAtribuicao.addFilho(resultadoMatematica);
            }
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "Início de expressão válido",
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_EXPRESSAO);
        }

        return noAtribuicao;
    }

    private noArvoreDTO parseComandoEscrita() {

        noArvoreDTO noWrite = new noArvoreDTO("Comando", "WRITE");
        match("WRITE");   // <-- SE ESTA LINHA NÃO RODAR, O COMPILADOR TRAVA!
        match("ABREPAR");

        noArvoreDTO expressaoImpressa = parseExpressao();
        if (expressaoImpressa != null) {
            noWrite.addFilho(expressaoImpressa);
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            match("VIRGULA");
            noArvoreDTO proximaExpressao = parseExpressao();
            if (proximaExpressao != null) {
                noWrite.addFilho(proximaExpressao);
            }
        }

        match("FECHAPAR");
        return noWrite;
    }

    private noArvoreDTO parseComandoLeitura() {

        noArvoreDTO noRead = new noArvoreDTO("Comando", "READ");
        match("READ");
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
        }
        return lista;
    }

    private noArvoreDTO expressaoSimples() {

        noArvoreDTO noExpSimples = new noArvoreDTO("expressão simples", "");

        // <op> ::= + | - | EPSILON
        if (tokenAtual.getToken().equals("OPSOMA") || tokenAtual.getToken().equals("OPSUB")) {
            noExpSimples.addFilho(new noArvoreDTO("Op", tokenAtual.getLexema()));
            match(tokenAtual.getToken());
        }

        noArvoreDTO noTermo = parseTermo();
        if (noTermo != null) { noExpSimples.addFilho(noTermo); }

        noArvoreDTO noExpSimplesLinha = parseExpressaoSimplesLinha();
        if  (noExpSimplesLinha != null) { noExpSimples.addFilho(noExpSimplesLinha); }

        return noExpSimples;
    }

    private noArvoreDTO parseExpressaoSimplesLinha() {
        // <op2> ::= + | - | or
        if (FIRST_OP_SOMA_SUB.contains(tokenAtual.getToken())) {

            noArvoreDTO noLinha = new  noArvoreDTO("expressão simples", tokenAtual.getLexema());
            match(tokenAtual.getToken());

            noArvoreDTO noTermo =  parseTermo();
            if (noTermo != null) { noLinha.addFilho(noTermo); }

            noArvoreDTO proxLinha = parseExpressaoSimplesLinha();
            if (proxLinha != null) { noLinha.addFilho(proxLinha); }

            return noLinha;
        }

        return null; // EPSILON
    }

    public noArvoreDTO parseExpressao() {
        noArvoreDTO noEsquerda = expressaoSimples();

        if (FIRST_RELACAO.contains(tokenAtual.getToken())) {

            String operadorRelacional = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();

            noArvoreDTO noRelacional = new noArvoreDTO("Operador Relacional", operadorRelacional);

            match(tokenDoOperador);

            noArvoreDTO noDireito = expressaoSimples();

            if (noEsquerda != null) noRelacional.addFilho(noEsquerda);
            if (noDireito != null) noRelacional.addFilho(noDireito);

            return noRelacional;
        }
        return noEsquerda;
    }

    // <termo> ::= <fator> <termo'>
    public noArvoreDTO parseTermo() {

        noArvoreDTO noTermo =  new noArvoreDTO("Termo", "");
        noArvoreDTO noFator = parseFator();
        if (noFator != null) { noTermo.addFilho(noFator); }

        noArvoreDTO noTermoLinha = parseTermoLinha();
        if (noTermoLinha != null) { noTermo.addFilho(noTermoLinha); }

        return noTermo;
    }

    // <termo'> ::= <op3> <fator> <termo'> | EPSILON
    private noArvoreDTO parseTermoLinha() {
        // <op3> ::= * | div | and
        if (FIRST_OP_MUL_DIV.contains(tokenAtual.getToken())) {

            noArvoreDTO noLinha = new  noArvoreDTO("Termo'", tokenAtual.getLexema());
            match(tokenAtual.getToken());

            noArvoreDTO noFator = parseFator();
            if (noFator != null) { noLinha.addFilho(noFator); }

            noArvoreDTO proxLinha = parseTermoLinha();
            if (proxLinha != null) { noLinha.addFilho(proxLinha); }

            return noLinha;
        }
        return null; // EPSILON
    }

    private noArvoreDTO parseVariavel() {
        noArvoreDTO noVar = new  noArvoreDTO("Variavel", "");
        noVar.addFilho(new noArvoreDTO("Id", tokenAtual.getLexema(), tokenAtual.getLinha()));
        match("IDENTIFICADOR");

        noArvoreDTO noVarLinha = parseVariavelLinha();
        if (noVarLinha != null) { noVar.addFilho(noVarLinha); }

        return noVar;
    }

    private noArvoreDTO parseVariavelLinha() {

        if(tokenAtual.getToken().equals("ABRECOL")) {
            noArvoreDTO noVarlinha = new noArvoreDTO("Variável", "array");
            match("ABRECOL");

            noArvoreDTO noExp = parseExpressao();
            if (noExp != null) { noVarlinha.addFilho(noExp); }
            match("FECHACOL");
            return noVarlinha;
        } else if (tokenAtual.getToken().equals("ABREPAR")) {
            noArvoreDTO noVarLinha = new noArvoreDTO("Variável", "procedimento");
            match("ABREPAR");

            noArvoreDTO lista = parseListaExpressoes();
            if (lista != null) { noVarLinha.addFilho(lista); }
            match("FECHAPAR");
            return noVarLinha;
        }

        return null; // EPSILON
    }

    private noArvoreDTO parseListaExpressoes() {

        noArvoreDTO noLista = new noArvoreDTO("Lista express~eos", "");

        noArvoreDTO exp = parseExpressao();
        if (exp != null) { noLista.addFilho(exp); }

        noArvoreDTO listaLinha = parseListaExpLinha();
        if (listaLinha != null) { noLista.addFilho(listaLinha); }

        return noLista;
    }

    private noArvoreDTO parseListaExpLinha() {

        if (tokenAtual.getToken().equals("VIRGULA")) {
            noArvoreDTO noLinha = new noArvoreDTO("Lista expressões", ",");
            match("VIRGULA");

            noArvoreDTO exp = parseExpressao();
            if (exp != null) { noLinha.addFilho(exp); }

            noArvoreDTO proxLinha = parseListaExpLinha();
            if (proxLinha != null) { noLinha.addFilho(proxLinha); }

            return noLinha;
        }
        return null; // EPSILON
    }


    public noArvoreDTO parseFator() {

        noArvoreDTO noFator = new noArvoreDTO("Fator", "");

        if (!FIRST_FATOR.contains(tokenAtual.getToken())) {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "Início de fator válido",
                    tokenAtual.getToken(),
                    tokenAtual.getLexema(),
                    tokenAtual.getLinha(),
                    tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_EXPRESSAO);
            return null; // Sai rápido, sem nem entrar no switch
        }
        switch (tokenAtual.getToken()) {
            case "IDENTIFICADOR" -> {
                noArvoreDTO noVar = parseVariavel();
                if (noVar != null) { noFator.addFilho(noVar); }
            }
            case "NUM" -> {
                noFator.addFilho(new noArvoreDTO("Num", tokenAtual.getLexema()));
                match(tokenAtual.getToken());
            }
            case "ABRPEAR" -> {
                match("ABREPAR");
                noArvoreDTO noExp = parseExpressao();
                if (noExp != null) { noFator.addFilho(noExp); }
                match("FECHAPAR");
            }
            case "NOT" -> {
                match("NOT");
                noArvoreDTO fNot = parseFator();
                if (fNot != null) { noFator.addFilho(fNot); }
            }
        }
        return noFator;

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
