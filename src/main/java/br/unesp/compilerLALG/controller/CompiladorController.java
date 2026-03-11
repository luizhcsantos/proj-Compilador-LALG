package br.unesp.compilerLALG.controller;

import br.unesp.compilerLALG.core.lexer.Lexer;
import br.unesp.compilerLALG.core.lexer.Token;
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
            return new CompilacaoResponse(false, "Código não pode ser vazio", null);
        }

        try {
            Lexer lexer = new Lexer(request.getCodigo());
            List<Token> tokens = lexer.tokenize();

            // Devolve a resposta de sucesso
            return new CompilacaoResponse(
                    true,
                    "Análise Léxica concluída com sucesso! " + tokens.size() + " tokens encontrados.",
                    tokens
            );

        } catch (Exception ex) {
            return new CompilacaoResponse(
                    false,
                    "FALHA NA COMPILAÇÃO: " + ex.getMessage(),
                    null
            );
        }

    }
}


