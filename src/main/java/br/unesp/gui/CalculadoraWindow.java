package br.unesp.gui;

import br.unesp.compilerLALG.lexer.Lexer;
import br.unesp.compilerLALG.lexer.Token;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class CalculadoraWindow extends JFrame {

    private JTextArea editorArea;
    private JTable tabelaLexemas;
    private DefaultTableModel modeloTabela;
    private JTextArea logArea;
    private JTextArea sintaticaArea;
    private JTabbedPane painelInferior;

    public CalculadoraWindow() {
        setTitle("IDE Compilador LALG");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. CRIAR BARRA DE MENUS
        JMenuBar menuBar = new JMenuBar();
        JMenu menuArquivo = new JMenu("Arquivo");
        JMenu menuEditar = new JMenu("Editar");
        JMenu menuCompilar = new JMenu("Compilar");

        JMenuItem itemCompilarLexico = new JMenuItem("Executar Análise Léxica");
        menuCompilar.add(itemCompilarLexico);

        menuBar.add(menuArquivo);
        menuBar.add(menuEditar);
        menuBar.add(menuCompilar);
        setJMenuBar(menuBar);

        // 2. CRIAR ÁREA DO EDITOR (Parte Superior)
        JTabbedPane painelArquivos = new JTabbedPane();
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        // Texto inicial para teste baseado na sua imagem
        editorArea.setText("program soma;\n" +
                "var a, b, soma : int;\n" +
                "begin\n" +
                "  read(a, b);\n" +
                "  soma := a + b;\n" +
                "  write(soma)\n" +
                "end.\n" +
                "   \n" +
                "  ");

        // JScrollPane adiciona barras de rolagem ao editor
        JScrollPane scrollEditor = new JScrollPane(editorArea);
        painelArquivos.addTab("Arquivo 1", scrollEditor);

        // 3. CRIAR PAINEL INFERIOR (Tabelas e Logs)
        painelInferior = new JTabbedPane();

        // 3.1 Aba de Análise Sintática (vazia por enquanto)
        sintaticaArea = new JTextArea();
        sintaticaArea.setEditable(false);
        painelInferior.addTab("Análise Sintática", new JScrollPane(sintaticaArea));

        // 3.2 Aba de Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setForeground(Color.RED); // Erros ficam em vermelho
        painelInferior.addTab("Logs de compilação", new JScrollPane(logArea));

        // 3.3 Aba da Tabela de Lexemas
        String[] colunas = {"Lexema", "Token", "Linha", "Coluna Inicial", "Coluna Final"};
        modeloTabela = new DefaultTableModel(colunas, 0);
        tabelaLexemas = new JTable(modeloTabela);
        painelInferior.addTab("Tabela de lexemas", new JScrollPane(tabelaLexemas));

        // 4. DIVISOR DE TELA (JSplitPane) - Separa o editor em cima e a tabela embaixo
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, painelArquivos, painelInferior);
        splitPane.setDividerLocation(350); // Altura onde o divisor começa
        splitPane.setResizeWeight(0.7);    // O editor cresce mais que a tabela se a janela for maximizada
        add(splitPane, BorderLayout.CENTER);

        // 5. AÇÃO DE COMPILAR (O que acontece quando clica no menu)
        itemCompilarLexico.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executarAnaliseLexica();
            }
        });

        setLocationRelativeTo(null); // Centraliza a janela
    }

    private void executarAnaliseLexica() {
        // Limpa as tabelas e logs antigos antes de rodar de novo
        modeloTabela.setRowCount(0);
        logArea.setText("");
        logArea.setForeground(Color.BLACK);

        String codigoFonte = editorArea.getText();

        if (codigoFonte.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "O editor está vazio!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Instancia o Lexer que criamos com o código do editor
            Lexer lexer = new Lexer(codigoFonte);
            List<Token> tokens = lexer.tokenize();

            // Percorre a lista de tokens e preenche a Tabela (JTable)
            for (Token t : tokens) {
                // Não precisamos mostrar o EOF na tabela para o usuário
                if (t.getToken() != null && !t.getToken().equals("EOF")) {
                    Object[] linhaTabela = {
                            t.getLexema(),
                            t.getToken(),
                            t.getLinha(),
                            t.getColunaInicial(),
                            t.getColunaFinal()
                    };
                    modeloTabela.addRow(linhaTabela);
                }
            }

            logArea.setForeground(new Color(0, 153, 0)); // Verde para sucesso
            logArea.setText("Análise Léxica concluída com sucesso!\n" + tokens.size() + " tokens encontrados.");

            // Foca automaticamente na aba da Tabela de Lexemas para o usuário ver o resultado
            painelInferior.setSelectedIndex(2);

        } catch (Exception ex) {
            // Se o Lexer lançar um erro (ex: caractere não reconhecido), mostra no Log
            logArea.setForeground(Color.RED);
            logArea.setText("FALHA NA COMPILAÇÃO:\n\n" + ex.getMessage());

            // Foca automaticamente na aba de Logs para o usuário ler o erro
            painelInferior.setSelectedIndex(1);
        }
    }
}