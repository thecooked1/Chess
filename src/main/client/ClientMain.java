package main.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ClientMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set Look and Feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(GameController::new);
    }
}