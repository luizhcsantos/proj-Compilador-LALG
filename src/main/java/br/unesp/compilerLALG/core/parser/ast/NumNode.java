package br.unesp.compilerLALG.core.parser.ast;

public class NumNode extends ASTnode {

    private final double value;

    public NumNode(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
