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

    private final JTextArea editorArea;
    private final DefaultTableModel modeloTabela;
    private final JTextArea logArea;
    private final JTabbedPane painelInferior;

    public CalculadoraWindow() {
        setTitle("Compilador LALG");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // BARRA DE MENUS
        JMenuBar menuBar = new JMenuBar();
        JMenu menuArquivo = new JMenu("Arquivo");
        JMenu menuEditar = new JMenu("Editar");
        JMenu menuCompilar = new JMenu("Compilar");

        JMenuItem itemAbrirArquivo = new JMenuItem("Abrir Arquivo");
        JMenuItem itemSalvarArquivo = new JMenuItem("Salvar Arquivo");
        menuArquivo.add(itemAbrirArquivo);
        menuArquivo.add(itemSalvarArquivo);

        JMenuItem itemCompilarLexico = new JMenuItem("Executar Análise Léxica");
        menuCompilar.add(itemCompilarLexico);

        menuBar.add(menuArquivo);
        menuBar.add(menuEditar);
        menuBar.add(menuCompilar);
        setJMenuBar(menuBar);

        // ÁREA DO EDITOR (Parte Superior)
        JTabbedPane painelArquivos = new JTabbedPane();
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        // Texto inicial para teste
        editorArea.setText("program soma;\n" +
                "var a, b, soma : int;\n" +
                "begin\n" +
                "  read(a, b);\n" +
                "  soma := a + b;\n" +
                "  write(soma)\n" +
                "end.\n" +
                "   \n" +
                "  ");

        JScrollPane scrollEditor = new JScrollPane(editorArea);
        painelArquivos.addTab("Arquivo 1", scrollEditor);

        // CRIAR PAINEL INFERIOR (Tabelas e Logs)
        painelInferior = new JTabbedPane();

        // Aba de Análise Sintática (impkementada nu futuro próximo...)
        JTextArea sintaticaArea = new JTextArea();
        sintaticaArea.setEditable(false);
        painelInferior.addTab("Análise Sintática", new JScrollPane(sintaticaArea));

        // Aba de Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setForeground(Color.RED); // Erros ficam em vermelho
        painelInferior.addTab("Logs de compilação", new JScrollPane(logArea));

        // Aba da Tabela de Lexemas
        String[] colunas = {"Lexema", "Token", "Linha", "Coluna Inicial", "Coluna Final"};
        modeloTabela = new DefaultTableModel(colunas, 0);
        JTable tabelaLexemas = new JTable(modeloTabela);
        painelInferior.addTab("Tabela de lexemas", new JScrollPane(tabelaLexemas));

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

        itemAbrirArquivo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirArquivo();
            }
        });

        itemSalvarArquivo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                salvarArquivo();
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
            // Instancia o Lexer
            Lexer lexer = new Lexer(codigoFonte);
            List<Token> tokens = lexer.tokenize();

            // Percorre a lista de tokens e preenche a Tabela
            for (Token t : tokens) {
                // Não é neces´sario mostrar o EOF na tabela para o usuário
                if (t.getToken() != null && !t.getToken().equals("EOF")) {
                    Object[] linhaTabela = {
                            t.getLexema(),
                            "<" + t.getToken() + ">",
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

    private void abrirArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        int resultado = fileChooser.showOpenDialog(this);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            try {
                String caminhoArquivo = fileChooser.getSelectedFile().getAbsolutePath();
                String conteudo = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(caminhoArquivo)));
                editorArea.setText(conteudo);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void salvarArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        int resultado = fileChooser.showSaveDialog(this);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            try {
                String caminhoArquivo = fileChooser.getSelectedFile().getAbsolutePath();
                java.nio.file.Files.write(java.nio.file.Paths.get(caminhoArquivo), editorArea.getText().getBytes());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}