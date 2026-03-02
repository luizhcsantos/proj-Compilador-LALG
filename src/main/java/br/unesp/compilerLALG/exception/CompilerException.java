package br.unesp.compilerLALG.exception;

public class CompilerException extends Exception {

    public CompilerException(String message) {
        super(message);
    }

    public CompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class CaractereNaoReconhecidoException extends CompilerException {
        public CaractereNaoReconhecidoException(String caractere) {
            super("Caractere não reconhecido: '" + caractere + "'");
        }
    }

    public static class ParenteseFechadoEsperadoException extends CompilerException {
        public ParenteseFechadoEsperadoException() {
            super("Esperado ')'");
        }
    }

     public static class TokenInesperadoException extends CompilerException {
        public TokenInesperadoException(String token) {
            super("Token inesperado: '" + token + "'");
        }
    }

     public static class DivisaoPorZeroException extends CompilerException {
        public DivisaoPorZeroException() {
            super("Erro de execução: Divisão por zero");
        }
    }


}
