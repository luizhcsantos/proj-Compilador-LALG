package br.unesp.compilerLALG.controller;

import br.unesp.compilerLALG.core.codegen.GeradorCodigo;
import br.unesp.compilerLALG.core.lexer.Lexer;
import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.GramaticaLALG;
import br.unesp.compilerLALG.core.parser.Parser;
import br.unesp.compilerLALG.core.parser.TabelaSintatica;
import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.core.semantic.AnalisadorSemantico;
import br.unesp.compilerLALG.core.semantic.TabelaSimbolos;
import br.unesp.compilerLALG.dto.CompilacaoRequest;
import br.unesp.compilerLALG.dto.CompilacaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompiladorController {

    @PostMapping("/compilar")
    public ResponseEntity<CompilacaoResponse> compilaCodigo(@RequestBody CompilacaoRequest request) {

        CompilacaoResponse response = new CompilacaoResponse();
        List<String> todosOsErros = new ArrayList<>();
        if (request.getCodigo() == null || request.getCodigo().trim().isEmpty()) {
            response.setSucesso(false);
            response.setMensagem("Código não pode ser vazio");
            return ResponseEntity.badRequest().body(response);
        }

        // analise lexica
        Lexer lexer = new Lexer(request.getCodigo());
        List<Token> tokens = lexer.tokenize();
        response.setTokens(tokens);

        if (lexer.temErros()) {
            todosOsErros.addAll(lexer.getErros().stream().map(Throwable::getMessage).toList());
        }

        // analise sintatica
        Parser parser = new Parser(tokens);
        parser.analisar();

        if (parser.temErros()) {
            todosOsErros.addAll(parser.getErros().stream().map(Throwable::getMessage).toList());
        }

        noArvore raiz = parser.getRaizArvore();
        response.setArvoreSintatica(raiz);

        // analsie semantica
        // Só tenta rodar a semântica se a árvore não for completamente nula
        AnalisadorSemantico semantico = new AnalisadorSemantico();
        if (raiz != null) {
            semantico.analisar(raiz);
            response.setTabelaSimbolos(semantico.getTabelaSimbolos().getTodosSimbolos());
            if (semantico.temErros()) {
                todosOsErros.addAll(semantico.getErros().stream().map(Throwable::getMessage).toList());
            }
        }

        response.setErros(todosOsErros); // Anexa todos os erros de uma vez só

        if (todosOsErros.isEmpty()) {
            response.setSucesso(true);
            response.setMensagem("Compilação concluída com sucesso!");
        } else {
            response.setSucesso(false);
            response.setMensagem("Foram encontrados " + todosOsErros.size() + " erros durante a compilação.");
        }

        if (todosOsErros.isEmpty() && raiz != null) {
            GeradorCodigo gerador = new GeradorCodigo(semantico.getTabelaSimbolos());
            gerador.gerar(raiz);
            System.out.println("Codigo MEPA gerado:\n" + gerador.getCodigoGerado());
            response.setCodigoMEPA(gerador.getCodigoGerado());
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/tabela-sintatica")
    public Map<String, Map<String, List<String>>> obterTabelaSintatica() {
        TabelaSintatica ts = GramaticaLALG.criarTabela();
        return ts.getTabela();

    }
}


