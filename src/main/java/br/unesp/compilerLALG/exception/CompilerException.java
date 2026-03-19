package br.unesp.compilerLALG.exception;

public class CompilerException extends Exception {

    public CompilerException(String message) {
        super(message);
    }

    public CompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    // Classe base para erros léxicos
    public static class LexicalException extends CompilerException {
        private final int linha;
        private final int coluna;

        public LexicalException(String mensagemEspecifica, int linha, int coluna) {
            super("Erro Léxico: " + mensagemEspecifica + " na linha " + linha + ", coluna " + coluna);
            this.linha = linha;
            this.coluna = coluna;
        }

        public int getLinha() { return linha; }
        public int getColuna() { return coluna; }
    }

    // Herdam de LexicalException para erros específicos

    public static class CaractereNaoReconhecidoException extends LexicalException {
        public CaractereNaoReconhecidoException(char caractere, int linha, int coluna) {
            super("Caractere não reconhecido '" + caractere + "'", linha, coluna);
        }
    }

    public static class ComentarioNaoFechadoException extends LexicalException {
        public ComentarioNaoFechadoException(int linha, int colunaInicio) {
            super("Comentário de bloco '{' nunca foi fechado", linha, colunaInicio);
        }
    }

    public static class LimiteExcedidoException extends LexicalException {
        public LimiteExcedidoException(String tipo, String lexema, int limite, int linha, int coluna) {
            super("O " + tipo + " '" + lexema + "' excede o limite máximo de " + limite + " caracteres/dígitos", linha, coluna);
        }
    }
}