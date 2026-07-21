package br.unesp.compilerLALG.core.semantic;

import br.unesp.compilerLALG.core.parser.ast.noArvoreDTO;

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

    public void analisar(noArvoreDTO raizArvore) {
        if (raizArvore == null) return;
        visitar(raizArvore);
        verificarVariaveisNaoUsadas();
    }

    private void visitar(noArvoreDTO no) {
        if (no == null) {
            return;
        }

        switch (no.getNome()) {
            case "Declaração de variáveis":
                processarDeclaracaoVariaveis(no);
                break;
            case "Declaração de procedimento":
                processarProcedimento(no);
                break;
            case "Atribuição":
                processarAtribuicao(no);
                break;
            case "Comando condicional 1":
            case "Comando repetitivo 1":
                processarIfWhile(no);
            case "Variável":
            case "Chamada de procedimento":
                verificarUsoIdentificador(no);
                break;
        }

        for (noArvoreDTO filho : no.getFilhos()) {
            visitar(filho);
        }
    }



    private void verificarUsoIdentificador(noArvoreDTO noId) {
        String nome =  noId.getValor();
        int linha =  noId.getLinha();

        if (noId.getNome().equals("Variável") && !noId.getFilhos().isEmpty()) {
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

    private void processarAtribuicao(noArvoreDTO no) {

        noArvoreDTO noVar = no.getFilhos().get(0);
        verificarUsoIdentificador(noVar);

        if (no.getFilhos().size() > 2) {
            visitar(no.getFilhos().get(2));
        }
    }

    private void processarProcedimento(noArvoreDTO no) {

        String nomeProc = "";
        int linhaPRoc = -1;

        for (noArvoreDTO noFilho : no.getFilhos()) {
            if (noFilho.getNome().equals("Nome")) {
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

        for (noArvoreDTO filho : no.getFilhos()) {
            if (filho.getNome().equals("Parametros Formais")) {
                processarParametros(filho);
            } else if (!filho.getNome().equals("Nome")) {
                visitar(filho);
            }
        }

        verificarVariaveisNaoUsadas();

        tabelaSimbolos.sairEscopo();
    }

    private void processarParametros(noArvoreDTO noFilho) {
        for (noArvoreDTO noTipo : noFilho.getFilhos()) {
            String tipoVariavel = noTipo.getValor();

            for (noArvoreDTO noVar :  noTipo.getFilhos()) {
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

    private void processarDeclaracaoVariaveis(noArvoreDTO noDeclaracao) {

        for (noArvoreDTO noTipo : noDeclaracao.getFilhos()) {
            String tipoVariavel = noTipo.getValor();

            for (noArvoreDTO noVar :  noTipo.getFilhos()) {
                String nomeVar = noVar.getNome();
                int linha =  noTipo.getLinha();

                try {
                    Simbolo s = new Simbolo(nomeVar, tipoVariavel, "variavel",
                            noVar.getValor(), null, false,
                            tabelaSimbolos.getNivelLexicoAtual(), tabelaSimbolos.getEscopoAtual());
                    tabelaSimbolos.adicionarSimbolo(s);
                } catch (Exception e) {
                    errosSemanticos.add("Linha " + linha + " -> " + e.getMessage());
                }
            }
        }
    }

    private void processarIfWhile(noArvoreDTO no) {

    }

    private void verificarVariaveisNaoUsadas() {
        for (Simbolo s : tabelaSimbolos.getPilhaEscopos().peek().values()) {
            if (!s.isUsada() && !s.getCategoria().equals("PROCEDURE")) {
                errosSemanticos.add("Aviso: Símbolo '" + s.getSimbolo() + "' declarado no escopo '" +
                        tabelaSimbolos.getEscopoAtual() + "', mas nunca foi utilizado.");
            }
        }
    }

    private String inferirTipoExpressao(noArvoreDTO no) {

        if (no == null) return "ERRO";

        String nomeNo = no.getNome();
        String valorNo  = no.getValor() != null ? no.getValor().toLowerCase() : "";

        if (nomeNo.equals("Operador relacional")) {
            return "BOOLEAN";
        }

        if (valorNo.equals("and|or|not")) {
            return "BOOLEAN";
        }

        if (nomeNo.equals("NUM")) return "INT";
        if (valorNo.equals("true") || valorNo.equals("false")) return "BOOLEAN";

        if (nomeNo.equals("Id") || nomeNo.equals("Variavél") && no.getFilhos().isEmpty()) {
            Simbolo s = tabelaSimbolos.buscarSimbolo(no.getValor());
            if (s != null) {
                s.setUsada(true);
                return s.getTipo().toUpperCase();
            }
            return "ERRO";
        }

        String tipoREsultante = null;
        for (noArvoreDTO filho : no.getFilhos()) {
            String tipoFilho = inferirTipoExpressao(filho);
            if (tipoFilho.equals("ERRO")) continue;

            if (tipoREsultante == null) {
                tipoREsultante = tipoFilho;
            } else if   (!tipoREsultante.equals(tipoFilho)) {
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
