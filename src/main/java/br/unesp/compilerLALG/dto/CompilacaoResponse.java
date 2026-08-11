package br.unesp.compilerLALG.dto;

import br.unesp.compilerLALG.core.lexer.Token;
import br.unesp.compilerLALG.core.parser.ast.noArvore;
import br.unesp.compilerLALG.core.semantic.Simbolo;

import java.util.List;

public class CompilacaoResponse {

    private boolean sucesso;
    private String mensagem;
    private List<Token> tokens;
    private List<String> erros;
    private noArvore arvoreSintatica;
    private List<Simbolo> tabelaSimbolos;
    private List<String> codigoGerado;

    public CompilacaoResponse() {
    }

    public CompilacaoResponse(boolean sucesso, String mensagem, List<Token> tokens, List<String> lsitaErros) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.tokens = tokens;
        this.erros = lsitaErros;
    }

    public boolean isSucesso() { return sucesso; }
    public String getMensagem() { return mensagem; }
    public List<Token> getTokens() { return tokens; }
    public List<String> getErros() { return erros; }
    public noArvore getArvoreSintatica() { return arvoreSintatica; }
    public List<Simbolo> getTabelaSimbolos() { return tabelaSimbolos; }
    public List<String> getCodigoGerado() { return codigoGerado; }

    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public void setTokens(List<Token> tokens) { this.tokens = tokens; }
    public void setErros(List<String> erros) { this.erros = erros; }
    public void setArvoreSintatica(noArvore arvoreSintatica) { this.arvoreSintatica = arvoreSintatica; }
    public void setTabelaSimbolos(List<Simbolo> tabelaSimbolos) { this.tabelaSimbolos = tabelaSimbolos; }
    public void setCodigoGerado(List<String> codigoGerado) { this.codigoGerado = codigoGerado; }
}
