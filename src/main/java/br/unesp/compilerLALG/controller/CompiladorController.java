package br.unesp.compilerLALG.controller;

import br.unesp.compilerLALG.core.lexer.Lexer;
import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.Parser;
import br.unesp.compilerLALG.core.parser.ast.noArvoreDTO;
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
            System.out.println("tokens: ");
            for (Token token : tokens) {
                System.out.println(token.getToken());
            }
        }

        Parser parser = new Parser(tokens);
        parser.analisar();

        if (parser.temErros()) {
            response.setSucesso(false);
            response.setMensagem("Foram encontrados " + parser.getErros().size() + " erros sintáticos.");
            response.setErros(parser.getErros().stream().map(Throwable::getMessage).toList());
        } else {
            response.setSucesso(true);
            response.setMensagem("Análise concluída com sucesso!");

            // MOCK TEMPORÁRIO PARA VER A ÁRVORE NO VUE.JS
            // aqui entrará o código real da árvore gerada
            java.util.Map<String, Object> folhaEsq = new java.util.HashMap<>();
            folhaEsq.put("nome", "Variável");
            folhaEsq.put("valor", "a");

            java.util.Map<String, Object> folhaDir = new java.util.HashMap<>();
            folhaDir.put("nome", "Número");
            folhaDir.put("valor", "10");

            java.util.Map<String, Object> raiz = new java.util.HashMap<>();
            raiz.put("nome", "Atribuição");
            raiz.put("valor", ":=");
            raiz.put("filhos", java.util.List.of(folhaEsq, folhaDir));

            // (Você precisa mudar o tipo da variável arvoreSintatica na classe
            // CompilacaoResponse para Object temporariamente para isso funcionar)
            response.setArvoreSintatica((noArvoreDTO) raiz);
        }

        return ResponseEntity.ok(response);


    }
}


