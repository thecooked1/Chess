package main.client;

import main.view.GameSettings;
import main.view.StartMenu;
import javax.swing.SwingUtilities;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StartMenu startMenu = new StartMenu(gameSettings -> {
                // --- SIMPLIFIED: No longer need to ask "Which player are you?" ---
                String myPlayerName = gameSettings.playerName;
                int totalTimeInSeconds = gameSettings.getTotalSeconds();

                // Create the controller with this player's specific settings.
                new GameController(myPlayerName, totalTimeInSeconds);
            });
            startMenu.show();
        });
    }
}