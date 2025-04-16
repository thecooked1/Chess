package view;

import model.Clock;
import model.GameState;
import model.PieceColor;

import javax.swing.*;
import java.awt.*;

public class GameInfoPanel extends JPanel {

    private final JLabel whitePlayerLabel;
    private final JLabel blackPlayerLabel;
    private final JLabel whiteClockLabel;
    private final JLabel blackClockLabel;
    private final JLabel statusLabel;
    private final JLabel turnLabel;

    private String whiteName = "White"; // Default names
    private String blackName = "Black";

    public GameInfoPanel() {
        setLayout(new GridLayout(3, 2, 10, 5)); // Rows, Cols, hgap, vgap
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        whitePlayerLabel = new JLabel(whiteName, SwingConstants.CENTER);
        blackPlayerLabel = new JLabel(blackName, SwingConstants.CENTER);
        whiteClockLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        blackClockLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        statusLabel = new JLabel("Game Started", SwingConstants.CENTER);
        turnLabel = new JLabel("Turn: WHITE", SwingConstants.CENTER);

        Font labelFont = new Font("SansSerif", Font.BOLD, 14);
        whitePlayerLabel.setFont(labelFont);
        blackPlayerLabel.setFont(labelFont);
        whiteClockLabel.setFont(labelFont);
        blackClockLabel.setFont(labelFont);
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        turnLabel.setFont(labelFont);

        // Row 1: Player Names
        add(whitePlayerLabel);
        add(blackPlayerLabel);

        // Row 2: Clocks
        add(whiteClockLabel);
        add(blackClockLabel);

        // Row 3: Turn Indicator and Status Message
        add(turnLabel);
        add(statusLabel);
    }

    public void setPlayerNames(String white, String black) {
        this.whiteName = white;
        this.blackName = black;
        whitePlayerLabel.setText(whiteName);
        blackPlayerLabel.setText(blackName);
    }

    // Update display based on GameState
    public void updateInfo(GameState gameState) {
        if (gameState == null) return;

        // Update Clocks
        Clock whiteClock = gameState.getClock(PieceColor.WHITE);
        Clock blackClock = gameState.getClock(PieceColor.BLACK);
        whiteClockLabel.setText(whiteClock.getTime());
        blackClockLabel.setText(blackClock.getTime());
        whiteClockLabel.setForeground(whiteClock.isRunning() ? Color.RED : Color.BLACK);
        blackClockLabel.setForeground(blackClock.isRunning() ? Color.RED : Color.BLACK);


        // Update Turn Indicator
        PieceColor currentTurn = gameState.getCurrentPlayerTurn();
        turnLabel.setText("Turn: " + currentTurn);
        turnLabel.setForeground(currentTurn == PieceColor.WHITE ? Color.DARK_GRAY : Color.BLACK); // Example visual cue

        // Update Status Message
        statusLabel.setText(getGameStatusMessage(gameState.getGameStatus()));

        // Maybe add highlight to current player's name/clock
        whitePlayerLabel.setBorder(currentTurn == PieceColor.WHITE ? BorderFactory.createLineBorder(Color.BLUE, 2) : null);
        blackPlayerLabel.setBorder(currentTurn == PieceColor.BLACK ? BorderFactory.createLineBorder(Color.BLUE, 2) : null);


        revalidate();
        repaint();
    }

    private String getGameStatusMessage(GameState.GameStatus status) {
        switch (status) {
            case ONGOING:
                return "Ongoing";
            case WHITE_WINS_CHECKMATE:
                return whiteName + " wins by Checkmate!";
            case BLACK_WINS_CHECKMATE:
                return blackName + " wins by Checkmate!";
            case WHITE_WINS_TIME:
                return whiteName + " wins on time!";
            case BLACK_WINS_TIME:
                return blackName + " wins on time!";
            case WHITE_WINS_RESIGNATION:
                return whiteName + " wins by Resignation!";
            case BLACK_WINS_RESIGNATION:
                return blackName + " wins by Resignation!";
            case STALEMATE:
                return "Draw by Stalemate";
            case DRAW_FIFTY_MOVE_RULE:
                return "Draw by 50-move rule";
            case DRAW_THREEFOLD_REPETITION:
                return "Draw by Repetition";
            case DRAW_INSUFFICIENT_MATERIAL:
                return "Draw by Insufficient Material";
            case DRAW_AGREEMENT:
                return "Draw by Agreement";
            default:
                return "Unknown Status";
        }
    }

    // Update only the clock display (called by controller's timer)
    public void updateClocks(GameState gameState) {
        if (gameState == null) return;
        Clock whiteClock = gameState.getClock(PieceColor.WHITE);
        Clock blackClock = gameState.getClock(PieceColor.BLACK);
        whiteClockLabel.setText(whiteClock.getTime());
        blackClockLabel.setText(blackClock.getTime());
        whiteClockLabel.setForeground(whiteClock.isRunning() ? Color.RED : Color.BLACK);
        blackClockLabel.setForeground(blackClock.isRunning() ? Color.RED : Color.BLACK);
        // No need to repaint whole panel, labels update themselves
    }

    // Display a game-over message (called by view or controller)
    public void showGameOverDialog(GameState.GameStatus status, ChessView parentFrame) {
        String message = getGameStatusMessage(status);
        String title = "Game Over";
        JOptionPane.showMessageDialog(parentFrame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}