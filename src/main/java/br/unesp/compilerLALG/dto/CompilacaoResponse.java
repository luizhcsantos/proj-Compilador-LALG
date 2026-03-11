package br.unesp.compilerLALG.dto;

import br.unesp.compilerLALG.core.lexer.Token;

import java.util.List;

public class CompilacaoResponse {

    private boolean sucesso;
    private String mensagem;
    private List<Token> tokens;

    public CompilacaoResponse(boolean sucesso, String mensagem, List<Token> tokens) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.tokens = tokens;
    }

    public boolean isSucesso() { return sucesso; }
    public String getMensagem() { return mensagem; }
    public List<Token> getTokens() { return tokens; }

    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public void setTokens(List<Token> tokens) { this.tokens = tokens; }
}
