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
        2. Comandos de ControlE (IF e WHILE) -- if feito, while necessita de atenção -- feito
        3. Comandos de Entrada/Saída (READ e WRITE) -- feito
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

    private void parsePrograma() {

        noArvoreDTO raizArvore = new noArvoreDTO("programa", "");
        if (tokenAtual.getToken().equals("PROGRAM")) {
            raizArvore.addFilho(new noArvoreDTO("program", "program"));
            match("PROGRAM");

            String nomePrograma = tokenAtual.getLexema();
            noArvoreDTO noId = new  noArvoreDTO("IDENTIFICADOR", nomePrograma);
            raizArvore.addFilho(noId);
            match("IDENTIFICADOR");
            noId.addFilho(new noArvoreDTO("id", nomePrograma));
            raizArvore.addFilho(new noArvoreDTO("PONTOVIRGULA", ""));
            match("PONTOVIRGULA");

            noArvoreDTO noBloco = parseBloco();
            raizArvore.addFilho(noBloco);

            raizArvore.addFilho(new  noArvoreDTO("PONTO", "."));
            match("PONTO");

        }
    }

    // <bloco> ::= [<parte de declarção de variáveis>] [<parte de declaração de subrotinas>] <comando composto>
    private noArvoreDTO parseBloco() {
        noArvoreDTO noRaizBloco = new noArvoreDTO("bloco", "bloco");


        noArvoreDTO noComandoCompost = parseComandoComposto();
        noRaizBloco.addFilho(noComandoCompost);

        return noRaizBloco;
    }

    // <comando composto> ::= begin <comando> { ; <comando> } end
    private noArvoreDTO parseComandoComposto() {
        noArvoreDTO noRaizComandoComposto = new noArvoreDTO("comando composto", "comando composto");
        noRaizComandoComposto.addFilho(new noArvoreDTO("BEGIN", "BEGIN"));
        match("BEGIN");

        noArvoreDTO comandos = parseComando();
        noRaizComandoComposto.addFilho(comandos);

        noRaizComandoComposto.addFilho(new noArvoreDTO("END", "END"));
        match("END");

        return noRaizComandoComposto;
    }

    // <comando> ::= <identificador> <resto do comando>
    // | <comando composto>
    // | <comando condicional 1>
    // | <comando repetitivo 1>
    private noArvoreDTO parseComando() {
        noArvoreDTO noRaizComando = new noArvoreDTO("comando", "teste");

        switch(tokenAtual.getToken()) {
            case "IDENTIFICADOR":
                match("IDENTIFICADOR");
                if (tokenAtual.getToken().equals("ABREPAR")) {
                    match("ABREPAR");
                    match("FECHAPAR");
                    return new noArvoreDTO("chamada de função", "chamada de função");
                }
            case "READ":
                //return parseLeitura();
                break;
            case "WRITE":
                noRaizComando.addFilho(parseComandoEScrita());
                return noRaizComando;
            case "IF":
                noRaizComando.addFilho(parseComandoCondicional());
                return noRaizComando;
            case "WHILE":
                //return parseComandoRepetitivo();
                break;
            case "BEGIN":
                //return parseComandoComposto();
                break;
            default:
                return null;
        }
        return null;
    }

    private noArvoreDTO parseComandoCondicional() {
        noArvoreDTO raizComandoCondicional = new noArvoreDTO("comando condicional", "comando condicional");
        raizComandoCondicional.addFilho(new  noArvoreDTO("IF", "if"));
        match("IF");

        noArvoreDTO expressao = parseExpressao();
        raizComandoCondicional.addFilho(expressao);

        raizComandoCondicional.addFilho(new  noArvoreDTO("THEN", "then"));
        match("THEN");

        noArvoreDTO comando = parseComando();
        raizComandoCondicional.addFilho(comando);

        noArvoreDTO noElse = parseElse();
        raizComandoCondicional.addFilho(noElse);

        return raizComandoCondicional;
    }

    private noArvoreDTO parseElse() {

        if (tokenAtual.getToken().equals("ELSE")) {
            noArvoreDTO noElse = new noArvoreDTO("ELSE", "else");
            match("ELSE");
            noElse.addFilho(parseComando());
            return noElse;
        }
        return null;
    }

    private noArvoreDTO parseComandoRepetitivo() {
        return null;
    }

    private  noArvoreDTO parseComandoEScrita() {
        noArvoreDTO raizEScrita = new  noArvoreDTO("identificador", "identificador");
        raizEScrita.addFilho(new noArvoreDTO("escrita", "write"));
        match("WRITE");
        match("ABREPAR");
        match("FECHAPAR");
        match("PONTOVIRGULA");
        return raizEScrita;
    }

    private  noArvoreDTO parseExpressao() {
        return null;
    }

    private noArvoreDTO parseExpressaoSimples() {
        // op termo expressãosimples

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
        noArvoreDTO noEsquerda = parseTermo();
        while (tokenAtual.getToken().equals("OPSOMA") ||
                tokenAtual.getToken().equals("OPSUB") ||
                tokenAtual.getToken().equals("OPOR")) {

            String operador = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();
            match(tokenDoOperador); // consome +, - ou or

            noArvoreDTO noDireita = parseTermo();

            noArvoreDTO noPai = new noArvoreDTO("Expressão simples", operador);

            // "pendura" a matemática na ordem exata: esquerda, meio (-, + ou or), direita
            noPai.addFilho(noEsquerda);

            noArvoreDTO terminalOperador = new noArvoreDTO("Operador", operador);
            noPai.addFilho(terminalOperador);

            noPai.addFilho(noDireita);

            noEsquerda = noPai;
        }

        return noEsquerda;
    }

    private noArvoreDTO parseTermo() {
        // fator termo
        noArvoreDTO noEsquerda = parseFator();

        while (tokenAtual.getToken().equals("OPMUL") ||
                tokenAtual.getToken().equals("OPDIV") ||
                tokenAtual.getToken().equals("OPAND")) {

            String operador = tokenAtual.getLexema();
            String tokenDoOperador = tokenAtual.getToken();
            match(tokenDoOperador); // consome *, / ou and

            noArvoreDTO noDireita = parseFator();

            noArvoreDTO noPai = new noArvoreDTO("Termo", operador);
            noPai.addFilho(noEsquerda);
            noPai.addFilho(noDireita);

            noEsquerda = noPai;
        }


        return noEsquerda;
    }

    private noArvoreDTO parseFator() {
        // variavel | numero | ( expressão ) | not fator
        switch (tokenAtual.getToken()) {
            case "IDENTIFICADOR" -> {
                String nomeIdentificador = tokenAtual.getLexema();
                match("IDENTIFICADOR");

                if (tokenAtual.getToken().equals("ABRECOLCHETE")) {
                    match("ABRECOLCHETE");
                    noArvoreDTO indiceVetor = parseExpressaoSimples();
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

                noArvoreDTO noExpressaoInterna = parseExpressaoSimples();
                match("FECHAPAR");
                return noExpressaoInterna;
            }
            case "OPNOT" -> {
                match("OPNOT");

                noArvoreDTO noFatorNEgado = parseFator();
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
