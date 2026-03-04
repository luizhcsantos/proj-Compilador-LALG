package br.unesp.compilerLALG.lexer;

import br.unesp.compilerLALG.exception.CompilerException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Lexer {

    private final String input;
    private int pos = 0;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() throws CompilerException {
        List<Token> tokens = new ArrayList<>();
        int linha = 1;
        int coluna = 1;

        // 1. Ignorar Espaços em branco e atualizar contadores de linha/coluna
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
            // 2. Ignorar Comentários de Múltiplas Linhas { ... }
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
                    throw new CompilerException("Erro Léxico: Comentário de bloco '{' iniciado na coluna "
                            + colunaInicioComentario + " nunca foi fechado.");
                }
                continue;
            }

            // 3. Ignprar Comentários de Linha Única // ...
            if (c == '/' && pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                pos += 2; // Pula os dois caracteres
                coluna += 2;
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    pos++;
                    coluna++;
                }
                continue;
            }

            // 4. Reconhecimento sos tokens via Regex
            String restoDaString = input.substring(pos);
            boolean tokenReconhecido = false;

            for (TipoToken tipo : TipoToken.values()) {
                Matcher matcher = tipo.getPadrao().matcher(restoDaString);

                if (matcher.find()) {
                    String lexema = matcher.group();

                    int colunaInicial = coluna;
                    int colunaFinal = coluna + lexema.length() - 1;

                    tokens.add(new Token(tipo.name(), lexema, String.valueOf(linha), colunaInicial, colunaFinal));

                    pos += lexema.length();
                    coluna += lexema.length();
                    tokenReconhecido = true;
                    break;
                }
            }

            // 5. Tratamento de Erro Léxico
            if (!tokenReconhecido) {
                throw new CompilerException("Erro Léxico: Caractere não reconhecido '" + input.charAt(pos) +
                        "' na linha " + linha + ", coluna " + coluna);
            }
        }
        // Adiciona o token finalizador
        tokens.add(new Token("EOF", "EOF", String.valueOf(linha), coluna, coluna));
        return tokens;

    }
}