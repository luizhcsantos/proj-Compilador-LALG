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
//            br.unesp.compilerLALG.core.parser.ast.noArvoreDTO folhaEsq =
//                    new br.unesp.compilerLALG.core.parser.ast.noArvoreDTO("Variável", "a");
//
//            br.unesp.compilerLALG.core.parser.ast.noArvoreDTO folhaDir =
//                    new br.unesp.compilerLALG.core.parser.ast.noArvoreDTO("Número", "10");
//
//            br.unesp.compilerLALG.core.parser.ast.noArvoreDTO raiz =
//                    new br.unesp.compilerLALG.core.parser.ast.noArvoreDTO("Atribuição", ":=");
//
//            // Adiciona as folhas na raiz
//            raiz.addFilhos(folhaEsq);
//            raiz.addFilhos(folhaDir);
//
//            // Agora o Java aceita sem reclamar!
//            response.setArvoreSintatica(raiz);
            response.setArvoreSintatica(parser.getRaizArvore());
        }

        return ResponseEntity.ok(response);


    }
}


