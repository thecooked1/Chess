package main;

import main.controller.GameController;
import main.view.StartMenu;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Game {

    public static void main(String[] args) {
        // Set look and feel (optional, makes UI look more native)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set Look and Feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            // Create controller first
            GameController controller = new GameController();

            // Create and run the Start Menu, passing the controller's setup method as a callback
            StartMenu startMenu = new StartMenu(controller::setupGame);
            startMenu.run();
        });
    }
}