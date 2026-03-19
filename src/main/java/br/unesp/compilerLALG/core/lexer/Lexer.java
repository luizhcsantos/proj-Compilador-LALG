package br.unesp.compilerLALG.core.lexer;

import br.unesp.compilerLALG.exception.CompilerException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Lexer {

    private final String input;
    private int pos = 0;

    // lista para guardar os erros - em vez de parar o programa
    private List<CompilerException.LexicalException> listaErros = new ArrayList<>();

    public Lexer(String input) {
        this.input = input;
    }

    // Metodo para a API consultar se houve erros após a tokenização
    public List<CompilerException.LexicalException> getErros() {
        return listaErros;
    }

    public boolean temErros() {
        return !listaErros.isEmpty();
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        int linha = 1;
        int coluna = 1;

        // Ignorar Espaços em branco e atualizar contadores de linha/coluna
        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (Character.isWhitespace(c)) {
                if (c == '\n') {
                    linha++;
                    coluna = 1;
                } else {
                    coluna++;
                }
                pos++;
                continue;
            }
            //Ignorar Comentários de Múltiplas Linhas { ... }
            if (c == '{') {
                int colunaInicioComentario = coluna;
                pos++;
                coluna++;
                boolean comentarioFechado = false;

                while (pos < input.length()) {
                    char charComentario = input.charAt(pos);
                    if (charComentario == '\n') {
                        linha++;
                        coluna = 1;
                    } else {
                        coluna++;
                    }
                    pos++;

                    if (charComentario == '}') {
                        comentarioFechado = true;
                        break;
                    }
                }
                // Se chegou ao fim do arquivo e não achou a chave '}'
                if (!comentarioFechado) {
                    throw new CompilerException.ComentarioNaoFechadoException(
                            linha, colunaInicioComentario);
                }
                continue;
            }

            // Ignprar Comentários de Linha Única // ...
            if (c == '/' && pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                pos += 2; // Pula os dois caracteres
                coluna += 2;
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    pos++;
                    coluna++;
                }
                continue;
            }

            // Reconhecimento sos tokens via Regex
            String restoDaString = input.substring(pos);
            boolean tokenReconhecido = false;

            for (TipoToken tipo : TipoToken.values()) {
                Matcher matcher = tipo.getPadrao().matcher(restoDaString);

                if (matcher.find()) {
                    String lexema = matcher.group();

                    if (tipo.name().equals("IDENTIFICADOR") && lexema.length() > 10) {
                        throw new CompilerException.LimiteExcedidoException(
                                "identificador", lexema, 10, linha, coluna);
                    }

                    if (tipo.name().equals("NUM") && lexema.length() > 10) {
                        throw new CompilerException.LimiteExcedidoException(
                                "número", lexema, 10, linha, coluna);
                    }

                    if (tipo.name().equals("NUM_REAL")) {
                        String[] partes = lexema.split("\\.");
                        // Verifica a parte inteira
                        if (partes[0].length() > 10) {
                            throw new CompilerException.LimiteExcedidoException(
                                    "parte inteira do número", lexema, 10, linha, coluna);
                        }
                        // Verifica a parte decimal
                        if (partes.length > 1 && partes[1].length() > 10) {
                            throw new CompilerException.LimiteExcedidoException(
                                    "parte decimal do número", lexema, 10, linha, coluna);
                        }
                    }

                    int colunaInicial = coluna;
                    int colunaFinal = coluna + lexema.length() - 1;

                    tokens.add(new Token(tipo.name(), lexema, linha, colunaInicial, colunaFinal));

                    pos += lexema.length();
                    coluna += lexema.length();
                    tokenReconhecido = true;
                    break;
                }
            }

            // Tratamento de Erro Léxico - Caractere Não Reconecido
            if (!tokenReconhecido) {
                // Em vez de 'throw', o erro é add à lista
                listaErros.add(new CompilerException.CaractereNaoReconhecidoException(
                        input.charAt(pos), linha, coluna));

                // Ignora o caractere inválido e força o avanço
                pos++;
                coluna++;
                continue; // Volta para o início do while para analisar o próximo caractere
            }
        }
        // Adiciona o token finalizador
        tokens.add(new Token("EOF", "EOF", linha, coluna, coluna));
        return tokens;

    }
}