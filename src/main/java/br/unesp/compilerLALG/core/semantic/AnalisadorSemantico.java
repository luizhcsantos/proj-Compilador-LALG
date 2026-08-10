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
            case "parte declaração variáveis":
                processarParteDeclaracaoVariaveis(no);
                break;
            case "declaração procedimento":
                processarProcedimento(no);
                break;
            case "atribuição":
                processarAtribuicao(no);
                break;
            case "comando condicional 1":
            case "comando repetitivo 1":
                processarIfWhile(no);
            case "variável":
            case "chamada procedimento":
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

        if (no.getNome().equalsIgnoreCase("atribuição")) {

            // o primeiro filho é a variável e a expressão vem depois
            noArvore noLadoEsquerdo = no.getFilhos().get(0); // o 'a'
            noArvore noLadoDireito = no.getFilhos().get(1);  // o '10' ou 'a + b'

            String tipoVar = inferirTipoExpressao(noLadoEsquerdo);
            String tipoExp = inferirTipoExpressao(noLadoDireito);

            if (!tipoVar.equals("desconhecido") && !tipoExp.equals("desconhecido")) {
                if (!tipoVar.equals(tipoExp)) {
                    errosSemanticos.add("Erro Semântico: Não é possível atribuir um valor do tipo '"
                            + tipoExp + "' a uma variável do tipo '" + tipoVar + "'");
                }
            }
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

    private void processarParteDeclaracaoVariaveis(noArvore noParteDeclaracao) {
        for (noArvore noDeclaracao : noParteDeclaracao.getFilhos()) {
            if (noDeclaracao.getNome().equalsIgnoreCase("declaracao variaveis")) {
                for (noArvore noTipo : noDeclaracao.getFilhos()) {
                    if (noTipo.getNome().equalsIgnoreCase("Tipo")) {
                        String tipoVariavel =  noTipo.getValor(); // int ou boolean

                        for (noArvore noId : noTipo.getFilhos()) {
                            if (noId.getNome().equalsIgnoreCase("Id")) {
                                String nomeVar =  noId.getValor();
                                int linha =  noId.getLinha();

                                try {
                                    Simbolo s = new Simbolo(nomeVar, tipoVariavel, "VARIAVEL",
                                            tabelaSimbolos.getNivelLexicoAtual(), tabelaSimbolos.getEscopoAtual());
                                    tabelaSimbolos.adicionarSimbolo(s);
                                } catch (Exception e) {
                                    errosSemanticos.add("Linha " + linha + " -> " + e.getMessage());
                                }
                            }
                        }
                    }
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

    private void processarIfWhile(noArvore noComando) {

        for (noArvore filho : noComando.getFilhos()) {
            if (filho.getNome().equals("Condição")) {
                if (!filho.getFilhos().isEmpty()) {
                    String tipoCondicao = inferirTipoExpressao(filho.getFilhos().get(0));

                    if (!tipoCondicao.equals("BOOLEAN") && !tipoCondicao.equals("ERRO") && !tipoCondicao.equals("IGNORAR")) {
                        errosSemanticos.add("Erro Semântico: A condição do comando '" + noComando.getValor() + "' deve resultar em um valor BOOLEAN, mas retornou " + tipoCondicao + ".");
                    }
                }
            } else {
                // entra no " comando composto" e depois no "comando composto linha"
                visitar(filho);
            }
        }
    }

    private void verificarVariaveisNaoUsadas() {
        for (Simbolo s : tabelaSimbolos.getPilhaEscopos().peek().values()) {
            if (!s.isUsada() && !s.getCategoria().equalsIgnoreCase("PROCEDURE")) {
                errosSemanticos.add("Aviso: Símbolo '" + s.getSimbolo() + "' declarado no escopo '" +
                        tabelaSimbolos.getEscopoAtual() + "', mas nunca foi utilizado.");
            }
        }
    }

    public String inferirTipoExpressao(noArvore no) {
        if (no == null) return "desconhecido";

        String nomeNo = no.getNome().toLowerCase();

        // IGNORAR NÓS VISUAIS: Símbolos na AST que não têm tipo de dado
        if (nomeNo.equals("mais") || nomeNo.equals("menos") || nomeNo.equals("vezes") ||
                nomeNo.equals("divisao") || nomeNo.equals("abrepar") || nomeNo.equals("fechapar") ||
                nomeNo.equals("relação")) {
            return "ignorar";
        }

        switch (nomeNo) {
            case "expressão":
                // <expressão simples> [<relação> <expressão simples>]
                if (no.getFilhos().size() >= 3) {
                    // se tem relação, o resultado de uma comparação sempre é boolean
                    String tipoEsq = inferirTipoExpressao(no.getFilhos().get(0));
                    String tipoDir = inferirTipoExpressao(no.getFilhos().get(2)); // O filho 1 é o nó relação

                    if (!tipoEsq.equals("desconhecido") && !tipoDir.equals("desconhecido") && !tipoEsq.equals(tipoDir)) {
                        errosSemanticos.add("Tipos incompatíveis na comparação. Não é possível comparar '" + tipoEsq + "' com '" + tipoDir + "'");
                    }
                    return "boolean";
                } else if (!no.getFilhos().isEmpty()) {
                    // se não tem relação, apenas propaga o tipo de baixo para cimaa
                    return inferirTipoExpressao(no.getFilhos().get(0));
                }
                break;

            case "expressão simples":
            case "termo":
                // avalia a corrente de somas/subtrações ou multiplicações
                String tipoREsultante = null;
                for (noArvore filho : no.getFilhos()) {
                    String tipoFilho = inferirTipoExpressao(filho);

                    if (!tipoFilho.equals("ignorar") && !tipoFilho.equals("desconhecido")) {
                        if (tipoREsultante == null) {
                            tipoREsultante = tipoFilho; // Define o tipo base (ex: int)
                        } else if (!tipoREsultante.equals(tipoFilho)) {
                            errosSemanticos.add("Operação matemática com tipos incompatíveis: " + tipoREsultante + " e " + tipoFilho);
                            tipoREsultante = "desconhecido"; // proteção para não flodar o console com o mesmo erroo
                        }
                    }
                }
                return tipoREsultante != null ? tipoREsultante : "desconhecido";

            case "fator":
                // o fator só repassa o tipo de quem está dentro dele (num, variável, ou expressão entre parênteses)
                for (noArvore filho : no.getFilhos()) {
                    String t = inferirTipoExpressao(filho);
                    if (!t.equals("ignorar")) return t;
                }
                break;

            case "num":
                // possível implementação do tipo real
//                if (no.getValor() != null && no.getValor().contains(".")) {
//                    return "real"; // ou float, dependendo da sua definição
//                }
                return "int"; //  integer

            case "identificador":
            case "variável":
                // pegue o nome real da variável que foi guardado no nó
                String nomeVar = no.getValor() != null && !no.getValor().isEmpty() ? no.getValor() : no.getNome();

                Simbolo sim = tabelaSimbolos.buscarSimbolo(nomeVar);
                if (sim == null) {
                    errosSemanticos.add("Erro Semântico: Variável '" + nomeVar + "' não declarada neste escopo!");
                    return "desconhecido";
                }
                return sim.getTipo();
        }

        for (noArvore filho : no.getFilhos()) {
            String tipo = inferirTipoExpressao(filho);
            if (!tipo.equals("ignorar") && !tipo.equals("desconhecido")) {
                return tipo;
            }
        }

        return "desconhecido";
    }
    public List<String> getErrosSemanticos() {
        return errosSemanticos;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public TabelaSimbolos getTabelaSimbolos() {
        return tabelaSimbolos;
    }
}
