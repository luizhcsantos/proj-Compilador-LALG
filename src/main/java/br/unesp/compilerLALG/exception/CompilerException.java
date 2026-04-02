package br.unesp.compilerLALG.exception;

public class CompilerException extends RuntimeException {

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

    // Classe base para erros léxicos
    public static class SyntaxException extends CompilerException {
        private final int linha;
        private final int coluna;

        public SyntaxException(String mensagemEspecifica, int linha, int coluna) {
            super("Erro Sintático: " + mensagemEspecifica + " na linha " + linha + ", coluna " + coluna);
            this.linha = linha;
            this.coluna = coluna;
        }

        public int getLinha() { return linha; }
        public int getColuna() { return coluna; }
    }

    // Erro do match() -> "Esperava X, encontrou Y"
    public static class TokenInesperadoException extends SyntaxException {
        public TokenInesperadoException(String tipoEsperado, String tokenEncontrado, String lexema, int linha, int coluna) {
            super("Esperava o token [" + tipoEsperado + "], mas encontrou [" + tokenEncontrado + "] ('" + lexema + "')", linha, coluna);
        }
    }

    // Erro para quando um comando começa de forma inválida
    public static class ComandoInvalidoException extends SyntaxException {
        public ComandoInvalidoException(String lexema, int linha, int coluna) {
            super("Comando inválido ou não reconhecido: '" + lexema + "'", linha, coluna);
        }
    }

    // Erro para quando há código sobrando após o "end." do programa
    public static class CodigoExtraException extends SyntaxException {
        public CodigoExtraException(String lexema, int linha, int coluna) {
            super("Código extra encontrado após o fim do programa. Token: '" + lexema + "'", linha, coluna);
        }
    }

}