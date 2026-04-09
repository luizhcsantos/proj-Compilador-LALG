package br.unesp.compilerLALG.core.parser.ast;

public class BinOpNode {

    private final noArvoreDTO left;
    private final String op;
    private final noArvoreDTO right;

    public BinOpNode(noArvoreDTO left, String op, noArvoreDTO right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public noArvoreDTO getLeft() {
        return left;
    }

    public String getOp() {
        return op;
    }

    public noArvoreDTO getRight() {
        return right;
    }
}
