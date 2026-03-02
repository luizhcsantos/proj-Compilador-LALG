package br.unesp.compilerLALG;

import br.unesp.compilerLALG.exception.CompilerException;
import br.unesp.compilerLALG.lexer.Lexer;
import br.unesp.compilerLALG.lexer.Token;
import br.unesp.compilerLALG.parser.ast.ASTnode;
import br.unesp.compilerLALG.parser.ast.BinOpNode;
import br.unesp.compilerLALG.parser.ast.NumNode;

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

    public class evaluator {
        public double evaluate() {
            // Implementar a lógica de avaliação da AST aqui
            return 0;
        }

        public double visit(ASTnode node) {
            if (node instanceof NumNode) {
                return ((NumNode) node).getValue();
            }
            else if (node instanceof BinOpNode binOpNode) {
                double leftValue = visit(binOpNode.getLeft());
                double rightValue = visit(binOpNode.getRight());
                return switch (binOpNode.getOp()) {
                    case "+" -> leftValue + rightValue;
                    case "-" -> leftValue - rightValue;
                    case "*" -> leftValue * rightValue;
                    case "/" -> {
                        if (rightValue == 0) {
                            throw new ArithmeticException("Divisão por zero");
                        }
                        yield leftValue / rightValue;
                    }
                    default -> throw new IllegalArgumentException("Operação desconhecida: " + binOpNode.getOp());
                };
            }
            return 0;
        }
    }
}
