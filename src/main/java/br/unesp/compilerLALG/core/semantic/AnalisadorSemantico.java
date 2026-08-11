package br.unesp.compilerLALG.core.semantic;

import br.unesp.compilerLALG.core.parser.ast.noArvore;

import java.util.ArrayList;
import java.util.List;

public class AnalisadorSemantico {

    private final TabelaSimbolos tabelaSimbolos;
    private final List<String> errosSemanticos;
    private final List<String> warnings;

    private static final java.util.Set<String> PALAVRAS_RESERVADAS_E_REGRAS = java.util.Set.of(
            "identificador", "identificadores", "variável", "variavel", "terminal",
            "procedure", "procedimento", "program", "programa", "declaração",
            "declaracao", "sub-rotina", "subrotina", "int", "integer", "boolean",
            "real", "begin", "end", "if", "then", "else", "while", "do", "read", "write", ";"
    );

    public AnalisadorSemantico() {
        this.tabelaSimbolos = new TabelaSimbolos();
        this.errosSemanticos = new ArrayList<>();
        this.warnings = new ArrayList<>();

    }

    public void analisar(noArvore raizArvore) {
        if (raizArvore == null) return;

        tabelaSimbolos.entrarEscopo();

        tabelaSimbolos.inserir("read", "procedure", "procedimento", 0);
        tabelaSimbolos.inserir("write", "procedure", "procedimento", 0);
        tabelaSimbolos.inserir("true", "boolean", "constante", 0);
        tabelaSimbolos.inserir("false", "boolean", "constante", 0);

        visitar(raizArvore);

    }

    private void visitar(noArvore no) {
        if (no == null) {
            return;
        }
        //System.out.println("Visitação do nó: " + no.getNome() + " com valor: " + no.getValor() + " na linha: " + no.getLinha());

        switch (no.getNome()) {
            case "programa":
                // regista o nome do programa na Tabela para não dar erroo
                noArvore idProg = buscarNoPorNome(no, "identificador");
                String nomeProg = extrairNomeLexema(idProg != null ? idProg : no);
                if (!nomeProg.isEmpty()) {
                    tabelaSimbolos.inserir(nomeProg, "programa", "nome_prog", no.getLinha());
                }
                break;
            //case "parte declaração variáveis":
            case "declaração variáveis":
                registrarVariaveisTabela(no);
                return;
            case "declaração de sub-rotina":
            case "declaração sub-rotina":
            case "declaração procedimento":
            case "procedure":
                processarDeclaracaoProcedimento(no);
                return;
            case "atribuição":
            case "comando atribuição":
                validarAtribuicao(no);
                return;
            case "comando condicional 1":
            case "comando repetitivo 1":
            case "comando if":
            case "comando while":
            case "if":
            case "while":
                validarIfWhile(no);
                break; // Apenas valida a expressão, e CONTINUA a visitar os comandos de dentro do bloco
            case "chamada procedimento":
                validarChamadaProcedimento(no);
                return;
            case "identificador":
            case "variável":
                String nomeVar = obterNomeReal(no);
                if (nomeVar.isEmpty()) return;

                Simbolo sim = tabelaSimbolos.buscar(nomeVar);
                if (sim == null) {
                    int linhaErro = no.getLinha() > 0 ? no.getLinha() : (!no.getFilhos().isEmpty() ? no.getFilhos().get(0).getLinha() : -1);
                    errosSemanticos.add("Linha " + linhaErro + " -> Erro Semântico: Variável ou Procedimento '" + nomeVar + "' não declarado!");
                    return;
                }

                sim.setUsada(true);
                int linhaReal = no.getLinha() > 0 ? no.getLinha() : (!no.getFilhos().isEmpty() ? no.getFilhos().get(0).getLinha() : -1);
                tabelaSimbolos.inserirReferencia(nomeVar, linhaReal);
                return;
            case "read":
            case "write":
                int linhaRW = no.getLinha() > 0 ? no.getLinha() : (!no.getFilhos().isEmpty() ? no.getFilhos().get(0).getLinha() : -1);
                tabelaSimbolos.inserirReferencia(no.getNome(), linhaRW);

                // desce nos filhos para avaliar as variáveis passadas nos parênteses
                for (noArvore f : no.getFilhos()) {
                    inferirTipoExpressao(f);
                }
                return;
        }

        for (noArvore filho : no.getFilhos()) {
            visitar(filho);
        }
    }

    /**
     * Regra 1: Verificação de Declaração de Variáveis e Procedimentos
     * Regra 2: Checagem de Expressões: Nos comandos if e while, a expressão tem obrigatoriamente de resultar em boolean.
     * Regrea 3: Compatibilidade de Atribuição: O lado esquerdo tem de ser estruturalmente compatível com o lado direito
     * Regra 4: Resolução Dinâmica do "Filho Vazio"
     */


    /**
     * REGRA 1: Declaração de Variáveis na Tabela de Símbolos
     */
    private void registrarVariaveisTabela(noArvore noDecl) {
        String tipoVariavel = "desconhecido";

        // Busca o nó que diz o tipo (int, boolean)
        noArvore noTipo = buscarNoPorNome(noDecl, "tipo");
        if (noTipo != null && !noTipo.getFilhos().isEmpty()) {
            tipoVariavel = noTipo.getFilhos().get(0).getValor();
            if (tipoVariavel == null || tipoVariavel.trim().isEmpty()) {
                tipoVariavel = noTipo.getFilhos().get(0).getNome(); // Fallback de segurança
            }
        }

        // Extrai e guarda as variáveis
        extrairEInserirVariaveis(noDecl, tipoVariavel);
    }

    /**
     * Varre as declarações para encontrar os Nomes
     */
    private void extrairEInserirVariaveis(noArvore no, String tipo) {
        if (no == null) return;

        if (no.getNome().equalsIgnoreCase("variável") || no.getNome().equalsIgnoreCase("identificador")) {
            String nomeVar = extrairNomeLexema(no);
//            if (!nomeVar.trim().isEmpty()) {
//                if (!no.getFilhos().isEmpty()) {
//                    nomeVar = no.getFilhos().get(0).getValor();
//                }
//            }

            if (!nomeVar.trim().isEmpty()) {
                //int linhaReal = no.getLinha();
//                if (linhaReal <= 0 && !no.getFilhos().isEmpty()) {
//                    linhaReal = no.getFilhos().get(0).getLinha();
//                }

                int linhaReal = no.getLinha() > 0 ? no.getLinha() : -1;
                tabelaSimbolos.inserir(nomeVar, tipo, "variavel", linhaReal);
            }
        }

        // Continua procurando variáveis vizinhas (ex: int a, b, soma)
        for (noArvore filho : no.getFilhos()) {
            extrairEInserirVariaveis(filho, tipo);
        }
    }

    /**
     * REGRA 2: Escopo e Procedimentos (Variáveis Locais)
     */
    private void processarDeclaracaoProcedimento(noArvore no) {


        noArvore noId = buscarNoPorNome(no, "identificador");
        String nomeProc = noId != null ? extrairNomeLexema(noId) : extrairNomeLexema(no);

        if (nomeProc.isEmpty()) return; // Se não conseguir extrair o nome do procedimento, ignora

        int linhaReal = no.getLinha() > 0 ? no.getLinha() : (noId != null && noId.getLinha() > 0 ? noId.getLinha() : 1);

        // insere o procedimento na tabela global com o nome correto
        tabelaSimbolos.inserir(nomeProc, "procedure", "procedimento", linhaReal);

        // abre o escopo local da subrotina (nivel 1)
        tabelaSimbolos.entrarEscopo();

        noArvore parametros = buscarNoPorNome(no, "parâmetros formais");
        if (parametros == null) parametros = buscarNoPorNome(no, "lista de parâmetros");
        if (parametros == null) parametros = buscarNoPorNome(no, "seção de parâmetros formais");

        if (parametros != null) {
            registrarVariaveisTabela(parametros);
        }

        // analisa o corpo do procedimento
        for (noArvore filho : no.getFilhos()) {
            String nomeFilho = filho.getNome().toLowerCase();
            if (!nomeFilho.equalsIgnoreCase("identificador") &&
                    !filho.getNome().contains("parâmetros") &&
                    !nomeFilho.contains("parametros")) {
                visitar(filho);
            }
        }

        tabelaSimbolos.sairEscopo();

//        if (noId != null) {
//            nomeProc = noId.getValor();
//            if (nomeProc == null || nomeProc.trim().isEmpty() && !noId.getFilhos().isEmpty()) {
//                nomeProc = noId.getFilhos().get(0).getValor();
//            }
//        }
//
//        // Insere o procedimento na Tabela Global
//        if (!nomeProc.isEmpty()) {
//            int linhaReal = noId != null ? noId.getLinha() : no.getLinha();
//            tabelaSimbolos.inserir(nomeProc, "procedure", "procedimento", linhaReal);
//        }
//
//        // Abre um NOVO ESCOPO
//        tabelaSimbolos.entrarEscopo();
//
//        // Verifica os Parâmetros Formais
//        noArvore parametros = buscarNoPorNome(no, "parâmetros formais");
//        if (parametros == null) parametros = buscarNoPorNome(no, "lista de parâmetros");
//
//        if (parametros != null) {
//            registrarVariaveisTabela(parametros);
//        }
//
//        // 4. Analisa o corpo do procedimento
//        for (noArvore filho : no.getFilhos()) {
//            if (!filho.getNome().equalsIgnoreCase("identificador") &&
//                    !filho.getNome().contains("parâmetros")) {
//                visitar(filho);
//            }
//        }
//
//        // 5. Fecha o escopo (Destrói as variáveis locais e volta pro global)
//        tabelaSimbolos.sairEscopo();
    }

    /**
     * REGRA 3: Validação de Atribuição (Tipagem Forte)
     */
    private void validarAtribuicao(noArvore no) {
        if (no.getFilhos().size() < 2) return;

        noArvore noLadoEsquerdo = no.getFilhos().get(0);
        noArvore noLadoDireito = no.getFilhos().get(no.getFilhos().size() - 1);

        String tipoVar = inferirTipoExpressao(noLadoEsquerdo);
        String tipoExp = inferirTipoExpressao(noLadoDireito);

        if (!tipoVar.equals("desconhecido") && !tipoExp.equals("desconhecido") && !tipoVar.equals("ignorar")) {

            // não foi implementado o tipo 'real', mas vou deixar aqui para implementar no futuro
            if (tipoVar.equals("int") && tipoExp.equals("real")) {
                errosSemanticos.add("Linha " + no.getLinha() + " -> Erro Semântico: Atribuição inválida. Uma variável 'int' não pode receber um valor 'real'.");
            } else if (!tipoVar.equals(tipoExp) && !(tipoVar.equals("real") && tipoExp.equals("int"))) {
                errosSemanticos.add("Linha " + no.getLinha() + " -> Erro Semântico: Tipos incompatíveis na atribuição. Não é possível atribuir '"
                        + tipoExp + "' a '" + tipoVar + "'.");
            }
        }
    }

    /**
     * REGRA 4: Validação de Comandos de Controle (IF/WHILE) -> Exige tipo Boolean
     */
    private void validarIfWhile(noArvore no) {
        noArvore noExp = buscarNoPorNome(no, "expressão");
        if (noExp != null) {
            String tipoExp = inferirTipoExpressao(noExp);
            if (!tipoExp.equals("desconhecido") && !tipoExp.equals("boolean")) {
                errosSemanticos.add("Linha " + no.getLinha() + " -> Erro Semântico: O comando if/while exige uma condição booleana, mas a expressão resulta em '" + tipoExp + "'.");
            }
        }

        // Assegura a descida para o restante do if (o que vem depois do THEN ou DO)
        for (noArvore filho : no.getFilhos()) {
            if (!filho.getNome().equalsIgnoreCase("expressão")) {
                visitar(filho);
            }
        }
    }

    /**
     * REGRA 5: Inferência de Tipos de Expressão (Matemática e Lógica)
     */
    public String inferirTipoExpressao(noArvore no) {
        if (no == null) return "desconhecido";

        String nomeNo = no.getNome().toLowerCase();

        // Ignora símbolos da árvore que não contêm dados (chaves, sinais e delimitadores)
        if (nomeNo.equals("mais") || nomeNo.equals("menos") || nomeNo.equals("vezes") ||
                nomeNo.equals("divisao") || nomeNo.equals("abrepar") || nomeNo.equals("fechapar") ||
                nomeNo.equals("relação") || nomeNo.equals("opsoma") || nomeNo.equals("opmult") || nomeNo.equals("oprel")) {
            return "ignorar";
        }

        switch (nomeNo) {
            case "expressão":
                // Regra EBNF: Se a expressão tiver mais de um lado com um Operador Relacional (=, <, >, etc)
                if (no.getFilhos().size() >= 3 && contemNo(no.getFilhos(), "relação")) {
                    String tipoEsq = inferirTipoExpressao(no.getFilhos().get(0));
                    String tipoDir = inferirTipoExpressao(no.getFilhos().get(no.getFilhos().size() - 1));

                    if (!tipoEsq.equals("desconhecido") && !tipoDir.equals("desconhecido")) {
                        if (tipoEsq.equals("boolean") || tipoDir.equals("boolean")) {
                            if (!tipoEsq.equals(tipoDir)) {
                                errosSemanticos.add("Linha " + no.getLinha() + " -> Erro Semântico: Operação ilegal. Não é possível comparar '" + tipoEsq + "' com '" + tipoDir + "'.");
                            }
                        }
                    }
                    return "boolean"; // O resultado de uma comparação é SEMPRE boolean
                } else if (!no.getFilhos().isEmpty()) {
                    return inferirTipoExpressao(no.getFilhos().get(0)); // Passa a responsabilidade para a expressão simples
                }
                break;

            case "expressão simples":
            case "termo":
                String tipoResultante = null;
                for (noArvore filho : no.getFilhos()) {
                    String tipoFilho = inferirTipoExpressao(filho);

                    if (!tipoFilho.equals("ignorar") && !tipoFilho.equals("desconhecido")) {
                        if (tipoResultante == null) {
                            tipoResultante = tipoFilho;
                        } else if (!tipoResultante.equals(tipoFilho)) {
                            if ((tipoResultante.equals("real") && tipoFilho.equals("int")) ||
                                    (tipoResultante.equals("int") && tipoFilho.equals("real"))) {
                                tipoResultante = "real";
                            } else {
                                errosSemanticos.add("Linha " + no.getLinha() + " -> Erro Semântico: Operação matemática com tipos incompatíveis: " + tipoResultante + " e " + tipoFilho);
                                tipoResultante = "desconhecido";
                            }
                        }
                    }
                }
                return tipoResultante != null ? tipoResultante : "desconhecido";

            case "fator":
                for (noArvore filho : no.getFilhos()) {
                    String t = inferirTipoExpressao(filho);
                    if (!t.equals("ignorar")) return t;
                }
                break;

            case "num":
            case "numero":
                if (no.getValor() != null && no.getValor().contains(".")) {
                    return "real";
                }
                return "int";

            case "true":
            case "false":
                return "boolean";

            case "identificador":
            case "variável":
                String nomeVar = extrairNomeLexema(no);

                if (nomeVar.isEmpty()) return "ignorar";

//                if (nomeVar.trim().isEmpty()) {
//                    if (!no.getFilhos().isEmpty()) {
//                        nomeVar = no.getFilhos().get(0).getValor();
//                    }
//                }
//
//                if (nomeVar == null || nomeVar.trim().isEmpty()) {
//                    return "ignorar";
//                }

                Simbolo sim = tabelaSimbolos.buscar(nomeVar);
                if (sim == null) {
                    int linhaErro = no.getLinha();

                    if (linhaErro <= 0 && !no.getFilhos().isEmpty()) {
                        linhaErro = no.getFilhos().get(0).getLinha();
                    }

                    errosSemanticos.add("Linha " + linhaErro + " -> Erro Semântico: Variável ou Procedimento '" + nomeVar + "' não declarado!");
                    return "desconhecido";
                }
                sim.setUsada(true);

                int linhaReal = no.getLinha() > 0 ? no.getLinha() : (!no.getFilhos().isEmpty() ? no.getFilhos().get(0).getLinha() : -1);
                tabelaSimbolos.inserirReferencia(nomeVar, linhaReal);

                return sim.getTipo();
        }

        // Descida recursiva padrão para qualquer outro bloco
        for (noArvore filho : no.getFilhos()) {
            String tipo = inferirTipoExpressao(filho);
            if (!tipo.equals("ignorar") && !tipo.equals("desconhecido")) {
                return tipo;
            }
        }

        return "desconhecido";
    }

    private void validarChamadaProcedimento(noArvore no) {
        noArvore noId = buscarNoPorNome(no, "identificador");
        String nomeProc = noId != null ? extrairNomeLexema(noId) : extrairNomeLexema(no);

        if (nomeProc.isEmpty()) return;

        Simbolo proc = tabelaSimbolos.buscar(nomeProc);
        if (proc == null) {
            int linhaErro = no.getLinha() > 0 ? no.getLinha() : -1;
            errosSemanticos.add("Linha " + linhaErro + " -> Erro Semântico: Procedimento '" + nomeProc + "' não declarado.");
            return;
        }

        // marca que o procedimento foi efetivamente utilizado no código
        proc.setUsada(true);

        // registra a referencia da chamada do procedimento
        int linhaChamada = no.getLinha() > 0 ? no.getLinha() : (noId != null ? noId.getLinha() : -1);
        tabelaSimbolos.inserirReferencia(nomeProc, linhaChamada);

        // valida as variáveis que o utilizador passou dentro dos parênteses
        noArvore listaExp = buscarNoPorNome(no, "lista de expressões");
        if (listaExp != null) {
            for (noArvore exp : listaExp.getFilhos()) {
                inferirTipoExpressao(exp);
            }
        }
    }

    private noArvore buscarNoPorNome(noArvore no, String alvo) {
        if (no.getNome().equalsIgnoreCase(alvo)) return no;
        for (noArvore f : no.getFilhos()) {
            noArvore achou = buscarNoPorNome(f, alvo);
            if (achou != null) return achou;
        }
        return null;
    }

    private boolean contemNo(List<noArvore> filhos, String nomeAlvo) {
        for (noArvore f : filhos) {
            if (f.getNome().equalsIgnoreCase(nomeAlvo)) return true;
        }
        return false;
    }

    // extrai nomes de variáveis/procedimentos
    private String obterNomeReal(noArvore no) {
        if (no == null) return "";

        // tenta pegar o valor do próprio nó
        String valor = no.getValor();

        // se o valor existir e não for uma etiqueta estrutural do Parser, achamos o nome
        if (valor != null && !valor.trim().isEmpty() &&
                !valor.equalsIgnoreCase("identificador") &&
                !valor.equalsIgnoreCase("variável") &&
                !valor.equalsIgnoreCase("variavel") &&
                !valor.equalsIgnoreCase("procedure") &&
                !valor.equalsIgnoreCase("terminal")) {

            return valor.trim();
        }

        // se o valor era apenas "lixo sintático",
        // varre todos os filhos à procura da verdadeira palavra (ex: "soma")
        for (noArvore filho : no.getFilhos()) {
            String valorFilho = obterNomeReal(filho);
            if (!valorFilho.isEmpty()) {
                return valorFilho; // Retorna a primeira palavra real que encontrar!
            }
        }

        return "";
    }

    private String extrairNomeLexema(noArvore no) {
        if (no == null) return "";

        // testa se o valor do nó atual é um lexema válido
        String val = no.getValor();
        if (val != null && !val.trim().isEmpty()) {
            String valTrim = val.trim();
            if (!PALAVRAS_RESERVADAS_E_REGRAS.contains(valTrim.toLowerCase())) {
                return valTrim;
            }
        }

        // testa se o nome do nó é um lexema válido (caso o Parser tenha salvo no nome)
        String nome = no.getNome();
        if (nome != null && !nome.trim().isEmpty()) {
            String nomeTrim = nome.trim();
            if (!PALAVRAS_RESERVADAS_E_REGRAS.contains(nomeTrim.toLowerCase())) {
                return nomeTrim;
            }
        }

        // se for nó estrutural (ex: "identificador"), percorre os filhos recursivamente
        if (no.getFilhos() != null) {
            for (noArvore filho : no.getFilhos()) {
                String lexemaFilho = extrairNomeLexema(filho);
                if (!lexemaFilho.isEmpty()) {
                    return lexemaFilho;
                }
            }
        }

        return "";
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
