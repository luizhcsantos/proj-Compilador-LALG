package br.unesp.gui;

import javax.swing.*;

public class Main {
    static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CalculadoraWindow window = new CalculadoraWindow();
            window.setVisible(true);
        });
    }
}