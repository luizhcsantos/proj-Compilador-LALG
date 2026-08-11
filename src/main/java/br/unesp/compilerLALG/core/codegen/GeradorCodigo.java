package br.unesp.compilerLALG.core.codegen;

import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.core.semantic.Simbolo;
import br.unesp.compilerLALG.core.semantic.TabelaSimbolos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GeradorCodigo {

    private final TabelaSimbolos tabelaSimbolos;
    private final List<String> instrucoesMEPA;
    private int varsGlobaisAlocadas = 0;


    private static final java.util.Set<String> REGRAS_ESTRUTURAIS = java.util.Set.of(
            "identificador", "variável", "variavel", "terminal", "procedure",
            "procedimento", "program", "programa", "int", "real", "boolean", ";",
            "expressão", "expressao", "expressão_simples", "expressao_simples",
            "termo", "fator", "número", "numero", "atribuição", "comando",
            "lista de comandos", "comando composto", "bloco"
    );

    public GeradorCodigo(TabelaSimbolos tabelaSimbolos) {
        this.tabelaSimbolos = tabelaSimbolos;
        this.instrucoesMEPA = new ArrayList<>();
    }

    public List<String> gerar(noArvore raiz) {
        instrucoesMEPA.add("INPP"); // Inicia o Programa Principal

        visitar(raiz);

        // Desaloca a memória usada pelas variáveis globais no fim do programa
        if (varsGlobaisAlocadas > 0) {
            instrucoesMEPA.add("DMEM " + varsGlobaisAlocadas);
        }
        instrucoesMEPA.add("PARA"); // Termina a máquina

        return instrucoesMEPA;
    }

    private void visitar(noArvore no) {
        if (no == null) return;
        String nomeNo = no.getNome().toLowerCase();

        switch (nomeNo) {
            case "declaração variáveis":
                int qtd = contarVariaveis(no);
                if (qtd > 0) {
                    instrucoesMEPA.add("AMEM " + qtd);
                    varsGlobaisAlocadas += qtd;
                }
                return;

            case "atribuição":
                gerarAtribuicao(no);
                return;

            case "chamada de procedimento":
            case "comando": // O read/write pode estar encapsulado como comando
                if (verificarReadWrite(no)) return;
                break;
        }

        // Continua a descer na árvore
        for (noArvore filho : no.getFilhos()) {
            visitar(filho);
        }
    }

    // --- MÉTODOS DE GERAÇÃO ESPECÍFICOS ---

    private void gerarAtribuicao(noArvore no) {
        if (no.getFilhos().size() < 2) return;

        //O lado esquerdo é a variável que vai receber o valor
        noArvore noEsq = no.getFilhos().get(0);
        String nomeVar = extrairNomeLexema(noEsq);

        // O lado direito é a expressão matemática a ser resolvida
        noArvore noDir = no.getFilhos().get(no.getFilhos().size() - 1);

        // Resolve a matemática (isto vai deixar o resultado no topo da pilha da MEPA)
        gerarExpressao(noDir);

        // 3. Descobre onde a variável mora na memória e guarda lá o resultado (ARMZ)
        Simbolo sim = tabelaSimbolos.buscar(nomeVar);
        if (sim != null) {
            instrucoesMEPA.add("ARMZ " + sim.getDeslocamento());
        }
    }

    private boolean verificarReadWrite(noArvore no) {
        String nomeProcedimento = extrairNomeLexema(no);

        if (nomeProcedimento.equalsIgnoreCase("read")) {
            noArvore listaExp = buscarNoPorNome(no, "lista de expressões");
            if (listaExp != null) {
                // Para cada variável dentro do read(a, b)
                for (noArvore varNo : listaExp.getFilhos()) {
                    String nomeVar = extrairNomeLexema(varNo);
                    Simbolo sim = tabelaSimbolos.buscar(nomeVar);
                    if (sim != null && !nomeVar.isEmpty()) {
                        instrucoesMEPA.add("LEIT"); // Lê do teclado e põe no topo
                        instrucoesMEPA.add("ARMZ " + sim.getDeslocamento()); // Guarda na variável
                    }
                }
            }
            return true;
        }
        else if (nomeProcedimento.equalsIgnoreCase("write")) {
            noArvore listaExp = buscarNoPorNome(no, "lista de expressões");
            if (listaExp != null) {
                // Para cada conta/variável dentro do write(a + b)
                for (noArvore expNo : listaExp.getFilhos()) {
                    if (!expNo.getNome().equals(",")) {
                        gerarExpressao(expNo); // Resolve a conta e deixa no topo
                        instrucoesMEPA.add("IMPR"); // Imprime o topo
                    }
                }
            }
            return true;
        }
        return false;
    }

    // --- TRAVESSIA RECURSIVA PÓS-ORDEM ---
    private void gerarExpressao(noArvore no) {
        if (no == null) return;

        String nome = no.getNome().toLowerCase().trim();
        String lexema = extrairNomeLexema(no);

        // Caso Base: Se for um Número Puro (Gera CRCT)
        if (nome.equals("número") || nome.equals("numero") || isNumeric(lexema)) {
            if (!lexema.isEmpty() && !isOperadorLexema(null, lexema)) {
                instrucoesMEPA.add("CRCT " + lexema);
                return;
            }
        }

        // Caso Base: Se for uma Variável (Gera CRVL)
        if (nome.equals("identificador") || nome.equals("variável") || nome.equals("variavel")) {
            if (!lexema.isEmpty() && !isOperadorLexema(null, lexema)) {
                Simbolo sim = tabelaSimbolos.buscar(lexema);
                if (sim != null) {
                    instrucoesMEPA.add("CRVL " + sim.getDeslocamento());
                }
                return;
            }
        }

        // Se este nó for um Operador Matemático isolado
        if (isOperadorLexema(null, lexema)) {
            // Se a árvore guardar o operador no próprio nó atual
            List<noArvore> filhos = no.getFilhos();
            if (filhos != null && filhos.size() >= 2) {
                gerarExpressao(filhos.get(0)); // Esquerda
                gerarExpressao(filhos.get(1)); // Direita
            }
            gerarInstrucaoOperador(no);
            return;
        }

        // Se for um nó composto (Expressão, Termo, Fator, Atribuição)
        List<noArvore> filhos = no.getFilhos();
        if (filhos == null || filhos.isEmpty()) return;

        // Se tiver exatamente 3 filhos estruturais (ex: [Esquerda, Operador, Direita])
        if (filhos.size() == 3) {
            noArvore esq = filhos.get(0);
            noArvore op = filhos.get(1);
            noArvore dir = filhos.get(2);

            String aux = extrairNomeLexema(op);

            if (isOperador(op) || isOperadorLexema(null, aux)) {
                gerarExpressao(esq); // Gera código para a esquerda
                gerarExpressao(dir); // Gera código para a direita
                gerarInstrucaoOperador(op); // Emite a operação
                return;
            }
        }

        // Caso geral: Varre todos os filhos recursivamente acumulando a expressão
        for (noArvore filho : filhos) {
            // Se o filho for diretamente um operador, é tratado de forma especial
            if (isOperador(filho)) {
                gerarInstrucaoOperador(filho);
            } else {
                gerarExpressao(filho);
            }
        }
    }

    // Traduz Folhas em Código (Carrega os dados para a Pilha)
    private void gerarInstrucaoParaTermo(noArvore termo) {
        String lexema = extrairNomeLexema(termo);

        if (isNumeric(lexema)) {
            instrucoesMEPA.add("CRCT " + lexema);
        } else {
            // Se for variável
            Simbolo sim = tabelaSimbolos.buscar(lexema);
            if (sim != null) {
                instrucoesMEPA.add("CRVL " + sim.getDeslocamento());
            }
        }
    }

    // Escava a Árvore e devolve uma lista pura (sem nós "expressão", "termo", etc)
    private List<noArvore> achatarExpressao(noArvore no) {
        List<noArvore> listaPlana = new ArrayList<>();
        if (no == null) return listaPlana;

        String lexema = extrairNomeLexema(no);

        // Se este nó já é um dado final (numero, var ou operador)
        if (isNumeric(lexema) || isOperadorLexema(null, lexema) || (!lexema.isEmpty() && !REGRAS_ESTRUTURAIS.contains(lexema.toLowerCase()))) {

            if (!lexema.equals("(") && !lexema.equals(")")) {
                listaPlana.add(no);
            }
        }

        for (noArvore filho : no.getFilhos()) {
            listaPlana.addAll(achatarExpressao(filho));
        }

        return listaPlana;
    }

    private void gerarInstrucaoOperador(noArvore no) {
        String nome = no.getNome().trim().toLowerCase();
        String valor = no.getValor() != null ? no.getValor().trim().toLowerCase() : "";
        String op = isOp(valor) ? valor : nome;

        switch (op) {
            case "+": instrucoesMEPA.add("SOMA"); break;
            case "-": instrucoesMEPA.add("SUBT"); break;
            case "*": instrucoesMEPA.add("MULT"); break;
            case "/":
            case "div": instrucoesMEPA.add("DIVI"); break;
        }
    }


    private int contarVariaveis(noArvore no) {
        int count = 0;
        if (no.getNome().equalsIgnoreCase("identificador") || no.getNome().equalsIgnoreCase("variável")) {
            if (!extrairNomeLexema(no).isEmpty()) count++;
        }
        for (noArvore f : no.getFilhos()) {
            count += contarVariaveis(f);
        }
        return count;
    }

    private String extrairNomeLexema(noArvore no) {
        if (no == null) return "";

        // usamos se o valor estiver no nó atual
        String val = no.getValor();
        if (val != null && !val.trim().isEmpty() && !REGRAS_ESTRUTURAIS.contains(val.trim().toLowerCase())) {
            return val.trim();
        }

        // se for um nó pai, varremos os filhos
        if (no.getFilhos() != null) {
            for (noArvore f : no.getFilhos()) {
                String lex = extrairNomeLexema(f);
                if (!lex.isEmpty()) return lex;
            }
        }

        // Fallback para nomes (se o Parser jogou o dado no campo Nome)
        String nome = no.getNome();
        if (nome != null && !nome.trim().isEmpty() && !REGRAS_ESTRUTURAIS.contains(nome.trim().toLowerCase())) {
            return nome.trim();
        }

        return "";
    }

    private noArvore buscarNoPorNome(noArvore no, String alvo) {
        if (no.getNome().equalsIgnoreCase(alvo)) return no;
        for (noArvore f : no.getFilhos()) {
            noArvore achou = buscarNoPorNome(f, alvo);
            if (achou != null) return achou;
        }
        return null;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private boolean isOperadorLexema(noArvore no, String op) {
        if (op == null) return false;
        String limpo = op.trim().toLowerCase();
        return limpo.equals("+") || limpo.equals("-") || limpo.equals("*") || limpo.equals("/") || limpo.equals("div");
    }

    private boolean isOp(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/") || s.equals("div");
    }

    private boolean isOperador(noArvore no) {
        if (no == null) return false;
        String nome = no.getNome().trim().toLowerCase();
        String valor = no.getValor() != null ? no.getValor().trim().toLowerCase() : "";
        return isOp(nome) || isOp(valor);
    }

}