package br.unesp.gui;

import br.unesp.compilerLALG.CalculadoraEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CalculadoraWindow extends JFrame implements ActionListener {

    private final JTextField display;
    private CalculadoraEngine engine;

    public CalculadoraWindow() {

        setTitle("Calculadora LALG");
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        display = new JTextField();
        display.setFont(new Font("Arial", Font.BOLD, 24));
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setEditable(false);
        add(display, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 4, 5, 5));

        String[] buttons = {
                "7", "8", "9", "/",
                "4", "5", "6", "*",
                "1", "2", "3", "-",
                "0", ".", "=", "+",
                "C", "(", ")", ""
        };

        for (String text : buttons) {
            JButton button = new JButton(text);
            button.setFont(new Font("Arial", Font.BOLD, 24));
            button.addActionListener(this);
            buttonPanel.add(button);
        }

        add(buttonPanel, BorderLayout.CENTER);
        setLocationRelativeTo(this);


    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals("C")) {
            display.setText("");
        } else if (command.equals("=")) {
            String expression = display.getText();
            System.out.println("Calculando expressão: " + expression);

            engine = new CalculadoraEngine();
            try {
                double result = engine.calculate(expression);
                if (result == (long) result) {
                    display.setText(String.format("%d", (long) result));
                } else {
                    display.setText(String.format("%s", result));
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Erro: " + ex.getMessage(),
                        "Erro de Cálculo", JOptionPane.ERROR_MESSAGE);
                display.setText("");
            }
        }
        else {
            display.setText(display.getText() + command);
        }
    }
}
