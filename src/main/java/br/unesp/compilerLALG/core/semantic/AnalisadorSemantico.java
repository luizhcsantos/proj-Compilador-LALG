package br.unesp.compilerLALG.core.semantic;

import br.unesp.compilerLALG.core.parser.ast.noArvore;

import java.util.ArrayList;
import java.util.List;

public class AnalisadorSemantico {

    private final TabelaSimbolos  tabelaSimbolos;
    private final List<String> errosSemanticos;
    private final List<String> warnings;

    public AnalisadorSemantico() {
        this.tabelaSimbolos = new TabelaSimbolos();
        this.errosSemanticos = new ArrayList<String>();
        this.warnings = new ArrayList<String>();
    }

    public void analisar(noArvore raizArvore) {
        if (raizArvore == null) return;
        visitar(raizArvore);
        verificarVariaveisNaoUsadas();
    }

    private void visitar(noArvore no) {
        if (no == null) {
            return;
        }

        switch (no.getNome()) {
            case "declaração de variáveis":
                processarDeclaracaoVariaveis(no);
                break;
            case "declaração de procedimento":
                processarProcedimento(no);
                break;
            case "atribuição":
                processarAtribuicao(no);
                break;
            case "comando condicional 1":
            case "comando repetitivo 1":
                processarIfWhile(no);
            case "variável":
            case "chamada de procedimento":
                verificarUsoIdentificador(no);
                break;
        }

        for (noArvore filho : no.getFilhos()) {
            visitar(filho);
        }
    }



    private void verificarUsoIdentificador(noArvore noId) {
        String nome =  noId.getValor();
        int linha =  noId.getLinha();

        if ((noId.getNome().equalsIgnoreCase("variável") || noId.getNome().equalsIgnoreCase("variavel")) && !noId.getFilhos().isEmpty()) {
            nome = noId.getFilhos().get(0).getValor();
            linha = noId.getFilhos().get(0).getLinha();
        }

        Simbolo s = tabelaSimbolos.buscarSimbolo(nome);
        if (s == null) {
            errosSemanticos.add("Linha " + linha + " -> Erro Semântico: Identificador '" + nome + "' não declarado.");
        } else {
            s.setUsada(true);
        }
    }

    private void processarAtribuicao(noArvore no) {

        noArvore noVar = no.getFilhos().get(0);
        verificarUsoIdentificador(noVar);

        if (no.getFilhos().size() > 2) {
            visitar(no.getFilhos().get(2));

        }
    }

    private void processarProcedimento(noArvore no) {

        String nomeProc = "";
        int linhaPRoc = -1;

        for (noArvore noFilho : no.getFilhos()) {
            if (noFilho.getNome().equalsIgnoreCase("Nome")) {
                nomeProc = noFilho.getValor();
                linhaPRoc = noFilho.getLinha();
                break;
            }
        }

        try {
            Simbolo s = new Simbolo(nomeProc, "PROCEDURE",
                    "PROCEDURE", tabelaSimbolos.getNivelLexicoAtual(), tabelaSimbolos.getEscopoAtual());
            tabelaSimbolos.adicionarSimbolo(s);
        } catch (Exception e) {
            errosSemanticos.add("Linha " + linhaPRoc + " -> Erro Semântico: Procedure '" + nomeProc + "' já declarada.");
        }

        tabelaSimbolos.entrarEScopo(nomeProc);

        for (noArvore filho : no.getFilhos()) {
            if (filho.getNome().equalsIgnoreCase("Parametros Formais")) {
                processarParametros(filho);
            } else if (!filho.getNome().equalsIgnoreCase("Nome")) {
                visitar(filho);
            }
        }

        verificarVariaveisNaoUsadas();

        tabelaSimbolos.sairEscopo();
    }

    private void processarParametros(noArvore noFilho) {
        for (noArvore noTipo : noFilho.getFilhos()) {
            String tipoVariavel = noTipo.getValor();

            for (noArvore noVar :  noTipo.getFilhos()) {
                String nomeVar = noVar.getNome();
                int linha =  noTipo.getLinha();

                try {
                    Simbolo s = new Simbolo(nomeVar, tipoVariavel, "parametro",
                            noVar.getValor(), null, false,
                            tabelaSimbolos.getNivelLexicoAtual(), tabelaSimbolos.getEscopoAtual());
                    tabelaSimbolos.adicionarSimbolo(s);
                } catch (Exception e) {
                    errosSemanticos.add("Linha " + linha + " -> " + e.getMessage());
                }
            }
        }
    }

    private void processarDeclaracaoVariaveis(noArvore noDeclaracao) {

        for (noArvore noTipo : noDeclaracao.getFilhos()) {
            String tipoVariavel = noTipo.getValor();

            for (noArvore noVar :  noTipo.getFilhos()) {
                String nomeVar = noVar.getValor();
                int linha =  noTipo.getLinha();

                try {
                    Simbolo s = new Simbolo(nomeVar, tipoVariavel, "variavel",
                            noVar.getValor(), null, false,
                            tabelaSimbolos.getNivelLexicoAtual(), tabelaSimbolos.getEscopoAtual());
                    tabelaSimbolos.adicionarSimbolo(s);
                    System.out.println(s.toString());
                } catch (Exception e) {
                    errosSemanticos.add("Linha " + linha + " -> " + e.getMessage());
                }
            }
        }
    }

    private void processarIfWhile(noArvore no) {

    }

    private void verificarVariaveisNaoUsadas() {
        for (Simbolo s : tabelaSimbolos.getPilhaEscopos().peek().values()) {
            if (!s.isUsada() && !s.getCategoria().equalsIgnoreCase("PROCEDURE")) {
                errosSemanticos.add("Aviso: Símbolo '" + s.getSimbolo() + "' declarado no escopo '" +
                        tabelaSimbolos.getEscopoAtual() + "', mas nunca foi utilizado.");
            }
        }
    }

    private String inferirTipoExpressao(noArvore no) {

        if (no == null) return "ERRO";

        String nomeNo = no.getNome();

        if (nomeNo.equalsIgnoreCase("terminal")) return "IGNORAR";

        String valorNo  = no.getValor() != null ? no.getValor().toLowerCase() : "";

        if (nomeNo.equalsIgnoreCase("operador relacional")) {
            return "BOOLEAN";
        }

        if (valorNo.equalsIgnoreCase("and|or|not")) {
            return "BOOLEAN";
        }

        if (nomeNo.equalsIgnoreCase("NUM")) return "INT";
        if (valorNo.equalsIgnoreCase("true") || valorNo.equalsIgnoreCase("false")) return "BOOLEAN";

        if (nomeNo.equalsIgnoreCase("Id") || nomeNo.equalsIgnoreCase("variavél") && no.getFilhos().isEmpty()) {
            Simbolo s = tabelaSimbolos.buscarSimbolo(no.getValor());
            if (s != null) {
                s.setUsada(true);
                return s.getTipo().toUpperCase();
            }
            return "ERRO";
        }

        String tipoREsultante = null;
        for (noArvore filho : no.getFilhos()) {
            String tipoFilho = inferirTipoExpressao(filho);
            if (tipoFilho.equalsIgnoreCase("ERRO")) return "ERRO";
            if (tipoFilho.equalsIgnoreCase("TERMINAL")) continue;

            if (tipoREsultante == null) {
                tipoREsultante = tipoFilho;
            } else if   (!tipoREsultante.equalsIgnoreCase(tipoFilho)) {
                errosSemanticos.add("Erro Semântico: Incompatibilidade de tipos na expressão. " +
                        "Tentativa de operar " + tipoREsultante + " com " + tipoFilho + ".");
                return "ERRO";
            }
        }
        return tipoREsultante != null ? tipoREsultante : "ERRO";
    }

    public List<String> getErrosSemanticos() {
        return errosSemanticos;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
