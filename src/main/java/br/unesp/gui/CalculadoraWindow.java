package br.unesp.gui;

import br.unesp.compilerLALG.lexer.Lexer;
import br.unesp.compilerLALG.lexer.Token;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class CalculadoraWindow extends JFrame {

    private final JTextPane editorArea;
    private final JTextArea linhasArea;
    private final DefaultTableModel modeloTabela;
    private final DefaultTableModel modeloTabelaSimbolos;
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
        editorArea = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }
        };
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        editorArea.setText("program teste;\nvar x: int;\nbegin\n   x := 10;\nend.");

        linhasArea = new JTextArea("1");
        linhasArea.setBackground(new Color(240, 240, 240));
        linhasArea.setForeground(Color.GRAY);
        linhasArea.setEditable(false);
        linhasArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        linhasArea.setMargin(new Insets(0, 5, 0, 5));

        editorArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { atualizarEditor(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { atualizarEditor(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        aplicarSyntaxHighlighting();

        // Texto inicial para teste
        editorArea.setText("program soma;\n" +
                "int a, b, soma;\n" +
                "begin\n" +
                "  a := 10;\n" +
                "  b := 15;\n" +
                "  soma := a + b;\n" +
                "end.\n" +
                "   \n" +
                "  ");

        JScrollPane scrollEditor = new JScrollPane(editorArea);
        scrollEditor.setRowHeaderView(linhasArea);
        atualizarEditor();
        painelArquivos.addTab("Arquivo 1", scrollEditor);

        // CRIAR PAINEL INFERIOR (Tabelas e Logs)
        painelInferior = new JTabbedPane();

        // Aba de Análise Sintática (impkementada num futuro próximo...)
        JTextArea sintaticaArea = new JTextArea();
        sintaticaArea.setEditable(false);
        //painelInferior.addTab("Análise Sintática", new JScrollPane(sintaticaArea));

        // Aba de Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setForeground(Color.RED); // Erros ficam em vermelho
        painelInferior.addTab("Logs de compilação", new JScrollPane(logArea));

        // Aba da Tabela de Lexemas
        String[] colunas = {"Lexema", "Token", "Linha", "Coluna Inicial", "Coluna Final"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabelaLexemas = new JTable(modeloTabela);

        painelInferior.addTab("Tabela de lexemas", new JScrollPane(tabelaLexemas));

        String[] colunasSimbolos = {"Símbolo", "Tipo"};
        modeloTabelaSimbolos = new DefaultTableModel(colunasSimbolos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabelaSimbolos = new JTable(modeloTabelaSimbolos);
        painelInferior.addTab("Tabela de Símbolos", new JScrollPane(tabelaSimbolos));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, painelArquivos, painelInferior);
        splitPane.setDividerLocation(350); // Altura onde o divisor começa
        splitPane.setResizeWeight(0.7);    // O editor cresce mais que a tabela se a janela for maximizada
        add(splitPane, BorderLayout.CENTER);

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
            painelInferior.setSelectedIndex(1);

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

    private void atualizarEditor() {

        // Aplica a cor azul nas palavras reservadas
        aplicarSyntaxHighlighting();

        // Calciula e desenha os números de linha
        SwingUtilities.invokeLater(() -> {
            try {
                int totalLinhas = editorArea.getDocument().getDefaultRootElement().getElementCount();

                StringBuilder textoNumeros = new StringBuilder();
                for (int i = 0; i < totalLinhas; i++) {
                    textoNumeros.append(i).append("\n");
                }

                linhasArea.setText(textoNumeros.toString());
            } catch(Exception e) {
                // Ignora erros de sincronia durante a digitação rápida
            }
        });
    }

    private void aplicarSyntaxHighlighting() {
        SwingUtilities.invokeLater(() -> {
            String texto = editorArea.getText();

            texto = texto.replaceAll("\\r", "");

            StyledDocument doc = editorArea.getStyledDocument();

            // Reseta tudo para Preto e sem negrito primeiro
            Style estiloPadrao = editorArea.addStyle("Padrao", null);
            StyleConstants.setForeground(estiloPadrao, Color.BLACK);
            StyleConstants.setBold(estiloPadrao, false);
            doc.setCharacterAttributes(0, texto.length(), estiloPadrao, true);

            // Define o estilo das Palavras Reservadas (Azul e Negrito)
            Style estiloReservada = editorArea.addStyle("Reservada", null);
            StyleConstants.setForeground(estiloReservada, Color.BLUE);
            StyleConstants.setBold(estiloReservada, true);

            // Regex com todas as palavras reservadas da LALG
            String regexPAlavras = "\\b(program|begin|end|procedure|var|if|then|else|while|do|int|boolean|read|write|true|false|div|and|or|not)\\b";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regexPAlavras).matcher(texto);

            // Procura palavras no textp e aplica a cor azul
            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), estiloReservada, false);
            }

        });
    }

    // Metodo público para que o Parser ou a Engine consigam inserir dados na tabela
    public void adicionarSimbolo(String simbolo, String tipo, String categoria) {
        SwingUtilities.invokeLater(() -> {
            modeloTabelaSimbolos.addRow(new Object[]{simbolo, tipo, categoria});
        });
    }

    // Metodo auxiliar para limpar a tabela antes de uma nova compilação
    public void limparTabelaSimbolos() {
        SwingUtilities.invokeLater(() -> {
            modeloTabelaSimbolos.setRowCount(0);
        });
    }
}