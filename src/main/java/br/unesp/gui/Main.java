package br.unesp.gui;

import javax.swing.*;

public class Main {
    static void main() {
        SwingUtilities.invokeLater(() -> {
            CalculadoraWindow window = new CalculadoraWindow();
            window.setVisible(true);
        });
    }
}
