import controller.GameController;
import model.*;
import view.*; // Assuming StartMenu is refactored or available

import javax.swing.*;

public class Game {

    public static void main(String[] args) {
        // Use invokeLater to ensure GUI creation happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Option 1: Launch Start Menu first (preferred)
            launchStartMenu();

            // Option 2: Launch directly with default settings (for testing)
            // launchGame("White", "Black", 0, 10, 0); // Example: 10 min game
        });
    }

    // Method to launch the Start Menu
    public static void launchStartMenu() {
        // Assuming StartMenu is refactored to take a callback or handle game launch
        // For now, let's assume StartMenu directly calls launchGame upon start
        StartMenu startMenu = new StartMenu(Game::launchGame); // Pass launchGame as a method reference
        startMenu.run(); // Make the start menu visible
    }


    /**
     * Launches the main chess game window with the specified settings.
     * This method should be called after getting settings from StartMenu.
     * @param whiteName Player name for White
     * @param blackName Player name for Black
     * @param hh Initial hours for clocks
     * @param mm Initial minutes for clocks
     * @param ss Initial seconds for clocks
     */
    public static void launchGame(String whiteName, String blackName, int hh, int mm, int ss) {
        // 1. Create Model components
        GameState gameState = new GameState(hh, mm, ss);
        Board board = new Board(gameState); // Board uses the GameState
        GameLogic gameLogic = new GameLogic(board); // Logic operates on the Board

        // 2. Create View
        // The View needs the Controller, Board, and GameState to display info
        // We create it here but pass the Controller reference in later
        // Let's pass models directly to View constructor for simplicity for now
        ChessView chessView = new ChessView(null, board, gameState); // Controller is null initially


        // 3. Create Controller
        // Controller needs the Model (GameLogic) and View
        GameController gameController = new GameController(gameLogic, chessView);

        // 4. Link View <-> Controller
        // Now set the controller reference in the view
        // This requires adding a setter or modifying the constructor logic.
        // Let's modify ChessView to accept controller in constructor.
        // **Correction:** ChessView constructor already takes Controller. Pass it now.

        // Recreate View with Controller reference
        final ChessView finalChessView = new ChessView(gameController, board, gameState);

        // Set player names and initial settings in the view
        finalChessView.setInitialGameSettings(whiteName, blackName, hh, mm, ss);

        // 5. Initialize View State
        finalChessView.updateView(); // Initial draw

        // 6. Make the View visible
        finalChessView.setVisible(true);
    }
}