package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.exception.CompilerException;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
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

    private final Set<String> FIRST_COMANDO_COMPOSTO =  Set.of(
            "BEGIN"
    );

    private final Set<String> FIRST_COMPOST_LINHA = Set.of(
            "PONTOVIRGULA"
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

    private final Set<String> FIRST_PARAM_FORM = Set.of(
            "ABREPAR", "PONTOVIRGULA"
    );

    private final Set<String> FIRST_SECAO_PARAM = Set.of(
            "VAR", "IDENTIFICADOR"
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

    // FOLLOW
    private final Set<String> FOLLOW_PARAM_FORM = Set.of(
            "PONTOVIRGULA"
    );

    private final Set<String> FOLLOW_SECAO_PARAM = Set.of(
            "PONTOVIRGULA", "FECHAPAR"
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

        matchEAdiciona("PROGRAM", raizArvore);

        String nomePrograma = tokenAtual.getLexema();
        noArvore noId = new noArvore("identificador", nomePrograma);
        raizArvore.addFilho(noId);
        matchEAdiciona("IDENTIFICADOR", noId);

        matchEAdiciona("PONTOVIRGULA", raizArvore);


        if (FIRST_BLOCO.contains(tokenAtual.getToken())) {
            noArvore noBloco = parseBloco();
            raizArvore.addFilho(noBloco);
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

        //raizArvore.addFilho(new noArvore("terminal", "."));
        matchEAdiciona("PONTO", raizArvore);
    }

    // <bloco> ::= <parte declaração variáveis> <parte declaração subrotinas> <comando composto>
    private noArvore parseBloco() {

        noArvore noBloco = new noArvore("bloco", "");

        // <parte declaração de variáveis>
        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            noArvore declaracoesVars = parsePArteDeclaracaoVAriaveis();
            if (declaracoesVars != null) noBloco.addFilho(declaracoesVars);
        }

        // <parte declaração de subrotinas>
        if (tokenAtual.getToken().equals("PROCEDURE")) {
            noArvore subrotinas = parseParteDeclaracaoSubrotinas();
            if (subrotinas != null) noBloco.addFilho(subrotinas);
        }

        // <comando composto>
        noArvore comandos = parseComandoComposto();
        if (comandos != null) {
            noBloco.addFilho(comandos);
        }

        return noBloco;

    }

    private noArvore parseDeclaracoesSubrotinas() {
        noArvore noSubrotinas = new noArvore("declaração subrotinas", "");

        while (FIRST_DECL_PROC.contains(tokenAtual.getToken())) {
            noArvore proc = parseDeclaracaoProcedimentos();
            if (proc != null) {
                noSubrotinas.addFilho(proc);
            }
            matchEAdiciona("PONTOVIRGULA", noSubrotinas);
        }
        return noSubrotinas.getFilhos().isEmpty() ? null : noSubrotinas; // se não tem subrotinas, retorna null
    }

    // <parte declaração subrotinas> ::= <declaração procedimentos> ; <declaração procedimentos '> | EPSILON
    private noArvore parseParteDeclaracaoSubrotinas() {
        if (FIRST_DECL_PROC.contains(tokenAtual.getToken())) {
            noArvore noParte = new noArvore("parte declaração subrotinas'", "");

            noArvore noDeclar = parseDeclaracaoProcedimentos();
            if (noDeclar != null) noParte.addFilho(noDeclar);

            matchEAdiciona("PONTOVIRGULA", noParte);

            noArvore noSubLinha = parseParteDeclaracaoSubrotinasLinha();
            if (noSubLinha != null) {
                noParte.addFilho(noSubLinha);
            }

            return noParte;
        }
        return null; // epsilon
    }

    // <parte_de_declarações_de_subrotinas'> ::= <declaração_de_procedimento> ; <parte_de_declarações_de_subrotinas'> | EPSILON
    private noArvore parseParteDeclaracaoSubrotinasLinha() {
        if (FIRST_DECL_PROC.contains(tokenAtual.getToken())) {
            noArvore noSubLinha = new noArvore("parte de declarações de subrotinas'", "");

            noArvore noDecl = parseDeclaracaoProcedimentos();
            if (noDecl != null) {
                noSubLinha.addFilho(noDecl);
            }

            matchEAdiciona("PONTOVIRGULA", noSubLinha);

            noArvore proxLinha = parseParteDeclaracaoSubrotinasLinha(); // Recursão à direita
            if (proxLinha != null) {
                noSubLinha.addFilho(proxLinha);
            }

            return noSubLinha;
        }
        return null; // EPSILON
    }

    // <declaração procedimentos> ::= PROCEDURE <identificador> <parâmetros formais> ; <bloco>
    private noArvore parseDeclaracaoProcedimentos() {

        noArvore noProc = new noArvore("declaração procedimento", "");

        matchEAdiciona("PROCEDURE", noProc);
        //String nomeProcedure = tokenAtual.getLexema();
        if (tokenAtual.getToken().equals("IDENTIFICADOR")) {
            noArvore noId = new noArvore("identificador", "identificador", tokenAtual.getLinha());
            noProc.addFilho(noId);
            //noProc.addFilho(new noArvore("nome", nomeProcedure));
            matchEAdiciona("IDENTIFICADOR", noId);
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_DECL_PROC);
        }

        noArvore noParams = parseParametrosFormais();
        if (noParams != null) {
            noProc.addFilho(noParams);
        }

        matchEAdiciona("PONTOVIRGULA", noProc);

        noArvore bloco = parseBloco();
        noProc.addFilho(bloco);

        return noProc;
    }

    private noArvore parseParametrosFormais() {
        if (FIRST_PARAM_FORM.contains(tokenAtual.getToken())) {
            noArvore noParams = new noArvore("parametros formais", "");
            matchEAdiciona("ABREPAR", noParams);

            noArvore secao = parseSecaoParametrosFormais();
            noParams.addFilho(secao);

            while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
                matchEAdiciona("PONTOVIRGULA", noParams);
                noArvore secao2 = parseSecaoParametrosFormais();
                noParams.addFilho(secao2);
            }
            matchEAdiciona("FECHAPAR", noParams);
            return noParams;
        }

        return null;
    }

    private noArvore parseParametrosFormaisLinha() {
        return null;
    }

    // <seção parâmetros formais> ::= var <lista identificadores> : <identificador>
    private noArvore parseSecaoParametrosFormais() {

        noArvore noSecao = new noArvore("seção parametros formais", "");

        if (!FIRST_SECAO_PARAM.contains(tokenAtual.getToken())) {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "VAR ou IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_SECAO_PARAM);
            return noSecao;
        }

        if (tokenAtual.getToken().equalsIgnoreCase("VAR")) {
            matchEAdiciona("VAR", noSecao);
        }

        noArvore noListaIds = parseListaIdentificadores();
        noSecao.addFilho(noListaIds);

        matchEAdiciona("DOISPONTOS", noSecao);

        noArvore noTipo = new noArvore("tipo", "");
        noSecao.addFilho(noTipo);
        matchEAdiciona(tokenAtual.getToken(), noTipo);

        return noSecao;
    }


    // <parte_de_declarações_de_variáveis> ::= <declaração_de_variáveis> ; <declaração_de_variáveis'> | EPSILON
    private noArvore parsePArteDeclaracaoVAriaveis() {

        noArvore noParte = new noArvore("parte declaração variáveis", "");

        // <declaração de variáveis>
        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            noArvore declaracoesVars = parseDeclaracaoVariaveis();
            noParte.addFilho(declaracoesVars);

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
        noDeclaracoes.addFilho(noListaIds);

        return noDeclaracoes;
    }

    private noArvore parseDeclaracaoVariaveisLinha() {

        if (FIRST_DECL_VAR.contains(tokenAtual.getToken())) {
            noArvore noDeclLinha = new noArvore("declaração de variáveis'", "");

            noArvore noDecl = parseDeclaracaoVariaveis();
            noDeclLinha.addFilho(noDecl);


            noArvore proxLinha = parseDeclaracaoVariaveisLinha(); // Recursão
            if (proxLinha != null) noDeclLinha.addFilho(proxLinha);

            matchEAdiciona("PONTOVIRGULA", noDeclLinha);

            return noDeclLinha;
        }
        return null;
    }

    private noArvore parseListaIdentificadores() {

        noArvore noListaIds = new noArvore("lista de identificadores", "");
        // <identificador>
        if (tokenAtual.getToken().equalsIgnoreCase("IDENTIFICADOR")) {
            noArvore noVAr = new noArvore("variável", tokenAtual.getLexema(), tokenAtual.getLinha());
            noListaIds.addFilho(noVAr);
            matchEAdiciona("IDENTIFICADOR", noVAr);
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                    tokenAtual.getLinha(), tokenAtual.getColunaInicial()
            ));
            sincronizar(FOLLOW_LISTA_ID);
            return noListaIds;
        }

        //<lista identificadores'>
        noArvore noListaLinha = parseListaIdentificadoresLinha();
        if (noListaLinha != null) noListaIds.addFilho(noListaLinha);


        return noListaIds;
    }

    private noArvore parseListaIdentificadoresLinha() {

        // ,
        if (tokenAtual.getToken().equalsIgnoreCase("VIRGULA")) {
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
            return null; // sai do metodo para evitar cascata de erros
        }

        switch (tokenAtual.getToken()) {
            case "IDENTIFICADOR", "" -> {

                noArvore noAtribOuChamada = parseAtribuicaoOuChamadaDeProcedimento();
                if (noAtribOuChamada != null) { noComando.addFilho(noAtribOuChamada); }
            }
            case "READ" -> {
                noArvore noEScrita = parseComandoLeitura();
                noComando.addFilho(noEScrita);
            }
            case "WRITE" -> {
                noArvore noLeitura = parseComandoEscrita();
                noComando.addFilho(noLeitura);
            }
            case "BEGIN" -> {
                noArvore noComp = parseComandoComposto();
                if (noComp != null) { noComando.addFilho(noComp); }
            }
            case "IF" -> {
                noArvore noIf = parseComandoIf();
                noComando.addFilho(noIf);
            }
            case "WHILE" -> {
                noArvore noWhile = parseComandoWhile();
                noComando.addFilho(noWhile);
            }
        }
        return noComando;
    }

    private noArvore parseComandoComposto() {

        if (FIRST_COMANDO_COMPOSTO.contains(tokenAtual.getToken())) {
            noArvore noComposto = new noArvore("comando composto", "");
            matchEAdiciona("BEGIN", noComposto);

            noArvore primeiroCmd = parseComando();
            if (primeiroCmd != null) { noComposto.addFilho(primeiroCmd); }

            noArvore cmdCompostoLinha = parseComandoCompostoLinha();
            if (cmdCompostoLinha != null) {
                noComposto.addFilho(cmdCompostoLinha);
            }

            matchEAdiciona("END", noComposto);
            return noComposto;
        }
        return null; // epsilon
    }

    private noArvore parseComandoCompostoLinha() {

        if (FIRST_COMPOST_LINHA.contains(tokenAtual.getToken())) {
            noArvore noCompostoLinha = new noArvore("comando composto '", "");

            // pendura o ponto e vírgula na árvore
            matchEAdiciona("PONTOVIRGULA", noCompostoLinha);

            if (tokenAtual.getToken().equals("END")) { return noCompostoLinha; }

            if(FIRST_COMANDO.contains(tokenAtual.getToken())) {
                // pega o comando seguinte
                noArvore proxCmd = parseComando();
                if (proxCmd != null) {
                    noCompostoLinha.addFilho(proxCmd);
                }
                // chama ele proprio para ver se há mais ; ou end
                noArvore proxLinha = parseComandoCompostoLinha();
                if (proxLinha != null) {
                    noCompostoLinha.addFilho(proxLinha);
                }
                return noCompostoLinha;
            }
        }
        // epsilon
        return null;
    }

    private noArvore parseAtribuicaoOuChamadaDeProcedimento() {
       noArvore noComando = new noArvore("comando", "");

       if(tokenAtual.getToken().equalsIgnoreCase("IDENTIFICADOR")){
           String nomeReal = tokenAtual.getLexema();
           noArvore noId = new  noArvore("identificador", nomeReal);
           noComando.addFilho(noId);
           matchEAdiciona("IDENTIFICADOR", noId);
       }
       else {
           listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                   "IDENTIFICADOR", tokenAtual.getToken(), tokenAtual.getLexema(),
                   tokenAtual.getLinha(), tokenAtual.getColunaInicial()
           ));
           sincronizar(FOLLOW_COMANDO);
           return noComando;
       }

       if (tokenAtual.getToken().equals("ATRIBUICAO")) {
           noComando.setNome("atribuição");

           matchEAdiciona("ATRIBUICAO", noComando);

           noArvore expressao = parseExpressao();
           if (expressao != null) { noComando.addFilho(expressao); }
       }

       else if(tokenAtual.getToken().equals("ABREPAR")) {
           noComando.setNome("chamada ed procedimento");

           matchEAdiciona("ABREPAR", noComando);

           noArvore noListaExp =  parseListaExpressoes();
           noComando.addFilho(noListaExp);

           matchEAdiciona("FECHAPAR", noComando);
       }
       else {
           noComando.setNome("chamada de procedimento");
       }

        return noComando;
    }

    // <comando repetitivo 1> ::= while <expressão> do <comando>
    private noArvore parseComandoWhile() {

        noArvore noWhile = new noArvore("comando repetitivo 1", "while");
        matchEAdiciona("WHILE", noWhile);

        noArvore expCondicao = parseExpressao();
        if (expCondicao != null){
            noWhile.addFilho(expCondicao);
        }
        matchEAdiciona("DO", noWhile);

        noArvore cmdCorpo = parseComando();
        if (cmdCorpo != null){
            noWhile.addFilho(cmdCorpo);
        }

        return noWhile;
    }

    // <comando condicional1> ::= if <expressão> then <comando> [ else <comando> ]
    private noArvore parseComandoIf() {

        if (tokenAtual.getToken().equals("IF")) {
            noArvore noIf = new noArvore("comando condicional", "");

            // Consome o 'if'
            matchEAdiciona("IF", noIf);

            // Avalia a condição (<expressão>)
            noArvore noExp = parseExpressao();
            if (noExp != null) { noIf.addFilho(noExp); }

            // Consome o 'then'
            matchEAdiciona("THEN", noIf);

            // Bloco de comandos caso verdadeiro (<comando>)
            noArvore noCmdThen = parseComando();
            if (noCmdThen != null) { noIf.addFilho(noCmdThen); }

            // Verifica a parte OPCIONAL (Bloco 'else')
            if (tokenAtual.getToken().equals("ELSE")) {
                matchEAdiciona("ELSE", noIf);

                noArvore noCmdElse = parseComando();
                if (noCmdElse != null) { noIf.addFilho(noCmdElse); }
            }

            return noIf;
        }
        return null;
    }

    private noArvore parseComandoAtribuicao(String nomeVariavel, int linha) {

        noArvore noAtribuicao = new noArvore("atribuição", "");

        noArvore terminalVar = new noArvore("variável", nomeVariavel, linha);

        noAtribuicao.addFilho(terminalVar);

        matchEAdiciona("ATRIBUICAO", noAtribuicao);


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
        if (noTermo != null) {
            noExpSimples.addFilho(noTermo);
        }

        noArvore noExpSimplesLinha = parseExpressaoSimplesLinha();
        if (noExpSimplesLinha != null) {
            noExpSimples.addFilho(noExpSimplesLinha);
        }

        return noExpSimples;
    }

    private noArvore parseExpressaoSimplesLinha() {
        // <op2> ::= + | - | or
        if (FIRST_OP_SOMA_SUB.contains(tokenAtual.getToken())) {
            noArvore noLinha = new noArvore("expressão simples", tokenAtual.getLexema());
            matchEAdiciona(tokenAtual.getToken(), noLinha);

            noArvore noTermo = parseTermo();
            if (noTermo != null) {
                noLinha.addFilho(noTermo);
            }

            noArvore proxLinha = parseExpressaoSimplesLinha();
            if (proxLinha != null) {
                noLinha.addFilho(proxLinha);
            }

            return noLinha;
        }

        return null; // EPSILON
    }

    public noArvore parseExpressao() {

        noArvore expPai = new noArvore("expressão", "");

        noArvore noEsquerdo = expressaoSimples();
        expPai.addFilho(noEsquerdo);

        if (FIRST_RELACAO.contains(tokenAtual.getToken())) {

            String operadorRelacional = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();

            noArvore noRelacional = new noArvore("relação", operadorRelacional);

            matchEAdiciona(tokenDoOperador, noRelacional);

            noArvore noDireito = expressaoSimples();

            expPai.addFilho(noRelacional);
            expPai.addFilho(noDireito);

            return expPai;
        }
        return expPai;
    }

    // <termo> ::= <fator> <termo'>
    public noArvore parseTermo() {

        noArvore noTermo = new noArvore("termo", "");
        noArvore noFator = parseFator();
        if (noFator != null) {
            noTermo.addFilho(noFator);
        }

        noArvore noTermoLinha = parseTermoLinha();
        if (noTermoLinha != null) {
            noTermo.addFilho(noTermoLinha);
        }

        return noTermo;
    }

    // <termo'> ::= <op3> <fator> <termo'> | EPSILON
    private noArvore parseTermoLinha() {
        // <op3> ::= * | div | and
        if (FIRST_OP_MUL_DIV.contains(tokenAtual.getToken())) {

            noArvore noLinha = new noArvore("termo'", tokenAtual.getLexema());
            matchEAdiciona(tokenAtual.getToken(), noLinha);

            noArvore noFator = parseFator();
            if (noFator != null) {
                noLinha.addFilho(noFator);
            }

            noArvore proxLinha = parseTermoLinha();
            if (proxLinha != null) {
                noLinha.addFilho(proxLinha);
            }

            return noLinha;
        }
        return null; // EPSILON
    }

    private noArvore parseVariavel() {


        String nomeVariavel = tokenAtual.getLexema();
        noArvore noVar = new noArvore("variável", nomeVariavel);
        //noVar.addFilho(new noArvore("Id", tokenAtual.getLexema(), tokenAtual.getLinha()));
        matchEAdiciona("IDENTIFICADOR", noVar);

        noArvore noVarLinha = parseVariavelLinha();
        if (noVarLinha != null) {
            noVar.addFilho(noVarLinha);
        }

        return noVar;
    }

    private noArvore parseVariavelLinha() {

        if (tokenAtual.getToken().equals("ABRECOL")) {
            noArvore noVarlinha = new noArvore("variável", "array");
            match("ABRECOL");

            noArvore noExp = parseExpressao();
            if (noExp != null) {
                noVarlinha.addFilho(noExp);
            }
            match("FECHACOL");
            return noVarlinha;
        } else if (tokenAtual.getToken().equals("ABREPAR")) {
            noArvore noVarLinha = new noArvore("variável", "procedimento");
            matchEAdiciona("ABREPAR", noVarLinha);

            noArvore lista = parseListaExpressoes();
            if (lista != null) {
                noVarLinha.addFilho(lista);
            }
            matchEAdiciona("FECHAPAR", noVarLinha);
            return noVarLinha;
        }

        return null; // EPSILON
    }

    private noArvore parseListaExpressoes() {

        noArvore noLista = new noArvore("lista expressões", "");
        noArvore exp = parseExpressao();
        if (exp != null) {
            noLista.addFilho(exp);
        }

        noArvore listaLinha = parseListaExpLinha();
        if (listaLinha != null) {
            noLista.addFilho(listaLinha);
        }

        return noLista;
    }

    private noArvore parseListaExpLinha() {

        if (tokenAtual.getToken().equals("VIRGULA")) {
            noArvore noLinha = new noArvore("lista expressões", ",");
            matchEAdiciona("VIRGULA", noLinha);

            noArvore exp = parseExpressao();
            if (exp != null) {
                noLinha.addFilho(exp);
            }

            noArvore proxLinha = parseListaExpLinha();
            if (proxLinha != null) {
                noLinha.addFilho(proxLinha);
            }

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
            return null;
        }
        switch (tokenAtual.getToken()) {
            case "IDENTIFICADOR" -> {
                noArvore noVar = parseVariavel();
                noFator.addFilho(noVar);
            }
            case "NUM" -> {
                noFator.addFilho(new noArvore("num", tokenAtual.getLexema()));
                match("NUM");
            }
            case "ABREPAR" -> {
                matchEAdiciona("ABREPAR", noFator);
                noArvore noExp = parseExpressao();
                if (noExp != null) {
                    noFator.addFilho(noExp);
                }
                matchEAdiciona("FECHAPAR", noFator);
            }
            case "NOT" -> {
                matchEAdiciona("NOT", noFator);
                noArvore fNot = parseFator();
                if (fNot != null) {
                    noFator.addFilho(fNot);
                }
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
