package br.unesp.compilerLALG.controller;

import br.unesp.compilerLALG.core.lexer.Lexer;
import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.Parser;
import br.unesp.compilerLALG.dto.CompilacaoRequest;
import br.unesp.compilerLALG.dto.CompilacaoResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompiladorController {

    @PostMapping("/compilar")
    public CompilacaoResponse compilaCodigo(@RequestBody CompilacaoRequest request) {

        if (request.getCodigo() == null || request.getCodigo().trim().isEmpty()) {
            return new CompilacaoResponse(false,
                    "Código não pode ser vazio", null, null);
        }


        Lexer lexer = new Lexer(request.getCodigo());
        List<Token> tokens = lexer.tokenize();

        if (lexer.temErros()) {
            List<String> mensagensErro = lexer.getErros().stream().
                    map(Throwable::getMessage).toList();
            return new CompilacaoResponse(false,
                    "Foram encontrados " + lexer.getErros().size() +
                            " erros léxicos.", tokens, mensagensErro);
        }

        Parser parser = new Parser(tokens);
        parser.analisar();

        if (parser.temErros()) {
            List<String> mensagensErro = parser.getErros().stream().
                    map(Throwable::getMessage).toList();
            return new CompilacaoResponse(false,
                    "Foram encontrados " + parser.getErros().size() +
                            " erros sintáticos.", tokens, mensagensErro);
        }

        // Devolve a resposta de sucesso
        return new CompilacaoResponse(true,
                "Análise concluída com sucesso!", tokens, null);


    }
}


