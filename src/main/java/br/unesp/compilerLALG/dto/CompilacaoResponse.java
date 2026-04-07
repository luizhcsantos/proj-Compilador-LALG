package br.unesp.compilerLALG.dto;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.ASTnode;

import java.util.List;

public class CompilacaoResponse {

    private boolean sucesso;
    private String mensagem;
    private List<Token> tokens;
    private List<String> lsitaErros;
    private ASTnode arvoreSintatica; 

    public CompilacaoResponse(boolean sucesso, String mensagem, List<Token> tokens, List<String> lsitaErros) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.tokens = tokens;
        this.lsitaErros = lsitaErros;
    }

    public boolean isSucesso() { return sucesso; }
    public String getMensagem() { return mensagem; }
    public List<Token> getTokens() { return tokens; }
    public List<String> getLsitaErros() { return lsitaErros; }

    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public void setTokens(List<Token> tokens) { this.tokens = tokens; }
    public void setLsitaErros(List<String> lsitaErros) { this.lsitaErros = lsitaErros; }
}
