package br.unesp.compilerLALG.core.semantic;

import br.unesp.compilerLALG.core.parser.ast.noArvore;

import java.util.ArrayList;
import java.util.List;

public class AnalisadorSemantico {

    private final TabelaSimbolos tabelaSimbolos;
    private final List<String> errosSemanticos;
    private final List<String> warnings;

    public AnalisadorSemantico() {
        this.tabelaSimbolos = new TabelaSimbolos();
        this.errosSemanticos = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void analisar(noArvore raizArvore) {
        if (raizArvore == null) return;

        tabelaSimbolos.entrarEscopo();
        visitar(raizArvore);

        //verificarUsoIdentificador(raizArvore); // Verifica se todos os identificadores foram declarados
        //tabelaSimbolos.sairEscopo();

    }

    private void visitar(noArvore no) {
        if (no == null) {
            return;
        }
        //System.out.println("Visitação do nó: " + no.getNome() + " com valor: " + no.getValor() + " na linha: " + no.getLinha());

        switch (no.getNome()) {
            //case "parte declaração variáveis":
            case "declaração variáveis":
                registrarVariaveisTabela(no);
                return;
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
            case "read":
            case "write":
                // Desce nos filhos apenas para verificar se as variáveis impressas/lidas existem
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
            String nomeVar = no.getValor();
            System.out.println(nomeVar);
            if (nomeVar == null || nomeVar.trim().isEmpty()) {
                if (!no.getFilhos().isEmpty()) {
                    nomeVar = no.getFilhos().get(0).getValor();
                }
            }

            if (nomeVar != null && !nomeVar.trim().isEmpty()) {
                int linhaReal = no.getLinha();
                if (linhaReal <= 0 && !no.getFilhos().isEmpty()) {
                    linhaReal = no.getFilhos().get(0).getLinha();
                }

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
        String nomeProc = "";
        noArvore noId = buscarNoPorNome(no, "identificador");

        if (noId != null) {
            nomeProc = noId.getValor();
            if (nomeProc == null || nomeProc.trim().isEmpty() && !noId.getFilhos().isEmpty()) {
                nomeProc = noId.getFilhos().get(0).getValor();
            }
        }

        // Insere o procedimento na Tabela Global
        if (!nomeProc.isEmpty()) {
            tabelaSimbolos.inserir(nomeProc, "procedure", "procedimento", no.getLinha());
        }

        // Abre um NOVO ESCOPO
        tabelaSimbolos.entrarEscopo();

        // Verifica os Parâmetros Formais
        noArvore parametros = buscarNoPorNome(no, "parâmetros formais");
        if (parametros == null) parametros = buscarNoPorNome(no, "lista de parâmetros");

        if (parametros != null) {
            registrarVariaveisTabela(parametros);
        }

        // 4. Analisa o corpo do procedimento
        for (noArvore filho : no.getFilhos()) {
            if (!filho.getNome().equalsIgnoreCase("identificador") &&
                    !filho.getNome().contains("parâmetros")) {
                visitar(filho);
            }
        }

        // 5. Fecha o escopo (Destrói as variáveis locais e volta pro global)
        tabelaSimbolos.sairEscopo();
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
                errosSemanticos.add("Linha " + no.getLinha() + " -> Erro Semântico: Tipos incompatíveis na atribuição. Não é possível atribuir '" + tipoExp + "' a '" + tipoVar + "'.");
            } else {
                // Extrai o texto do lado direito e atualiza o símbolo
                String nomeVar = noLadoEsquerdo.getValor();
                if ((nomeVar == null || nomeVar.trim().isEmpty()) && !noLadoEsquerdo.getFilhos().isEmpty()) {
                    nomeVar = noLadoEsquerdo.getFilhos().get(0).getValor();
                }

                if (nomeVar != null && !nomeVar.trim().isEmpty()) {
                    Simbolo sim = tabelaSimbolos.buscar(nomeVar);
                    if (sim != null) {
                        String valorTexto = reconstruirExpressaoTexto(noLadoDireito);
                        sim.setValor(valorTexto);
                    }
                }
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
                String nomeVar = no.getValor();

                if (nomeVar == null || nomeVar.trim().isEmpty()) {
                    if (!no.getFilhos().isEmpty()) {
                        nomeVar = no.getFilhos().get(0).getValor();
                    }
                }

                if (nomeVar == null || nomeVar.trim().isEmpty()) {
                    return "ignorar";
                }

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
        String nomeProc = "";
        noArvore noId = buscarNoPorNome(no, "identificador");

        if (noId != null) {
            nomeProc = noId.getValor();
            if (nomeProc == null || nomeProc.trim().isEmpty() && !noId.getFilhos().isEmpty()) {
                nomeProc = noId.getFilhos().get(0).getValor();
            }
        }

        Simbolo proc = tabelaSimbolos.buscar(nomeProc);
        if (proc == null) {
            errosSemanticos.add("Linha " + no.getLinha() + " -> Erro Semântico: O procedimento '" + nomeProc + "' não foi declarado ou não existe neste escopo.");
            return;
        }

        // Faz com que as expressões de argumento passadas sejam visitadas e checadas
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

    private String reconstruirExpressaoTexto(noArvore no) {
        if (no == null) return "";

        if (no.getValor() != null && !no.getValor().trim().isEmpty()) {
            return no.getValor();
        }

        StringBuilder sb = new StringBuilder();
        for (noArvore filho : no.getFilhos()) {
            String textoFilho = reconstruirExpressaoTexto(filho);
            if (!textoFilho.isEmpty()) {
                if (sb.length() > 0 && !textoFilho.equals(";") && !textoFilho.equals(")")) {
                    sb.append(" ");
                }
                sb.append(textoFilho);
            }
        }
        return sb.toString();
    }

//    private void verificarUsoIdentificador(noArvore noId) {
//        String nome = noId.getValor();
//        int linha = noId.getLinha();
//
//        if ((noId.getNome().equalsIgnoreCase("variável") || noId.getNome().equalsIgnoreCase("variavel")) && !noId.getFilhos().isEmpty()) {
//            nome = noId.getFilhos().get(0).getValor();
//            linha = noId.getFilhos().get(0).getLinha();
//        }
//
//        Simbolo s = tabelaSimbolos.buscar(nome);
//        if (s == null) {
//            errosSemanticos.add("Linha " + linha + " -> Erro Semântico: Identificador '" + nome + "' não declarado.");
//        } else {
//            s.setUsada(true);
//        }
//    }

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
