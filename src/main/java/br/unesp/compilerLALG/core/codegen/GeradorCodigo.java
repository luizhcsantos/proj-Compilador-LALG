package br.unesp.compilerLALG.core.codegen;

import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.core.semantic.Simbolo;
import br.unesp.compilerLALG.core.semantic.TabelaSimbolos;

import java.util.ArrayList;
import java.util.List;

public class GeradorCodigo {

    private final List<String> codigoMepa;
    private final TabelaSimbolos tabelaSimbolos; // Precisamos da tabela para saber os endereços
    private int ponteiroMemoriaLivre; // Controla qual é a próxima gaveta livre na MEPA
    private int contadorRotulos; // Para os IFs e WHILEs (L1, L2, etc)

    public GeradorCodigo(TabelaSimbolos tabelaSimbolos) {
        this.codigoMepa = new ArrayList<>();
        this.tabelaSimbolos = tabelaSimbolos;
        this.ponteiroMemoriaLivre = 0;
        this.contadorRotulos = 1;
    }

    public String getCodigoGerado() {
        return String.join("\n", codigoMepa);
    }

    public void gerar(noArvore raiz) {
        codigoMepa.add("INPP"); // Inicia o Programa Principal
        visitar(raiz);
        codigoMepa.add("PARA"); // Fim da execução
    }

    private void visitar(noArvore no) {
        if (no == null) return;
        String simbolo = no.getSimbolo();

        // declaração de variaveis (AMEM)
        if (simbolo.equals("<declaração_de_variáveis>")) {
            List<String> variaveis = extrairIdentificadores(no);
            int quantidade = variaveis.size();
            if (quantidade > 0) {
                codigoMepa.add("AMEM " + quantidade);
                for (String nomeVar : variaveis) {
                    Simbolo sim = tabelaSimbolos.buscar(nomeVar);
                    if (sim != null && sim.getEnderecoRelativo() == -1) {
                        sim.setEnderecoRelativo(ponteiroMemoriaLivre);
                        ponteiroMemoriaLivre++;
                    }
                }
            }
            return;
        }

        // atribuição de variáveis (ARMZ)
        else if (simbolo.equals("<comando>")) {
            if (no.getFilhos().isEmpty()) return;

            String lexemaCmd = extrairLexemaPorSimbolo(no.getFilhos().get(0), "identificador").toLowerCase();

            // verifica se tem := do lado direito
            noArvore noComandoLinha = (no.getFilhos().size() > 1) ? no.getFilhos().get(1) : null;
            noArvore noAtribuicao = (noComandoLinha != null) ? buscarFilho(noComandoLinha, ":=") : null;

            if (noAtribuicao != null) {
                Simbolo sim = tabelaSimbolos.buscar(lexemaCmd);
                noArvore expressao = buscarFilho(noComandoLinha, "<expressão>");

                visitar(expressao); // executa toda a matemática primeiro

                // depois de a conta estar no topo da pilha, guarda na memória
                if (sim != null) {
                    codigoMepa.add("ARMZ " + sim.getEnderecoRelativo());
                } else {
                    System.out.println("ERRO INTERNO: Tentou gerar ARMZ para variável não encontrada: " + lexemaCmd);
                }
                return;
            }
            else if (lexemaCmd.equals("read")) {
                codigoMepa.add("LEIT");
                noArvore listaExp = buscarFilho(no, "<lista_de_expressões>");
                if (listaExp != null) {
                    String nomeVar = extrairLexemaPorSimbolo(listaExp, "identificador");
                    Simbolo sim = tabelaSimbolos.buscar(nomeVar);
                    if (sim != null) codigoMepa.add("ARMZ " + sim.getEnderecoRelativo());
                }
                return;
            }
            else if (lexemaCmd.equals("write")) {
                noArvore listaExp = buscarFilho(no, "<lista_de_expressões>");
                if (listaExp != null) {
                    visitar(listaExp);
                    codigoMepa.add("IMPR");
                }
                return;
            }
        }


        // comando condicional - if then else
        else if (simbolo.equals("<comando_condicional_1>")) {
            String rotuloElse = criarRotulo();
            String rotuloFim = criarRotulo();

            noArvore expressao = buscarFilho(no, "<expressão>");
            noArvore comandoThen = buscarFilho(no, "<comando>");
            noArvore comandoCondicional2 = buscarFilho(no, "<comando_condicional_2>");

            // avalia a condição (o resultado fica no topo da pilha)
            visitar(expressao);

            // se for falso (0), desvia para o bloco ELSE (ou para o FIM se não houver else)
            codigoMepa.add("DSVF " + rotuloElse);

            // executa o bloco THEN
            visitar(comandoThen);
            codigoMepa.add("DSVS " + rotuloFim); // Terminou o THEN, salta o ELSE

            // executa o bloco ELSE (Se existir)
            codigoMepa.add(rotuloElse + ": NADA");
            if (comandoCondicional2 != null && !comandoCondicional2.getFilhos().get(0).getSimbolo().equals("EPSILON")) {
                noArvore comandoElse = buscarFilho(comandoCondicional2, "<comando>");
                visitar(comandoElse);
            }

            // marca o fim da instrução IF
            codigoMepa.add(rotuloFim + ": NADA");
            return;
        }

        // comando repetitivo - while do
        else if (simbolo.equals("<comando_repetitivo_1>")) {
            String rotuloInicio = criarRotulo();
            String rotuloFim = criarRotulo();

            // marca o início do laço de repetição
            codigoMepa.add(rotuloInicio + ": NADA");

            // avalia a condição do laço
            noArvore expressao = buscarFilho(no, "<expressão>");
            visitar(expressao);

            // se a condição for falsa, sai do laço
            codigoMepa.add("DSVF " + rotuloFim);

            // executa o interior do WHILE
            noArvore comandoBloco = buscarFilho(no, "<comando>");
            visitar(comandoBloco);

            // volta incondicionalmente para o início para testar a condição de novo
            codigoMepa.add("DSVS " + rotuloInicio);

            // marca o fim do laço
            codigoMepa.add(rotuloFim + ": NADA");
            return;
        }

        // read e write
        else if (simbolo.equals("<chamada_de_procedimento>") ||
                (simbolo.equals("<comando>") && !temAtribuicao(no))) {

            if (no.getFilhos().isEmpty()) return;

            String nomeProc = extrairLexemaPorSimbolo(no.getFilhos().get(0), "identificador").toLowerCase();

            if (nomeProc.equals("read")) {
                codigoMepa.add("LEIT");
                noArvore listaExp = buscarFilho(no, "<lista_de_expressões>");
                if (listaExp != null) {
                    String nomeVar = extrairLexemaPorSimbolo(listaExp, "identificador");
                    Simbolo sim = tabelaSimbolos.buscar(nomeVar);
                    if (sim != null) codigoMepa.add("ARMZ " + sim.getEnderecoRelativo());
                }
                return;
            }
            else if (nomeProc.equals("write")) {
                noArvore listaExp = buscarFilho(no, "<lista_de_expressões>");
                if (listaExp != null) {
                    visitar(listaExp); // joga o resultado da conta na pilha
                    codigoMepa.add("IMPR"); // imprime o topo da pilha
                }
                return;
            }
        }

        // expressões e termos
        else if (simbolo.equals("numero")) {
            codigoMepa.add("CRCT " + no.getLexema()); // carrrega constante no topo da pilha
            return;
        }
        else if (simbolo.equals("identificador")) {
            // descobre se é true/false ou variável
            String lexema = no.getLexema().toLowerCase();
            if (lexema.equals("true")) {
                codigoMepa.add("CRCT 1");
            } else if (lexema.equals("false")) {
                codigoMepa.add("CRCT 0");
            } else {
                Simbolo sim = tabelaSimbolos.buscar(lexema);
                if (sim != null) {
                    codigoMepa.add("CRVL " + sim.getEnderecoRelativo()); // carrega valor da memória
                }
            }
            return;
        }

        else if (simbolo.endsWith("'>") || simbolo.endsWith("_linha>")) {
            if (no.getFilhos().size() >= 2) {
                noArvore opNode = no.getFilhos().get(0);
                noArvore rightNode = no.getFilhos().get(1);

                String op = extrairLexemaOperador(opNode);
                if (!op.isEmpty()) {
                    visitar(rightNode); // Avalia o lado direito da conta
                    gerarInstrucaoOperador(op); // Executa a conta

                    // Se houver mais continuação (ex: + c + d), continua varrendo
                    if (no.getFilhos().size() > 2) {
                        visitar(no.getFilhos().get(2));
                    }
                    return;
                }
            }
        }

        // Se for um nó de operação (+, -, *, /)
        if (ehOperador(simbolo)) {
            // em pós-ordem, os filhos já foram visitados e estão na pilha
            gerarInstrucaoOperador(simbolo);
            return;
        }

        // continua a descer na árvore para procurar mais comandos e nós soltos
        for (noArvore filho : no.getFilhos()) {
            visitar(filho);
        }
    }

    private void gerarInstrucaoOperador(String op) {
        switch (op) {
            case "+": codigoMepa.add("SOMA"); break;
            case "-": codigoMepa.add("SUBT"); break;
            case "*": codigoMepa.add("MULT"); break;
            case "div":
            case "/": codigoMepa.add("DIVI"); break;
            case ">": codigoMepa.add("CMAI"); break;
            case "<": codigoMepa.add("CMEN"); break;
            case "=": codigoMepa.add("CMIG"); break;
            case ">=": codigoMepa.add("CMAQ"); break;
            case "<=": codigoMepa.add("CMEQ"); break;
            case "<>": codigoMepa.add("CDIF"); break;
            case "and": codigoMepa.add("CONJ"); break;
            case "or": codigoMepa.add("DISJ"); break;
        }
    }

    private boolean ehOperador(String sim) {
        return sim.equals("+") || sim.equals("-") || sim.equals("*") || sim.equals("/") ||
                sim.equals(">") || sim.equals("<") || sim.equals("=");
    }

    private String extrairLexemaOperador(noArvore no) {
        if (no == null) return "";
        String sim = no.getSimbolo();
        if (ehOperador(sim)) return sim;
        if (no.getLexema() != null && ehOperador(no.getLexema())) return no.getLexema();
        if (!no.getFilhos().isEmpty()) return extrairLexemaOperador(no.getFilhos().get(0));
        return "";
    }

    private List<String> extrairIdentificadores(noArvore no) {
        List<String> ids = new ArrayList<>();
        extrairIdsRec(no, ids);
        return ids;
    }
    private void extrairIdsRec(noArvore no, List<String> ids) {
        if (no.getSimbolo().equals("identificador") && no.getLexema() != null) ids.add(no.getLexema());
        for (noArvore f : no.getFilhos()) extrairIdsRec(f, ids);
    }

    private boolean temAtribuicao(noArvore no) {
        if (no.getSimbolo().equals(":=")) return true;
        for (noArvore f : no.getFilhos()) if (temAtribuicao(f)) return true;
        return false;
    }

    private noArvore buscarExpressao(noArvore pai) {
        if (pai.getSimbolo().equals("<expressão>")) return pai;
        for (noArvore f : pai.getFilhos()) {
            noArvore achado = buscarExpressao(f);
            if (achado != null) return achado;
        }
        return null;
    }

    private String criarRotulo() {
        return "L" + (contadorRotulos++);
    }

    private noArvore buscarFilho(noArvore pai, String simboloBuscado) {
        if (pai == null) return null;

        if (pai.getSimbolo().equals(simboloBuscado)) {
            return pai;
        }

        for (noArvore filho : pai.getFilhos()) {
            noArvore achado = buscarFilho(filho, simboloBuscado);
            if (achado != null) {
                return achado;
            }
        }

        return null;
    }

    private String extrairLexemaPorSimbolo(noArvore pai, String simboloAlvo) {
        if (pai == null) return "";

        // verifica se é o símbolo procurado e se tem um lexema guardado nele
        if (pai.getSimbolo().equals(simboloAlvo) && pai.getLexema() != null && !pai.getLexema().isEmpty()) {
            return pai.getLexema();
        }

        // procura recursivamente nos filhos
        for (noArvore filho : pai.getFilhos()) {
            String resultado = extrairLexemaPorSimbolo(filho, simboloAlvo);
            // assim que encontra o texto num dos filhos, interrompe a busca e devolve
            if (!resultado.isEmpty()) {
                return resultado;
            }
        }

        return ""; // Retorna vazio se não encontrar
    }

    // Verifica se o nó começa com uma variável
    private boolean comecaComIdentificador(noArvore no) {
        if (no == null || no.getFilhos().isEmpty()) return false;
        String sim = no.getFilhos().get(0).getSimbolo();
        return sim.equals("identificador") || sim.equals("<identificador>");
    }
}