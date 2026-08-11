package br.unesp.compilerLALG.controller;

import br.unesp.compilerLALG.core.lexer.Lexer;
import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.Parser;
import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.core.semantic.AnalisadorSemantico;
import br.unesp.compilerLALG.core.semantic.Simbolo;
import br.unesp.compilerLALG.core.semantic.TabelaSimbolos;
import br.unesp.compilerLALG.dto.CompilacaoRequest;
import br.unesp.compilerLALG.dto.CompilacaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompiladorController {

    @PostMapping("/compilar")
    public ResponseEntity<CompilacaoResponse> compilaCodigo(@RequestBody CompilacaoRequest request) {

        CompilacaoResponse response = new CompilacaoResponse();

        if (request.getCodigo() == null || request.getCodigo().trim().isEmpty()) {
            response.setSucesso(false);
            response.setMensagem("Código não pode ser vazio");
            return ResponseEntity.badRequest().body(response);
        }


        Lexer lexer = new Lexer(request.getCodigo());
        List<Token> tokens = lexer.tokenize();
        response.setSucesso(true);



        if (lexer.temErros()) {
            response.setSucesso(false);
            response.setMensagem("Foram encontrados " + lexer.getErros().size() + " erros léxicos.");
            response.setErros(lexer.getErros().stream().map(Throwable::getMessage).toList());
        } else {
            response.setTokens(tokens);
        }

        Parser parser = new Parser(tokens);
        parser.analisar();

        noArvore raiz = parser.getRaizArvore();
        response.setArvoreSintatica(raiz);
//        imprimirArvoreConsole(raiz);

        if (parser.temErros()) {
            response.setSucesso(false);
            response.setMensagem("Foram encontrados " + parser.getErros().size() + " erros sintáticos.");
            response.setErros(parser.getErros().stream().map(Throwable::getMessage).toList());
        } else {
            response.setSucesso(true);
            response.setMensagem("Análise concluída com sucesso!");

            response.setArvoreSintatica(raiz);
        }

        AnalisadorSemantico semantico = new AnalisadorSemantico();
        semantico.analisar(raiz);
        TabelaSimbolos tabela = semantico.getTabelaSimbolos();

//        for (Simbolo tabelaSimbolos : tabela.getTodosSimbolos()) {
//            System.out.println("Nome: " + tabelaSimbolos.getSimbolo() + ", Tipo: " + tabelaSimbolos.getTipo()
//                    + ", Escopo: " + tabelaSimbolos.getNivelLexico() + ", Categoria: " + tabelaSimbolos.getCategoria()
//                    + ", Linha: " + tabelaSimbolos.getLinhaDeclaracao() + ", Usada: " + tabelaSimbolos.isUsada());
//        }

        response.setErros(semantico.getErrosSemanticos().stream().toList());
        response.setTabelaSimbolos(semantico.getTabelaSimbolos().getTodosSimbolos());

        if (!semantico.getErrosSemanticos().isEmpty()) {
            for(String erro : semantico.getErrosSemanticos()) {
                System.out.println(erro);
            }
        }

        return ResponseEntity.ok(response);


    }

    /**
     * Método público para iniciar a impressão da árvore.
     * Chame este método passando o nó Raiz. Ex: imprimirArvoreConsole(parser.getRaizArvore());
     */
    public void imprimirArvoreConsole(noArvore raiz) {
        if (raiz == null) {
            System.out.println("Árvore Sintática está vazia (null).");
            return;
        }
        System.out.println("=== INÍCIO DA ÁRVORE SINTÁTICA ===");

        // Imprime a raiz (sem as linhas de galho)
        String valorRaiz = (raiz.getValor() != null && !raiz.getValor().trim().isEmpty()) ? " ('" + raiz.getValor() + "')" : "";
        System.out.println("📦 " + raiz.getNome() + valorRaiz);

        // Chama a recursão para os filhos
        List<noArvore> filhos = raiz.getFilhos();
        for (int i = 0; i < filhos.size(); i++) {
            boolean isUltimo = (i == filhos.size() - 1);
            imprimirNoRecursivo(filhos.get(i), "", isUltimo);
        }

        System.out.println("=== FIM DA ÁRVORE SINTÁTICA ===");
    }

    /**
     * Método privado que faz a mágica da recursão e desenha os galhos.
     */
    private void imprimirNoRecursivo(noArvore no, String prefixo, boolean isUltimo) {
        if (no == null) return;

        // Desenha o galho atual ("└── " para o último filho, "├── " para os do meio)
        System.out.print(prefixo);
        System.out.print(isUltimo ? "└── " : "├── ");

        // Imprime o Nome e o Valor do nó (se o valor não for vazio)
        String valorStr = (no.getValor() != null && !no.getValor().trim().isEmpty()) ? " -> [ " + no.getValor() + " ]" : "";
        System.out.println(no.getNome() + valorStr);

        // Prepara o espaçamento para a próxima geração (filhos deste nó)
        // Se este nó for o último, os filhos não precisam da linha vertical "│"
        String prefixoFilhos = prefixo + (isUltimo ? "    " : "│   ");

        // Chama recursivamente para todos os filhos deste nó
        List<noArvore> filhos = no.getFilhos();
        if (filhos != null) {
            for (int i = 0; i < filhos.size(); i++) {
                boolean ultimoFilho = (i == filhos.size() - 1);
                imprimirNoRecursivo(filhos.get(i), prefixoFilhos, ultimoFilho);
            }
        }
    }
}


