package br.unesp.compilerLALG;

import br.unesp.compilerLALG.exception.CompilerException;
import br.unesp.compilerLALG.lexer.Lexer;
import br.unesp.compilerLALG.lexer.Token;

import java.util.List;

public class CalculadoraEngine {

    private Lexer lexer;
    public double calculate(String expression) throws CompilerException {
        // 1. Lexer transforma a string em tokens
        // 2. Parser transforma tokens em AST
        // 3. Evaluator calcula o resultado da AST
        // Retorna o valor final


        List<Token> tokens = lexer.tokenize();

        return 0;
    }
}
