package view;

import controller.GameController;
import model.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class GameFrame extends JFrame implements IGameView {

    private GameController controller;
    private BoardPanel boardPanel;
    private JLabel whitePlayerLabel, blackPlayerLabel;
    private JLabel whiteClockLabel, blackClockLabel;
    private JLabel statusLabel; // For messages like "White's Turn", "Check!"
    private JButton newGameButton, quitButton, howToPlayButton;

    private String whitePlayerName = "White";
    private String blackPlayerName = "Black";

    public GameFrame(GameController controller, String whiteName, String blackName) {
        super("Chess");
        this.controller = controller;
        this.whitePlayerName = whiteName;
        this.blackPlayerName = blackName;

        boardPanel = new BoardPanel(controller); // Pass controller to panel

        try {
            Image icon = ImageIO.read(getClass().getResource("/assets/wking.png")); // Example icon
            setIconImage(icon);
        } catch (Exception e) {
            System.err.println("Warning: Could not load frame icon.");
        }

        setupUI();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle close via controller
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.requestQuitGame(); // Ask controller before closing
            }
        });

        pack();
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);
        // setVisible(true); // Controller should make it visible after game setup
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10)); // Add some spacing

        // --- Top Panel (Game Info) ---
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5)); // Rows, Cols, hgap, vgap
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        whitePlayerLabel = new JLabel(whitePlayerName, SwingConstants.CENTER);
        blackPlayerLabel = new JLabel(blackPlayerName, SwingConstants.CENTER);
        whiteClockLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        blackClockLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        statusLabel = new JLabel("Initializing...", SwingConstants.CENTER);
        // Add status label spanning two columns? Or dedicated panel?
        JLabel turnLabel = new JLabel("Status:", SwingConstants.RIGHT); // Label for status

        infoPanel.add(whitePlayerLabel);
        infoPanel.add(blackPlayerLabel);
        infoPanel.add(whiteClockLabel);
        infoPanel.add(blackClockLabel);
        infoPanel.add(turnLabel); // Add label
        infoPanel.add(statusLabel); // Add status field

        add(infoPanel, BorderLayout.NORTH);

        // --- Center Panel (Board) ---
        JPanel boardContainer = new JPanel(new GridBagLayout()); // Use GridBag to center
        boardContainer.add(boardPanel);
        add(boardContainer, BorderLayout.CENTER);


        // --- Bottom Panel (Buttons) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        howToPlayButton = new JButton("How to Play");
        newGameButton = new JButton("New Game");
        quitButton = new JButton("Quit Game");

        howToPlayButton.addActionListener(e -> showInstructions());
        newGameButton.addActionListener(e -> controller.requestNewGame());
        quitButton.addActionListener(e -> controller.requestQuitGame());

        buttonPanel.add(howToPlayButton);
        buttonPanel.add(newGameButton);
        buttonPanel.add(quitButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void showInstructions() {
        JOptionPane.showMessageDialog(this,
                "Select a piece of your color to see its legal moves.\n" +
                        "Click on a piece, then click on a highlighted legal square to move.\n" +
                        "Win by checkmate or if your opponent runs out of time.\n\n" +
                        "(Castling, En Passant, Promotion might require further implementation)",
                "How to Play",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // --- IGameView Implementation ---

    @Override
    public void initializeBoard(Piece[][] boardState) {
        boardPanel.setBoardState(boardState);
        // Optionally set initial status message
        // setStatusMessage("White's Turn");
    }

    @Override
    public void updateBoard(Piece[][] boardState) {
        boardPanel.setBoardState(boardState);
    }

    @Override
    public void updateClock(PlayerColor color, String timeString) {
        SwingUtilities.invokeLater(() -> { // Ensure UI updates on EDT
            if (color == PlayerColor.WHITE) {
                whiteClockLabel.setText(timeString);
            } else {
                blackClockLabel.setText(timeString);
            }
        });
    }

    @Override
    public void showGameOver(GameStatus status, PlayerColor winner) {
        SwingUtilities.invokeLater(() -> {
            String message;
            String title;
            switch (status) {
                case CHECKMATE_WHITE_WINS: message = whitePlayerName + " wins by Checkmate!"; title = "Checkmate!"; break;
                case CHECKMATE_BLACK_WINS: message = blackPlayerName + " wins by Checkmate!"; title = "Checkmate!"; break;
                case STALEMATE: message = "Draw by Stalemate!"; title = "Stalemate!"; break;
                case WHITE_WINS_BY_TIMEOUT: message = whitePlayerName + " wins on time!"; title = "Time Out!"; break;
                case BLACK_WINS_BY_TIMEOUT: message = blackPlayerName + " wins on time!"; title = "Time Out!"; break;
                default: message = "Game Over!"; title = "Game Over"; break; // Should not happen for standard end conditions
            }
            setStatusMessage(message); // Update status label as well
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
            // Controller might handle asking for new game here or based on button clicks
        });
    }

    @Override
    public void highlightSquare(Position pos, Color color) {
        // This was merged into BoardPanel's selectedSquare logic
        if (pos == null) {
            boardPanel.setSelectedSquare(null);
        } else {
            boardPanel.setSelectedSquare(pos);
        }
    }

    @Override
    public void highlightLegalMoves(List<Position> legalTargets) {
        boardPanel.setLegalMoveHighlights(legalTargets);
    }

    @Override
    public void clearHighlights() {
        boardPanel.clearHighlights();
    }

    @Override
    public void setStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    @Override
    public void closeGameWindow() {
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
        });
    }

}