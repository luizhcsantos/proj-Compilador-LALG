package br.unesp.compilerLALG.core.parser;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.ASTnode;
import br.unesp.compilerLALG.core.parser.ast.BinOpNode;
import br.unesp.compilerLALG.core.parser.ast.NumNode;
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

    // Lista para guardar os erros sintáticos (Panic Mode)
    private final List<CompilerException.SyntaxException> listaErrosSintaticos = new ArrayList<>();

    // Conjuntos First (Para escolher qual caminho seguir na EBNF)
    // FIRST(<bloco>) -> pode começar um bloco de código
    private final Set<String> FIRST_BLOCO = Set.of(
            "INT", "BOOLEAN", "IDENTIFICADOR", "PROCEDURE", "BEGIN"
    );

    // FIRST(<parte de declarações de variáveis>) -> inicia a declaração de variáveis
    private final Set<String> FIRST_DECL_VAR = Set.of(
            "INT", "BOOLEAN", "IDENTIFICADOR"
    );

    // FIRST(<comando>) -> Todos os tokens que podem iniciar um comando válido
    private final Set<String> FIRST_COMANDO = Set.of(
            "IDENTIFICADOR", "READ", "WRITE", "BEGIN", "IF", "WHILE"
    );

    // FIRST(<expressão>) -> pode começar uma conta matemática ou lógica
    private final Set<String> FIRST_EXPRESSAO = Set.of(
            "OPSOMA", "OPSUB", "IDENTIFICADOR", "NUM", "ABREPAR", "OPNOT", "TRUE", "FALSE"
    );

    // FIRST(<fator>) -> elementos mais básicos de uma expressão
    private final Set<String> FIRST_FATOR = Set.of(
            "IDENTIFICADOR", "NUM", "ABREPAR", "OPNOT", "TRUE", "FALSE"
    );

    // Conjuntos Follow (Para sair de loops e tratar erros - Panic Mode)

    // FOLLOW(<bloco>) -> vem logo após um bloco terminar
    private final Set<String> FOLLOW_BLOCO = Set.of(
            "PONTO", "PONTOVIRGULA"
    );

    // FOLLOW(<parte de declarações de variáveis>) -> vem depois das variáveis
    private final Set<String> FOLLOW_DECL_VAR = Set.of(
            "PROCEDURE", "BEGIN"
    );

    // FOLLOW(<declaração de procedimento>) -> vem após declarar uma procedure
    private final Set<String> FOLLOW_DECL_PROC = Set.of(
            "PONTOVIRGULA", "BEGIN"
    );

    // FOLLOW(<comando>) -> pode aparecer LOGO APÓS um comando terminar
    private final Set<String> FOLLOW_COMANDO = Set.of(
            "PONTOVIRGULA", "END", "ELSE"
    );

    // FOLLOW(<expressão>) -> sinais que indicam que a expressão (conta) acabou
    private final Set<String> FOLLOW_EXPRESSAO = Set.of(
            "PONTOVIRGULA", "END", "ELSE", "THEN", "DO", "FECHAPAR", "VIRGULA",
            "OPIGUAL", "OPDIF", "OPMENOR", "OPMENORIGUAL", "OPMAIOR", "OPMAIORIGUAL"
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
            //System.out.println("Análise sintática concluída com sucesso!");

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

    private void sincronizar(@NonNull Set<String> followSet) {
        while (!followSet.contains(tokenAtual.getToken()) && !tokenAtual.getToken().equals("EOF")) {
            avancar();
        }
    }

    // Programa ::= PROGRAM <identificador> ; <bloco> .
    public void parsePrograma() {
        match("PROGRAM");
        match("IDENTIFICADOR");
        match("PONTOVIRGULA");

        parseBloco();

        match("PONTO");
    }

    private void parseBloco() {

        if (tokenAtual.getToken().equals("INT") || tokenAtual.getToken().equals("BOOLEAN")) {
            parseDeclaracoesVariaveis();
        }

        if (tokenAtual.getToken().equals("PROCEDURE")) {
            parseDeclaracoesProcedimentos();
        }

        parseComandoComposto();
    }

    private void parseDeclaracoesProcedimentos() {

    }

    private void parseParteDeclaracoesVariaveis() {

        do {
            parseDeclaracoesVariaveis();
            match("PONTOVIRGULA");
        } while (tokenAtual.getToken().equals("INT") || tokenAtual.getToken().equals("BOOLEAN"));
    }

    private void parseDeclaracoesVariaveis() {

        if (tokenAtual.getToken().equals("INT")) {
            match("INT");
        } else if (tokenAtual.getToken().equals("BOOLEAN")) {
            match("BOOLEAN");
        } else {
            listaErrosSintaticos.add(new CompilerException.TokenInesperadoException(
                    "null",
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
        match("IDENTIFICADOR");

        while (tokenAtual.getToken().equals("VIRGULA")) {
            match("VIRGULA");
            match("IDENTIFICADOR");
        }
    }

    // <comando> ::= <comando_atribuicao> | <comando_leitura> | <comando_escrita>
    public void parseComando() {

        switch (tokenAtual.getToken()) {
            case "READ" -> parseComandoLeitura();

            case "WRITE" -> parseComandoEscrita();

            // Dentro do switch do seu parseComando():
            case "IDENTIFICADOR" -> {
                match("IDENTIFICADOR");

                // Fatoração: Olha para o PRÓXIMO token para decidir
                if (tokenAtual.getToken().equals("ATRIBUICAO")) { // :=
                    parseComandoAtribuicao();
                } else if (tokenAtual.getToken().equals("ABREPAR")) { // (
                    // É uma chamada de procedimento com parâmetros
                    match("ABREPAR");
                    // parseListaExpressoes();
                    match("FECHAPAR");
                } else {
                    // Chamada de procedimento sem parâmetros (ex: limpar_tela;)
                    // Não faz nada, já consumiu o identificador.
                }
            }

            case "BEGIN" -> parseComandoComposto();

            case "IF" -> parseComandoIf();

            case "ELSE" -> {

            }
            case "WHILE" -> parseComandoWhile();

            default -> {
                listaErrosSintaticos.add(new CompilerException.ComandoInvalidoException(
                        tokenAtual.getLexema(),
                        tokenAtual.getLinha(),
                        tokenAtual.getColunaInicial()
                ));

                // Dispara o Panic-Mode para tentar salvar o resto da compilação
                sincronizar(FOLLOW_COMANDO);
            }
        }


    }

    private void parseComandoComposto() {

    }

    private void parseComandoWhile() {

    }

    private void parseComandoIf() {


    }

    private void parseComandoAtribuicao() {

    }

    private void parseComandoEscrita() {

    }

    private void parseComandoLeitura() {

    }


    // <lista_comandos> ::= <comando> { ; <comando> }
    public void parseListaComandos() {
        try {
            parseComando();

            while (tokenAtual.getToken().equals("PONTOVIRGULA")) {
                match("PONTOVIRGULA");
                parseComando();
            }
        } catch (RuntimeException e) {
            // anota o erro (no futuro, pode adicionar numa lista de erros sintáticos)
            System.err.println(e.getMessage());

            // aciona o Panic Mode para pular até o fim do comando problemático
            sincronizar(FOLLOW_COMANDO);
        }
    }


    public ASTnode parse() {
        return expressao();
    }


    public ASTnode fator() {
        Token tokenAtual = tokens.get(pos);
        switch (tokenAtual.getToken()) {
            case "EOF" -> throw new RuntimeException("Expressão incompleta. Faltou um número.");
            case "NUM" -> {
                pos++; // "come" o número

                // Verifica se o Lexer mandou um texto vazio antes de converter
                String textoDoNumero = tokenAtual.getLexema();
                if (textoDoNumero == null || textoDoNumero.trim().isEmpty()) {
                    throw new RuntimeException("Erro interno no Lexer: Um número vazio foi gerado.");
                }

                try {
                    return new NumNode(Double.parseDouble(textoDoNumero));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Erro ao converter para número: '" + textoDoNumero + "'");
                }
            }
            case "AP" -> {
                pos++; // "come" o parêntese de abertura


                // resolve tudo que está dentro do oarenteses
                ASTnode expressaoInterna = expressao();

                // verifica se fehcou o parêntese corretamente
                if (pos < tokens.size() && tokens.get(pos).getToken().equals("FP")) {
                    pos++; // "come" o parêntese de fechamento
                    return expressaoInterna;
                } else {
                    throw new RuntimeException("Erro de Sintaxe: Esperado ')' na coluna " + tokenAtual.getColunaFinal());
                }
            }
        }
        throw new RuntimeException("Erro de Sintaxe: Token inesperado '" + tokenAtual.getLexema());
    }

    // resolve multiplicação e divisão
    public ASTnode termo() {
        ASTnode noEsquerda = fator();

        while (pos < tokens.size() && (tokens.get(pos).getToken().equals("OPMUL") || tokens.get(pos).getToken().equals("OPDIV"))) {

            String operador = tokens.get(pos).getLexema();
            pos++;

            ASTnode noDireita = fator();

            noEsquerda = new BinOpNode(noEsquerda, operador, noDireita);

        }
        // Se entrou no while, devolve a árvore de multiplicação.
        // Se não entrou, devolve o número puro intacto.
        return noEsquerda;
    }

    // resolve adição e subtração
    public ASTnode expressao() {
        ASTnode noEsquerda = termo();

        while (pos < tokens.size() && (tokens.get(pos).getToken().equals("OPSOMA") || tokens.get(pos).getToken().equals("OPSUB"))) {

            String operador = tokens.get(pos).getLexema();
            pos++;

            ASTnode noDireita = termo();

            noEsquerda = new BinOpNode(noEsquerda, operador, noDireita);
        }
        return noEsquerda;
    }

    public List<CompilerException.SyntaxException> getErros() {
        return listaErrosSintaticos;
    }

    public boolean temErros() {
        return !listaErrosSintaticos.isEmpty();
    }
}
