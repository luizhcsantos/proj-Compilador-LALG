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
                    throw new CompilerException("Erro Léxico: Comentário de bloco '{' iniciado na coluna "
                            + colunaInicioComentario + " nunca foi fechado.");
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
                        throw new CompilerException("Erro Léxico: O identificador '" + lexema +
                                " 'excede o limite máximo de 10 caracteres na linha " + linha);
                    }

                    if (tipo.name().equals("NUM") && lexema.length() > 10) {
                        throw new CompilerException("Erro Léxico: O número '" + lexema +
                                "' excede o limite máximo de 10 dígitos na linha " + linha);
                    }

                    if (tipo.name().equals("NUM_REAL")) {
                        String[] partes = lexema.split("\\.");
                        // Verifica a parte inteira (antes do ponto)
                        if (partes[0].length() > 10) {
                            throw new CompilerException("Erro Léxico: A parte inteira do número '" + lexema +
                                    "' excede 10 dígitos na linha " + linha);
                        }
                        // Verifica a parte decimal (depois do ponto), se existir
                        if (partes.length > 1 && partes[1].length() > 10) {
                            throw new CompilerException("Erro Léxico: A parte decimal do número '" + lexema +
                                    "' excede 10 dígitos na linha " + linha);
                        }
                    }

                    int colunaInicial = coluna;
                    int colunaFinal = coluna + lexema.length() - 1;

                    tokens.add(new Token(tipo.name(), lexema, String.valueOf(linha), colunaInicial, colunaFinal));

                    pos += lexema.length();
                    coluna += lexema.length();
                    tokenReconhecido = true;
                    break;
                }
            }

            // Tratamento de Erro Léxico
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