package br.unesp.compilerLALG.lexer;

import java.util.List;

public class LexerTest {

    public static void main(String[] args) {
        // Teste 1: O exemplo exato da especificação do projeto
        System.out.println("=== Teste 1: Expressão da Especificação ===");
        testarLexer("3.2 + (2 * 12.01)");

        // Teste 2: Testando espaços extras e números naturais isolados
        System.out.println("\n=== Teste 2: Naturais e Espaços ===");
        testarLexer("  15   / 3 -   2 ");

        // Teste 3: Testando como o compilador reage a um erro léxico
        System.out.println("\n=== Teste 3: Forçando um Erro Léxico ===");
        testarLexer("10 $ 5");
    }

    // Metodo auxiliar para evitar repetição de código
    private static void testarLexer(String input) {
        System.out.println("Analisando a string: \"" + input + "\"");
        Lexer lexer = new Lexer(input);

        try {
            List<Token> tokens = lexer.tokenize();

            // Se o Lexer funcionar, imprime token por token
            for (Token token : tokens) {
                System.out.println(token.toString());
            }
            System.out.println("-> Análise concluída com sucesso.\n");

        } catch (RuntimeException e) {
            // Se o Lexer encontrar um caractere inválido, cai aqui
            System.err.println("-> Falha na compilação: " + e.getMessage() + "\n");
        }
    }
}