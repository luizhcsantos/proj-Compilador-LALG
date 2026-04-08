package br.unesp.compilerLALG.core.parser.ast;

public class BinOpNode {

    private final ASTnode left;
    private final String op;
    private final ASTnode right;

    public BinOpNode(ASTnode left, String op, ASTnode right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public ASTnode getLeft() {
        return left;
    }

    public String getOp() {
        return op;
    }

    public ASTnode getRight() {
        return right;
    }
}
