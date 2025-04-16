package view;

import controller.GameController;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChessView extends JFrame {

    private final GameController controller;
    private final BoardPanel boardPanel;
    private final GameInfoPanel gameInfoPanel;
    // Keep model references if needed for direct updates, or get via controller
    private final Board boardModel;
    private final GameState gameStateModel;

    private String whitePlayerName = "White";
    private String blackPlayerName = "Black";
    private int initialH = 0, initialM = 0, initialS = 0;


    public ChessView(GameController controller, Board boardModel, GameState gameStateModel) {
        this.controller = controller;
        this.boardModel = boardModel;
        this.gameStateModel = gameStateModel;

        setTitle("Refactored Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Use EXIT_ON_CLOSE for main window
        setLayout(new BorderLayout(10, 10)); // Gaps between components

        // Create Panels
        gameInfoPanel = new GameInfoPanel();
        boardPanel = new BoardPanel(controller, boardModel); // Pass controller

        // Create Buttons Panel
        JPanel buttonPanel = createButtonPanel();

        // Add panels to frame
        add(gameInfoPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Finalize Frame
        pack(); // Size the frame based on components
        setMinimumSize(getPreferredSize()); // Prevent resizing too small
        setLocationRelativeTo(null); // Center on screen
        // setResizable(false); // Keep resizable for flexibility
    }

    public void setInitialGameSettings(String whiteName, String blackName, int hh, int mm, int ss) {
        this.whitePlayerName = whiteName;
        this.blackPlayerName = blackName;
        this.initialH = hh;
        this.initialM = mm;
        this.initialS = ss;
        gameInfoPanel.setPlayerNames(whiteName, blackName);
        // Clocks are already set in GameState constructor, info panel will read them
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10)); // Center alignment, gaps

        JButton newGameButton = new JButton("New Game");
        JButton resignButton = new JButton("Resign");
        JButton drawButton = new JButton("Offer Draw"); // Simple draw offer for now

        newGameButton.addActionListener(e -> handleNewGame());
        resignButton.addActionListener(e -> handleResign());
        drawButton.addActionListener(e -> handleDrawOffer());

        panel.add(newGameButton);
        panel.add(resignButton);
        // panel.add(drawButton); // Add draw later if needed

        return panel;
    }

    private void handleNewGame() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Start a completely new game?\nCurrent game progress will be lost.",
                "Confirm New Game",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Simplest way: Close current window and launch a new StartMenu instance
            this.dispose(); // Close this game window
            controller.stopTimers(); // Ensure controller stops timers
            SwingUtilities.invokeLater(() -> {
                // Need a way to relaunch the setup...
                // Option 1: Have Game.java handle this.
                // Option 2: Directly call new StartMenu (requires StartMenu to be available/refactored).
                // Let's assume Game.java handles restart for now.
                // We need a mechanism for the main Game class to know restart is needed.
                // For now, just dispose and expect manual restart or modify Game.java later.
                System.out.println("New Game requested. Please restart the application or implement restart logic.");
                // If StartMenu exists and is runnable:
                // new StartMenu().run();
            });
        }
    }

    private void handleResign() {
        if (gameStateModel.isGameOver()) return; // Can't resign if already over

        PieceColor resigningPlayer = gameStateModel.getCurrentPlayerTurn();
        String playerName = (resigningPlayer == PieceColor.WHITE) ? whitePlayerName : blackPlayerName;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                playerName + ", are you sure you want to resign?",
                "Confirm Resignation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            controller.handleResignation(resigningPlayer);
        }
    }

    private void handleDrawOffer() {
        // Basic implementation: Just show message
        // Real implementation needs opponent agreement logic
        JOptionPane.showMessageDialog(this, "Draw offered. (Agreement logic not implemented)", "Draw Offer", JOptionPane.INFORMATION_MESSAGE);
        // controller.handleDrawOffer(gameStateModel.getCurrentPlayerTurn());
    }


    // Called by controller after model updates
    public void updateView() {
        gameInfoPanel.updateInfo(gameStateModel);
        boardPanel.updateBoard(); // Redraw board and pieces

        // Check if game ended and show message dialog if needed
        if (gameStateModel.isGameOver()) {
            // Check if message already shown? Maybe add a flag?
            // For simplicity, show every update if game is over.
            // Disable board input? The mouse listener in BoardPanel already checks isGameOver.
            gameInfoPanel.showGameOverDialog(gameStateModel.getGameStatus(), this);
        }
    }

    // More granular update for clocks only
    public void updateClocks() {
        gameInfoPanel.updateClocks(gameStateModel);
    }

    // Delegate promotion choice request to BoardPanel
    public PieceType getPromotionChoice(PieceColor color) {
        return boardPanel.getPromotionChoice(color);
    }
}