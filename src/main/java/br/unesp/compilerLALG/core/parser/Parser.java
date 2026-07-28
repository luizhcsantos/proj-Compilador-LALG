package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.noArvore;
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
    private noArvore raizArvore;

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

    //  pendura o terminal na arvore e consome o token
    private void matchEAdiciona(String tipoEsperado, noArvore noPai) {
        if (tokenAtual.getToken().equals(tipoEsperado)) {
            // Adiciona o token como filho da árvore
            if (noPai != null) {
                noPai.addFilho(new noArvore("terminal", tokenAtual.getLexema(), tokenAtual.getLinha()));
            }
            avancar();
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(tipoEsperado, tokenAtual.getToken(), tokenAtual.getLexema(), tokenAtual.getLinha(), tokenAtual.getColunaInicial()));
        }
    }

    // Programa ::= PROGRAM <identificador> ; <bloco> .
    public void parsePrograma() {

        raizArvore = new noArvore("programa", "");

        //raizArvore.addFilho(new noArvore("terminal", "program"));
        matchEAdiciona("PROGRAM", raizArvore);

        noArvore noId = new noArvore("identificador", "identificador", tokenAtual.getLinha());
        raizArvore.addFilho(noId);
        matchEAdiciona("IDENTIFICADOR", noId);

        matchEAdiciona("PONTOVIRGULA", raizArvore);


        if(FIRST_BLOCO.contains(tokenAtual.getToken())) {
            noArvore noBloco = parseBloco();
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

        raizArvore.addFilho(new noArvore("terminal", "."));
        matchEAdiciona("PONTO", raizArvore);
    }

    private noArvore parseBloco() {

        noArvore noBloco = new noArvore("bloco", "");

        // <parte declaração de variáveis>
        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            noArvore declaracoesVars = parsePArteDeclaracaoVAriaveis();
            if (declaracoesVars != null) noBloco.addFilho(declaracoesVars);
        }

        // <parte declaração de subrotinas>
        if (tokenAtual.getToken().equals("PROCEDURE")) {
            noArvore subrotinas = parseDeclaracoesSubrotinas();
            if (subrotinas != null) noBloco.addFilho(subrotinas);
        }

        // <comando composto>
        noArvore comandos = parseComandoComposto();
        if (comandos != null) { noBloco.addFilho(comandos); }

        return noBloco;

    }

    private noArvore parseDeclaracoesSubrotinas() {
        noArvore noSubrotinas = new noArvore("declaração subrotinas", "");

        while (FIRST_DECL_PROC.contains(tokenAtual.getToken())) {
            noArvore proc = parseDeclaracaoProcedimentos();
            if (proc != null) { noSubrotinas.addFilho(proc); }
            matchEAdiciona("PONTOVIRGULA", noSubrotinas);
        }
        return noSubrotinas.getFilhos().isEmpty() ? null : noSubrotinas; // se não tem subrotinas, retorna null
    }

    private noArvore parseParteDeclaracoesSubrotinasLinha() {

        return null;
    }

    private noArvore parseDeclaracaoProcedimentos() {

        noArvore noProc = new noArvore("procedimento", "");

        // consome a palavra reservada 'procedure'
        // consome 'identificador' - nome do procedimento
        // lista de palametros formais - pode ser opcional
        //      ( seção de parametros formais - parametros formais' )
        //          [ var ] <lista de identificadores> : <tipo> { ; <lista de identificadores> : <tipo> }
        //              lista de identificadores ::= identificador { , identificador }
        // consome ';'
        // comando / comando composto
        matchEAdiciona("PROCEDURE", noProc);
        String nomeProcedure = tokenAtual.getLexema();
        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            noProc.addFilho(new noArvore("nome", nomeProcedure));
            match("IDENTIFICADOR");
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_DECL_PROC);
        }

        if (tokenAtual.getToken().equals("ABREPAR")) {
            noArvore params = parseParametrosFormais();
            if (params != null) {
                noProc.addFilho(params);
            }
        }
        matchEAdiciona("PONTOVIRGULA", noProc);

        noArvore bloco = parseBloco();
        if (bloco != null) { noProc.addFilho(bloco); }

        return noProc;
    }

    private noArvore parseDeclaracaoProcedimentosLinha() {
        return null;
    }

    private noArvore parseParametrosFormais() {
        noArvore noParams = new noArvore("parametros formais", "");
        matchEAdiciona("ABREPAR", noParams);

        noArvore secao = parseSEcaoParametrosFormais();
        if (secao != null) {
            noParams.addFilho(secao);
        }

        while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
            matchEAdiciona( "PONTOVIRGULA", noParams);
            noArvore secao2 = parseSEcaoParametrosFormais();
            if (secao2 != null) {
                noParams.addFilho(secao2);
            }
        }
        matchEAdiciona("FECHAPAR", noParams);
        return noParams;
    }

    private noArvore parseParametrosFormaisLinha() {
        return null;
    }

    private noArvore parseSEcaoParametrosFormais() {

        noArvore noSecao = new noArvore("seção", "");

        if (tokenAtual.getToken().equals("VAR")) {
            noSecao.addFilho(new noArvore("modificador", "var"));
            matchEAdiciona("VAR", noSecao);
        }

        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            noSecao.addFilho(new noArvore("identificador", tokenAtual.getLexema()));
            match("IDENTIFICADOR");
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            match("VIRGULA");
            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                noSecao.addFilho(new noArvore("identificador", tokenAtual.getLexema()));
                match("IDENTIFICADOR");
            }
        }
        matchEAdiciona("DOISPONTOS", noSecao);

        if (tokenAtual.getToken().equals("INT") || tokenAtual.getToken().equals("BOOLEAN")) {
            noSecao.addFilho(new noArvore("tipo", tokenAtual.getLexema()));
            match(tokenAtual.getToken());
        } else if (tokenAtual.getToken().equals("IDENTIFICADOR")) { // Algumas variantes de LALG aceitam identificador de tipo
            noSecao.addFilho(new noArvore("tipo", tokenAtual.getLexema()));
            match("IDENTIFICADOR");
        }
        return noSecao;
    }


    // <parte_de_declarações_de_variáveis> ::= <declaração_de_variáveis> ; <declaração_de_variáveis'> | EPSILON
    private noArvore parsePArteDeclaracaoVAriaveis() {

        noArvore noParte = new  noArvore("parte declaração variáveis", "");

        // <declaração de variáveis>
        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            noArvore declaracoesVars = parseDeclaracaoVariaveis();
            if (declaracoesVars != null) { noParte.addFilho(declaracoesVars); }

            // ;
            if (tokenAtual.getToken().equals("PONTOVIRGULA")) {
                matchEAdiciona("PONTOVIRGULA", noParte);
            }

            // <declaração_de_variáveis'>
            noArvore noDeclLinha = parseDeclaracaoVariaveisLinha();
            if (noDeclLinha != null) noParte.addFilho(noDeclLinha);

            return noParte;
        }

        return null; // epsilon
    }

    // <declaração_de_variáveis> ::= <tipo> <lista_de_identificadores>
    private noArvore parseDeclaracaoVariaveis() {

        noArvore noDeclaracoes = new noArvore("declaração variáveis", "");

        // Lê a palavra 'int' ou 'boolean'
        String tipoVar = tokenAtual.getLexema();
        String tokenTipo = tokenAtual.getToken();

        noArvore noTipo0 = new noArvore("tipo", "tipo");
        noDeclaracoes.addFilho(noTipo0);

        noArvore noTipo = new noArvore("tipo", tipoVar);
        noTipo0.addFilho(noTipo);
        //noDeclaracoes.addFilho(noTipo);
        match(tokenTipo);

        noArvore noListaIds = parseListaIdentificadores();
        if (noListaIds != null) noDeclaracoes.addFilho(noListaIds);

        return noDeclaracoes;
    }

    private noArvore parseDeclaracaoVariaveisLinha() {

        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            noArvore noDeclLinha = new noArvore("declaração de variáveis'", "");

            noArvore noDecl = parseDeclaracaoVariaveis();
            if (noDecl != null) noDeclLinha.addFilho(noDecl);


            noArvore proxLinha = parseDeclaracaoVariaveisLinha(); // Recursão
            if (proxLinha != null) noDeclLinha.addFilho(proxLinha);

            matchEAdiciona("PONTOVIRGULA", noDeclLinha);

            return noDeclLinha;
        }
        return null;
    }

    private noArvore parseListaIdentificadores() {

        noArvore noListaIds = new  noArvore("lista de identificadores", "");
        // <identificador>
        if (tokenAtual.getToken().equalsIgnoreCase("IDENTIFICADOR")) {
            noArvore noVAr = new  noArvore("variavel", tokenAtual.getLexema(), tokenAtual.getLinha());
            noListaIds.addFilho(noVAr);
            matchEAdiciona("IDENTIFICADOR", noVAr);
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_LISTA_ID);
            return  noListaIds;
        }

        //<lista identificadores'>
        noArvore noListaLinha = parseListaIdentificadoresLinha();
        if (noListaLinha != null) noListaIds.addFilho(noListaLinha);


        return noListaIds;
    }

    private noArvore parseListaIdentificadoresLinha() {

        // ,
        if(tokenAtual.getToken().equalsIgnoreCase("VIRGULA")) {
            noArvore noListaLinha = new noArvore("lista de identificadores'", "");
            matchEAdiciona("VIRGULA", noListaLinha);

            // <identificador>
            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                noArvore noVAr = new noArvore("variável", "");
                noListaLinha.addFilho(noVAr);
                matchEAdiciona("IDENTIFICADOR", noVAr);
            } else {
                listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                        "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                        tokenAtual.getLinha(), tokenAtual.getColunaInicial()
                ));
                sincronizar(FOLLOW_DECL_VAR);
                return noListaLinha;
            }
            // <lista identificadores '>
            noArvore proxLinha = parseListaIdentificadoresLinha();
            if (proxLinha != null) noListaLinha.addFilho(proxLinha);

            return noListaLinha;
        }

        return null; // epsilon
    }

    // <comando> ::= <comando_atribuicao> | <comando_leitura> | <comando_escrita>
    public noArvore parseComando() {

        noArvore noComando = new noArvore("comando", "");

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
                int linhaCmdId = tokenAtual.getLinha();
                match("IDENTIFICADOR");

                if (tokenAtual.getToken().equals("ATRIBUICAO")) { // :=
                    return parseComandoAtribuicao(nomeVariavelOuProcedimento, linhaCmdId);
                } else if (tokenAtual.getToken().equals("ABREPAR")) {

                    noArvore chamadaNode = new noArvore("chamada procedimento", nomeVariavelOuProcedimento);

                    matchEAdiciona("ABREPAR", chamadaNode);

                    noArvore parametros = parseListaExpressoes();

                    if (parametros != null) { chamadaNode.addFilho(parametros); }
                    matchEAdiciona("FECHAPAR", chamadaNode);

                    return chamadaNode;
                } else {
                    // Se não for := nem (, é uma chamada de procedimento sem parâmetros
                    return new noArvore("chamada procedimento", nomeVariavelOuProcedimento);
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

    private noArvore parseComandoComposto() {

        noArvore noComando = new noArvore("comando composto", "");
        matchEAdiciona("BEGIN", noComando);

        noArvore primeiroCmd = parseComando();
        if (primeiroCmd != null) {
            noComando.addFilho(primeiroCmd);
        }

        noArvore cmdLinha = parseComandoCompostoLinha();
        if (cmdLinha != null) {
            noComando.addFilho(cmdLinha);
        }

        matchEAdiciona("END", noComando);
        return noComando;
    }

    private noArvore parseComandoCompostoLinha() {

        if (tokenAtual.getToken().equals("PONTOVIRGULA")) {
            noArvore noLinha = new noArvore("comando composto'", "");

            // pendura o ponto e vírgula na árvore
            matchEAdiciona("PONTOVIRGULA", noLinha);

            if (tokenAtual.getToken().equals("END")) {
                return noLinha;
            }

            // pega o comando seguinte
            noArvore proxCmd = parseComando();
            if (proxCmd != null) { noLinha.addFilho(proxCmd); }

            // chama ele proprio para ver se há mais ;
            noArvore proxLinha = parseComandoCompostoLinha();
            if (proxLinha != null) { noLinha.addFilho(proxLinha); }

            return noLinha;
        }

        // epsilon
        return null;
    }

    // <comando repetitivo 1> ::= while <expressão> do <comando>
    private noArvore parseComandoWhile() {

        noArvore noWhile = new noArvore("comando repetitivo 1", "while");
        matchEAdiciona("WHILE", noWhile);

        noArvore noCondicao = new noArvore("condição (expressão)", "");
        noArvore expCondicao = parseExpressao();
        if (expCondicao != null) {
            noCondicao.addFilho(expCondicao);
        }
        noWhile.addFilho(noCondicao);

        matchEAdiciona("DO", noWhile);

        noArvore noCorpo = new noArvore("corpo do while (do) (comando)", "");
        noArvore cmdCorpo = parseComando();
        if (cmdCorpo != null) {
            noCorpo.addFilho(cmdCorpo);
        }
        noWhile.addFilho(noCorpo);

        return noWhile;
    }

    // <comando condicional1> ::= if <expressão> then <comando> [ else <comando> ]
    private noArvore parseComandoIf() {

        noArvore noIf = new noArvore("comando condicional 1", "if");

        matchEAdiciona( "IF", noIf);

        // condição
        noArvore noCondicao = new noArvore("condição", "");
        noArvore expCondicao = parseExpressao();
        if (expCondicao != null) {
            noCondicao.addFilho(expCondicao);
        }
        noIf.addFilho(noCondicao);

        matchEAdiciona("THEN", noIf);

        // verdadeiro
        noArvore noVerdadeiro = new noArvore("verdadeiro (then)", "");
        noArvore cmdVerdadeiro = parseComando();
        if (cmdVerdadeiro != null) {
            noVerdadeiro.addFilho(cmdVerdadeiro);
        }
        noIf.addFilho(noVerdadeiro);

        // else (opcional)
        if (tokenAtual.getToken().equals("ELSE")) {
            matchEAdiciona("ELSE", noIf);

            noArvore noFalso = new noArvore("falso (else)", "");
            noArvore cmdFalso = parseComando();
            if (cmdFalso != null) {
                noFalso.addFilho(cmdFalso);
            }
            noIf.addFilho(noFalso);
        }

        return noIf;
    }

    private noArvore parseComandoAtribuicao(String nomeVariavel, int linha) {

        noArvore noAtribuicao = new noArvore("atribuição", "");

        noArvore terminalVar = new noArvore("variável", nomeVariavel, linha);

        noAtribuicao.addFilho(terminalVar);

        matchEAdiciona("ATRIBUICAO", noAtribuicao);

//        noArvore terminalSinal = new noArvore("símbolo", ":=");
//        noAtribuicao.addFilho(terminalSinal);


        if (FIRST_EXPRESSAO.contains(tokenAtual.getToken())) {
            noArvore resultadoMatematica = parseExpressao();
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

    private noArvore parseComandoEscrita() {

        noArvore noWrite = new noArvore("comando", "WRITE");
        matchEAdiciona("WRITE", noWrite);
        matchEAdiciona("ABREPAR", noWrite);

        noArvore expressaoImpressa = parseExpressao();
        if (expressaoImpressa != null) {
            noWrite.addFilho(expressaoImpressa);
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            matchEAdiciona("VIRGULA", noWrite);
            noArvore proximaExpressao = parseExpressao();
            if (proximaExpressao != null) {
                noWrite.addFilho(proximaExpressao);
            }
        }

        matchEAdiciona("FECHAPAR", noWrite);
        return noWrite;
    }

    private noArvore parseComandoLeitura() {

        noArvore noRead = new noArvore("comando", "READ");
        matchEAdiciona("READ", noRead);
        matchEAdiciona("ABREPAR", noRead);

        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            noRead.addFilho(new noArvore("variável lida", tokenAtual.getLexema()));
            match("IDENTIFICADOR");
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
        }

        while (tokenAtual.getToken().equals("VIRGULA")) {
            matchEAdiciona("VIRGULA", noRead);
            if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
                noRead.addFilho(new noArvore("variável lida", tokenAtual.getLexema(), tokenAtual.getLinha()));
                match("IDENTIFICADOR");
            }
        }

        matchEAdiciona("FECHAPAR", noRead);
        return noRead;
    }

    // <lista_comandos> ::= <comando> { ; <comando> }
    public noArvore parseListaComandos() {

        noArvore lista = new noArvore("lista de comandos", "");

        noArvore cmd = parseComando();
        if (cmd != null) {
            lista.addFilho(cmd);
        }


        while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
            matchEAdiciona("PONTOVIRGULA", lista);

            if (tokenAtual.getToken().equals("END")) break;

            noArvore proximoCmd = parseComando();
            if (proximoCmd != null) {
                lista.addFilho(proximoCmd);
            }
        }
        return lista;
    }

    private noArvore expressaoSimples() {

        noArvore noExpSimples = new noArvore("expressão simples", "");

        // <op> ::= + | - | EPSILON
        if (tokenAtual.getToken().equals("OPSOMA") || tokenAtual.getToken().equals("OPSUB")) {
            noExpSimples.addFilho(new noArvore("Op", tokenAtual.getLexema()));
            matchEAdiciona(tokenAtual.getToken(), noExpSimples);
        }

        noArvore noTermo = parseTermo();
        if (noTermo != null) { noExpSimples.addFilho(noTermo); }

        noArvore noExpSimplesLinha = parseExpressaoSimplesLinha();
        if  (noExpSimplesLinha != null) { noExpSimples.addFilho(noExpSimplesLinha); }

        return noExpSimples;
    }

    private noArvore parseExpressaoSimplesLinha() {
        // <op2> ::= + | - | or
        if (FIRST_OP_SOMA_SUB.contains(tokenAtual.getToken())) {
            noArvore noLinha = new noArvore("expressão simples", tokenAtual.getLexema());
            matchEAdiciona(tokenAtual.getToken(), noLinha);

            noArvore noTermo =  parseTermo();
            if (noTermo != null) { noLinha.addFilho(noTermo); }

            noArvore proxLinha = parseExpressaoSimplesLinha();
            if (proxLinha != null) { noLinha.addFilho(proxLinha); }

            return noLinha;
        }

        return null; // EPSILON
    }

    public noArvore parseExpressao() {
        noArvore noEsquerda = expressaoSimples();

        if (FIRST_RELACAO.contains(tokenAtual.getToken())) {

            String operadorRelacional = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();

            noArvore noRelacional = new noArvore("operador relacional", operadorRelacional);

            matchEAdiciona(tokenDoOperador, noRelacional);

            noArvore noDireito = expressaoSimples();

            if (noEsquerda != null) noRelacional.addFilho(noEsquerda);
            if (noDireito != null) noRelacional.addFilho(noDireito);

            return noRelacional;
        }
        return noEsquerda;
    }

    // <termo> ::= <fator> <termo'>
    public noArvore parseTermo() {

        noArvore noTermo =  new noArvore("termo", "");
        noArvore noFator = parseFator();
        if (noFator != null) { noTermo.addFilho(noFator); }

        noArvore noTermoLinha = parseTermoLinha();
        if (noTermoLinha != null) { noTermo.addFilho(noTermoLinha); }

        return noTermo;
    }

    // <termo'> ::= <op3> <fator> <termo'> | EPSILON
    private noArvore parseTermoLinha() {
        // <op3> ::= * | div | and
        if (FIRST_OP_MUL_DIV.contains(tokenAtual.getToken())) {

            noArvore noLinha = new noArvore("termo'", tokenAtual.getLexema());
            matchEAdiciona(tokenAtual.getToken(), noLinha);

            noArvore noFator = parseFator();
            if (noFator != null) { noLinha.addFilho(noFator); }

            noArvore proxLinha = parseTermoLinha();
            if (proxLinha != null) { noLinha.addFilho(proxLinha); }

            return noLinha;
        }
        return null; // EPSILON
    }

    private noArvore parseVariavel() {
        noArvore noVar = new noArvore("variavel", "");
        noVar.addFilho(new noArvore("Id", tokenAtual.getLexema(), tokenAtual.getLinha()));
        match("IDENTIFICADOR");

        noArvore noVarLinha = parseVariavelLinha();
        if (noVarLinha != null) { noVar.addFilho(noVarLinha); }

        return noVar;
    }

    private noArvore parseVariavelLinha() {

        if(tokenAtual.getToken().equals("ABRECOL")) {
            noArvore noVarlinha = new noArvore("variável", "array");
            match("ABRECOL");

            noArvore noExp = parseExpressao();
            if (noExp != null) { noVarlinha.addFilho(noExp); }
            match("FECHACOL");
            return noVarlinha;
        } else if (tokenAtual.getToken().equals("ABREPAR")) {
            noArvore noVarLinha = new noArvore("variável", "procedimento");
            matchEAdiciona("ABREPAR", noVarLinha);

            noArvore lista = parseListaExpressoes();
            if (lista != null) { noVarLinha.addFilho(lista); }
            matchEAdiciona("FECHAPAR", noVarLinha);
            return noVarLinha;
        }

        return null; // EPSILON
    }

    private noArvore parseListaExpressoes() {

        noArvore noLista = new noArvore("lista expressões", "");
        noArvore exp = parseExpressao();
        if (exp != null) { noLista.addFilho(exp); }

        noArvore listaLinha = parseListaExpLinha();
        if (listaLinha != null) { noLista.addFilho(listaLinha); }

        return noLista;
    }

    private noArvore parseListaExpLinha() {

        if (tokenAtual.getToken().equals("VIRGULA")) {
            noArvore noLinha = new noArvore("lista expressões", ",");
            matchEAdiciona("VIRGULA", noLinha);

            noArvore exp = parseExpressao();
            if (exp != null) { noLinha.addFilho(exp); }

            noArvore proxLinha = parseListaExpLinha();
            if (proxLinha != null) { noLinha.addFilho(proxLinha); }

            return noLinha;
        }
        return null; // EPSILON
    }


    public noArvore parseFator() {

        noArvore noFator = new noArvore("fator", "");

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
                noArvore noVar = parseVariavel();
                if (noVar != null) { noFator.addFilho(noVar); }
            }
            case "NUM" -> {
                noFator.addFilho(new noArvore("num", tokenAtual.getLexema()));
                match("NUM");
            }
            case "ABREPAR" -> {
                matchEAdiciona("ABREPAR",  noFator);
                noArvore noExp = parseExpressao();
                if (noExp != null) { noFator.addFilho(noExp); }
                matchEAdiciona("FECHAPAR", noFator);
            }
            case "NOT" -> {
                matchEAdiciona("NOT", noFator);
                noArvore fNot = parseFator();
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

    public noArvore getRaizArvore() {
        return raizArvore;
    }
}
