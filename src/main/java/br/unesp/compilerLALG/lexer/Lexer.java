package br.unesp.compilerLALG.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final String input;
    private int pos = 0;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        int estado = 0;
        int linha = 1;
        int colunaAtual = 1;

        StringBuilder lexemaAtual = new StringBuilder();
        while (pos < input.length()) {
            char charAtual = input.charAt(pos);

            switch (estado) {
                case 0:
                    // Espaço em branco - ignorar
                    if (Character.isWhitespace(charAtual)) {
                        pos++;
                        break;
                    }
                    // Digito - iniciar construção de número
                    if (Character.isDigit(charAtual)) {
                        estado = 1;
                        lexemaAtual.append(charAtual);
                        pos++;
                        colunaAtual++;
                        break;
                    }
                    if (charAtual == '+') {
                        tokens.add(new Token(TipoToken.OPSOMA.name(), "+", String.valueOf(linha), colunaAtual, colunaAtual));
                        pos++;
                        colunaAtual++;
                    } else if (charAtual == '-') {
                        tokens.add(new Token(TipoToken.OPSUB.name(), "-", String.valueOf(linha), colunaAtual, colunaAtual));
                        pos++;
                        colunaAtual++;
                    } else if (charAtual == '*') {
                        tokens.add(new Token(TipoToken.OPMUL.name(), "*", String.valueOf(linha), colunaAtual, colunaAtual));
                        pos++;
                        colunaAtual++;
                    } else if (charAtual == '/') {
                        tokens.add(new Token(TipoToken.OPDIV.name(), "/", String.valueOf(linha), colunaAtual, colunaAtual));
                        pos++;
                        colunaAtual++;
                    } else if (charAtual == '(') {
                        tokens.add(new Token(TipoToken.AP.name(), "(", String.valueOf(linha), colunaAtual, colunaAtual));
                        pos++;
                        colunaAtual++;
                    } else if (charAtual == ')') {
                        tokens.add(new Token(TipoToken.FP.name(), ")", String.valueOf(linha), colunaAtual, colunaAtual));
                        pos++;
                        colunaAtual++;
                    } else {
                        throw new RuntimeException("Erro Léxico: Caractere não reconhecido '" + charAtual + "' na posição " + pos);
                    }

                    break;
                case 1:
                    // Estado de montagem da parte inteira do número:
                    // Se for dígito, acumula em 'lexemaAtual' e avança 'pos'.
                    // Se encontrar um '.', muda para o estado 2 (parte decimal), acumula e avança.
                    // Se for qualquer outra coisa, significa que o número acabou.
                    // Cria o token NUM com o valor de 'lexemaAtual', adicione à lista,
                    // limpa o 'lexemaAtual', volta o 'estado' para 0, mas NÃO avança o 'pos'.
                    if (Character.isDigit(charAtual)) {
                        lexemaAtual.append(charAtual);
                        pos++;
                        colunaAtual++;
                    } else if (charAtual == '.') {
                        estado = 2;
                        lexemaAtual.append(charAtual);
                        pos++;
                        colunaAtual++;
                    } else {
                        tokens.add(new Token(TipoToken.NUM.name(), lexemaAtual.toString(), String.valueOf(linha),
                                colunaAtual - lexemaAtual.length(),
                                colunaAtual - 1));
                        lexemaAtual.setLength(0); // Limpa o StringBuilder
                        estado = 0; // Volta para o estado inicial
                    }
                    break;
                case 2:
                    // Estado de montagem da parte decimal do número
                    // Se encontrar outro '.', deve lançar um Erro Léxico (ex: 3.14.15).
                    if (Character.isDigit(charAtual)) {
                        lexemaAtual.append(charAtual);
                        pos++;
                        colunaAtual++;
                    } else if (charAtual == '.') {
                        throw new RuntimeException("Erro Léxico: Número com mais de um ponto decimal '" + lexemaAtual.toString() + charAtual + "' na posição " + pos);
                    } else {
                        tokens.add(new Token(TipoToken.NUM.name(), lexemaAtual.toString(), String.valueOf(linha),
                                colunaAtual - lexemaAtual.length(),
                                colunaAtual - 1));
                        lexemaAtual.setLength(0); // Limpa o StringBuilder
                        estado = 0; // Volta para o estado inicial
                    }
                    break;
            }
        }

        if (estado == 1 || estado == 2) {
            tokens.add(new Token(TipoToken.NUM.name(),
                    lexemaAtual.toString(), String.valueOf(linha),
                    colunaAtual - lexemaAtual.length(),
                    colunaAtual - 1));
        }
        // Adiciona o token finalizador
        tokens.add(new Token(TipoToken.EOF, ""));
        return tokens;
    }
}