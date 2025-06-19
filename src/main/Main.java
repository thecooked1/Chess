package main;

import main.controller.GameController;
import main.view.StartMenu;
import main.view.GameSettings;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set Look and Feel: " + e.getMessage());
        }
        launchStartMenu();
    }

        public static void launchStartMenu(){

        SwingUtilities.invokeLater(() -> {
            StartMenu startMenu = new StartMenu(gameSettings -> {
                new GameController(gameSettings);

            });
            startMenu.show();
        });
        }
    }
