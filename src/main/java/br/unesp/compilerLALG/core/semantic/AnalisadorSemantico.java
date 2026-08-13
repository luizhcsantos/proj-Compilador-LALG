package br.unesp.compilerLALG.core.semantic;

import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.exception.CompilerException;

import java.util.ArrayList;
import java.util.List;

public class AnalisadorSemantico {

    private final TabelaSimbolos tabelaSimbolos;
    private final List<CompilerException.SemanticException> errosSemanticos;

    public AnalisadorSemantico() {
        this.tabelaSimbolos = new TabelaSimbolos();
        this.errosSemanticos = new ArrayList<>();
    }

    public List<CompilerException.SemanticException> getErrosSemanticos() {
        return errosSemanticos;
    }

    public void analisar(noArvore raiz) {
        errosSemanticos.clear();
        // Inicia a travessia na raiz da AST
        //System.out.println("DEBUG: Iniciando análise semântica na raiz da AST: " + raiz.getSimbolo());
        visitar(raiz);
    }

    /**
     * O Visitante Recursivo que varre a Árvore Sintática Abstrata
     */
    private void visitar(noArvore no) {
        if (no == null) return;

        String simbolo = no.getSimbolo();

        // Processar Declaração de Variáveis e capturar os tipos
        if (simbolo.equals("<declaração_de_variáveis>")) {
            processarDeclaracaoVariaveis(no);
            return; // Já processámos os filhos aqui dentro
        }

        // Processar Procedures (Entra no escopo, visita, sai do escopo)
        else if (simbolo.equals("<declaração_de_procedimento>")) {
            processarProcedure(no);
            return; // Bloqueia a travessia padrão para gerir o escopo manualmente
        }

        // Processar Atribuições (Checagem de Tipos)
        else if (simbolo.equals("<comando>")) {
            processarComando(no);
            return;
        }

        // Processar IF e WHILE (Exigência de expressão Booleana)
        else if (simbolo.equals("<comando_condicional_1>") || simbolo.equals("<comando_repetitivo_1>")) {
            processarComandoLogico(no);
        }

        // Processar Chamada de Procedure (Verifica se existe e parâmetros)
        else if (simbolo.equals("<chamada_de_procedimento>")) {
            processarChamadaProcedimento(no);
            return;
        }

        // Continua a travessia genérica para todos os filhos
        for (noArvore filho : no.getFilhos()) {
            visitar(filho);
        }
    }


    private void processarDeclaracaoVariaveis(noArvore noDeclaracao) {
        // Encontra o tipo (filho 0)
        noArvore noTipo = buscarFilho(noDeclaracao, "<tipo>");
        String tipoVariavel = extrairLexemaFolha(noTipo); // "int" ou "boolean"

        // Encontra a lista de IDs e regista cada um na tabela
        noArvore noListaIds = buscarFilho(noDeclaracao, "<lista_de_identificadores>");
        List<String> nomesVars = extrairTodosIdentificadores(noListaIds);

        for (String nome : nomesVars) {
            boolean sucesso = tabelaSimbolos.inserir(nome, tipoVariavel, "var");
            if (!sucesso) {
                errosSemanticos.add(new CompilerException.SemanticException("Variável '" + nome + "' já foi declarada neste âmbito."));
            }
        }
    }

    private void processarProcedure(noArvore noProc) {
        // Regista o nome da procedure no escopo atual
        String nomeProc = extrairLexemaPorSimbolo(noProc, "identificador");
        tabelaSimbolos.inserir(nomeProc, "procedure", "proc");

        // Cria o novo escopo LOCAL
        tabelaSimbolos.entrarEscopo();

        // Regista os parâmetros formais (se existirem) como variáveis locais
        noArvore params = buscarFilho(noProc, "<parâmetros_formais>");
        if (params != null) {
            // (Lógica semelhante a extrair variáveis, mas com categoria "param")

        }

        // Visita o bloco da procedure
        noArvore bloco = buscarFilho(noProc, "<bloco>");
        if (bloco != null) visitar(bloco);

        // Destrói o escopo local ao sair da procedure
        tabelaSimbolos.sairEscopo();
    }

    private void processarComando(noArvore noComando) {

        if (noComando.getFilhos().isEmpty()) return;

        // O primeiro filho do <comando> é o identificador (ex: "soma")
        String nomeVar = extrairLexemaPorSimbolo(noComando.getFilhos().get(0), "identificador");

        // Procura se dentro do <comando'> existe o operador de atribuição :=
        noArvore noComandoLinha = (noComando.getFilhos().size() > 1) ? noComando.getFilhos().get(1) : null;
        noArvore noAtribuicao = (noComandoLinha != null) ? buscarFilho(noComandoLinha, ":=") : null;

        if (noAtribuicao != null) {
            // é atribuição
            Simbolo sim = tabelaSimbolos.buscar(nomeVar);
            if (sim == null) {
                errosSemanticos.add(new CompilerException.IdentificadorNaoDeclaradoException(nomeVar));
                return;
            }

            // Procura a expressão do lado direito da atribuição
            noArvore expressao = buscarFilho(noComandoLinha, "<expressão>");
            String tipoExpressao = inferirTipoExpressao(expressao);

            // Checagem Semântica: Compara os tipos
            if (!sim.getTipo().equalsIgnoreCase(tipoExpressao) && !tipoExpressao.equals("desconhecido")) {
                errosSemanticos.add(new CompilerException.TiposIncompativeisException(nomeVar, sim.getTipo(), tipoExpressao));

            }
        } else {
            // é chamada de procdimento
            processarChamadaProcedimento(noComando);
        }
    }

    private void processarAtribuicao(noArvore noAtribuicao) {
        // O lado esquerdo é a variável
        String nomeVar = extrairLexemaPorSimbolo(noAtribuicao, "identificador");

        Simbolo sim = tabelaSimbolos.buscar(nomeVar);
        if (sim == null) {
            errosSemanticos.add(new CompilerException.SemanticException("Erro Semântico: Variável '" + nomeVar + "' não declarada."));
            return;
        }

        // O lado direito é a expressão
        noArvore expressao = buscarFilho(noAtribuicao, "<expressão>");
        String tipoExpressao = inferirTipoExpressao(expressao);

        // Checagem Clássica: Os tipos casam?
        if (!sim.getTipo().equals(tipoExpressao) && !tipoExpressao.equals("desconhecido")) {
            errosSemanticos.add(new CompilerException.SemanticException("Erro Semântico: Incompatibilidade de tipos na atribuição de '" + nomeVar +
                    "'. Esperado: " + sim.getTipo() + ", mas recebeu: " + tipoExpressao));
        }
    }

    private void processarComandoLogico(noArvore noComando) {
        // Extrai a condição (que é uma <expressão>)
        noArvore expressao = buscarFilho(noComando, "<expressão>");
        String tipoExp = inferirTipoExpressao(expressao);

        // A regra de Ouro: IF e WHILE exigem uma condição Booleana
        if (!tipoExp.equals("boolean") && !tipoExp.equals("desconhecido")) {
            String operacao = noComando.getSimbolo().contains("condicional") ? "IF" : "WHILE";
            errosSemanticos.add(new CompilerException.SemanticException(
                    "Erro Semântico: A condição do " + operacao + " " +
                            "deve ser booleana. Encontrado: " + tipoExp));
        }
    }

    private void processarChamadaProcedimento(noArvore noChamada) {
//        String nomeProc = extrairLexemaPorSimbolo(noChamada, "identificador");
//
//        // Procedimentos nativos do LALG são ignorados na checagem
//        if (nomeProc.equals("read") || nomeProc.equals("write")) return;
//
//        Simbolo sim = tabelaSimbolos.buscar(nomeProc);
//        if (sim == null) {
//            errosSemanticos.add(new CompilerException.SemanticException("Erro Semântico: Procedimento '" + nomeProc + "' não declarado."));
//        } else if (!sim.getCategoria().equals("proc")) {
//            errosSemanticos.add(new CompilerException.SemanticException("Erro Semântico: O identificador '" + nomeProc + "' " +
//                    "não é um procedimento."));
//        }
        if (noChamada.getFilhos().isEmpty()) return;

        // MÁGICA: Olha diretamente para o 1º filho em vez de fazer busca profunda!
        noArvore primeiroFilho = noChamada.getFilhos().get(0);
        String nomeProc = primeiroFilho.getLexema() != null && !primeiroFilho.getLexema().isEmpty()
                ? primeiroFilho.getLexema().toLowerCase()
                : primeiroFilho.getSimbolo().toLowerCase();

        // 1. É um READ ou WRITE nativo?
        if (nomeProc.equals("read") || nomeProc.equals("write")) {
            // Garante que a variável/expressão lá dentro foi declarada!
            noArvore listaExp = buscarFilho(noChamada, "<lista_de_expressões>");
            if (listaExp != null) inferirTipoExpressao(listaExp);
            return; // Interrompe aqui, pois read/write não estão na tabela de símbolos
        }

        // 2. É uma procedure do programador
        Simbolo sim = tabelaSimbolos.buscar(nomeProc);
        if (sim == null) {
            errosSemanticos.add(new CompilerException.IdentificadorNaoDeclaradoException(nomeProc));
        } else if (!sim.getCategoria().equals("proc")) {
            errosSemanticos.add(new CompilerException.NaoEProcedimentoException(nomeProc));
        }
    }


    private String inferirTipoExpressao(noArvore no) {
        if (no == null) return "desconhecido";

        // Se for um folha de terminal:
        if (no.getSimbolo().equals("numero")) return "int";
        if (no.getSimbolo().equals("identificador")) {

            String lexema = no.getLexema().toLowerCase();

            // Constantes literais booleanas
            if (lexema.equals("true") || lexema.equals("false")) {
                return "boolean";
            }

            Simbolo s = tabelaSimbolos.buscar(lexema);

            if (s == null) {
                errosSemanticos.add(new CompilerException.IdentificadorNaoDeclaradoException(lexema));
            }

            return (s != null) ? s.getTipo() : "desconhecido";
        }

        // Se contiver operadores relacionais (=, <, >), o resultado FINAL da conta é Booleano!
        if (contemOperadorRelacional(no)) {
            return "boolean";
        }

        // Procura nos filhos recursivamente para saber a base da conta
        String tipoDominante = "int"; // Assumimos int até prova em contrário (para contas)

//        for (noArvore filho : no.getFilhos()) {
//            if (filho.getSimbolo().contains("<expressão") ||
//                    filho.getSimbolo().contains("<termo") ||
//                    filho.getSimbolo().contains("<fator")) {
//
//                String tipoFilho = inferirTipoExpressao(filho);
//                if (tipoFilho.equals("boolean")) {
//                    tipoDominante = "boolean"; // Se envolver 'or', 'and', ou 'not'
//                }
//            }
//        }
//        return tipoDominante;
        for (noArvore filho : no.getFilhos()) {
            String t = inferirTipoExpressao(filho);
            if (t.equals("boolean")) {
                tipoDominante = "boolean";
            }
        }
        return tipoDominante;
    }


    private noArvore buscarFilho(noArvore pai, String simboloBuscado) {
        if (pai.getSimbolo().equals(simboloBuscado)) return pai;
        for (noArvore filho : pai.getFilhos()) {
            noArvore achado = buscarFilho(filho, simboloBuscado);
            if (achado != null) return achado;
        }
        return null;
    }

    private String extrairLexemaFolha(noArvore no) {
        if (no.getLexema() != null && !no.getLexema().isEmpty()) return no.getLexema();
        for (noArvore filho : no.getFilhos()) {
            String lex = extrairLexemaFolha(filho);
            if (!lex.isEmpty()) return lex;
        }
        return "";
    }

    private String extrairLexemaPorSimbolo(noArvore pai, String simboloAlvo) {
        if (pai.getSimbolo().equals(simboloAlvo) && pai.getLexema() != null) return pai.getLexema();
        for (noArvore filho : pai.getFilhos()) {
            String res = extrairLexemaPorSimbolo(filho, simboloAlvo);
            if (!res.isEmpty()) return res;
        }
        return "";
    }

    private List<String> extrairTodosIdentificadores(noArvore no) {
        List<String> ids = new ArrayList<>();
        extrairTodosIdentificadoresRec(no, ids);
        return ids;
    }

    private void extrairTodosIdentificadoresRec(noArvore no, List<String> ids) {
        if (no.getSimbolo().equals("identificador") && no.getLexema() != null && !no.getLexema().isEmpty()) {
            ids.add(no.getLexema());
        }
        for (noArvore filho : no.getFilhos()) {
            extrairTodosIdentificadoresRec(filho, ids);
        }
    }

    private boolean contemOperadorRelacional(noArvore no) {
        String s = no.getSimbolo();
        if (s.equals("<relação>") || s.equals("=") || s.equals("<>") || s.equals(">") || s.equals("<")) return true;
        for (noArvore f : no.getFilhos()) {
            if (contemOperadorRelacional(f)) return true;
        }
        return false;
    }

    public boolean temErros() {
        System.out.println("DEBUG: Análise semântica encontrou " + errosSemanticos.size() + " erros.");
        return !errosSemanticos.isEmpty();
    }

    public List<CompilerException.SemanticException> getErros() {
        return errosSemanticos;
    }

    public TabelaSimbolos getTabelaSimbolos() {
        return tabelaSimbolos;
    }
}