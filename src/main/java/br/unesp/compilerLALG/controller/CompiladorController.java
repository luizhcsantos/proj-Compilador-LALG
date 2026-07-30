package br.unesp.compilerLALG.controller;

import br.unesp.compilerLALG.core.lexer.Lexer;
import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.Parser;
import br.unesp.compilerLALG.core.semantic.AnalisadorSemantico;
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

            response.setArvoreSintatica(parser.getRaizArvore());
        }

//        AnalisadorSemantico semantico = new AnalisadorSemantico();
//        semantico.analisar(parser.getRaizArvore());
//        response.setErros(semantico.getErrosSemanticos().stream().toList());
//
//        if (!semantico.getErrosSemanticos().isEmpty()) {
//            // Retornar os erros Semânticos para o frontend
//            for(String erro : semantico.getErrosSemanticos()) {
//                System.out.println(erro);
//            }
//        }

        return ResponseEntity.ok(response);


    }
}


