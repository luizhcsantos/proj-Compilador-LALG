package br.unesp.compilerLALG;

import br.unesp.compilerLALG.exception.CompilerException;
import br.unesp.compilerLALG.core.lexer.Lexer;
import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.Parser;
import br.unesp.compilerLALG.core.parser.ast.ASTnode;
import br.unesp.compilerLALG.core.parser.ast.BinOpNode;
import br.unesp.compilerLALG.core.parser.ast.NumNode;

import java.util.List;

public class CalculadoraEngine {

    public CalculadoraEngine() {
    }

    public double calculate(String expression) throws CompilerException {
       try {
           // Lexer transforma a string em tokens
           Lexer lexer = new Lexer(expression);
           List<Token> tokens = lexer.tokenize();

           // Parser transforma tokens em AST
           Parser parser = new Parser(tokens);
           //ASTnode astRaiz = parser.parse();

           // Evaluator calcula o resultado da AST
//           evaluator eval = new evaluator();
//
//
//           // Retorna o valor final
//           return eval.visit(astRaiz);
           return 0;
       }catch (RuntimeException e) {
           throw new CompilerException(e.getMessage());
       }

    }

    public static class evaluator {

//        public double visit(ASTnode node) {
//            if (node instanceof NumNode) {
//                return ((NumNode) node).getValue();
//            }
//            else if (node instanceof BinOpNode binOpNode) {
//                double leftValue = visit(binOpNode.getLeft());
//                double rightValue = visit(binOpNode.getRight());
//                System.out.println("Calculando: " + leftValue + " " + binOpNode.getOp() + " " + rightValue);
//                return switch (binOpNode.getOp()) {
//                    case "+" -> leftValue + rightValue;
//                    case "-" -> leftValue - rightValue;
//                    case "*" -> leftValue * rightValue;
//                    case "/" -> {
//                        if (rightValue == 0) {
//                            throw new ArithmeticException("Divisão por zero");
//                        }
//                        yield leftValue / rightValue;
//                    }
//                    default -> throw new IllegalArgumentException("Operação desconhecida: " + binOpNode.getOp());
//                };
//            }
//            return 0;
//        }
    }
}
