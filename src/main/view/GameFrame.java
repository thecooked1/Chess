package main.view;

import main.controller.GameController; // Requires controller reference
import main.model.Color;
import main.model.PieceType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener; // Forward button actions

public class GameFrame extends JFrame {

    private final ChessBoardPanel chessBoardPanel;
    private final JLabel statusLabel;
    private final JLabel whitePlayerLabel;
    private final JLabel blackPlayerLabel;
    private final JLabel whiteClockLabel;
    private final JLabel blackClockLabel;
    private final JButton newGameButton;
    private final JButton quitButton;
    // Add instruction button if needed

    public GameFrame() {
        super("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Or DISPOSE_ON_CLOSE if StartMenu should reappear
        setLayout(new BorderLayout(10, 10)); // Add gaps

        // Attempt to set application icon
        Image icon = PieceImageLoader.getApplicationIcon();
        if (icon != null) {
            setIconImage(icon);
        }

        // Board Panel (Center)
        chessBoardPanel = new ChessBoardPanel();
        add(chessBoardPanel, BorderLayout.CENTER);

        // Status/Info Panel (North or South)
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5)); // Rows, Cols, hgap, vgap
        whitePlayerLabel = new JLabel("White: Player 1");
        blackPlayerLabel = new JLabel("Black: Player 2");
        whiteClockLabel = new JLabel("Time: 00:00:00");
        blackClockLabel = new JLabel("Time: 00:00:00");
        statusLabel = new JLabel("White's turn");

        // Center align text in labels
        whitePlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackPlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        whiteClockLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackClockLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Make status label span two columns
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);


        infoPanel.add(whitePlayerLabel);
        infoPanel.add(blackPlayerLabel);
        infoPanel.add(whiteClockLabel);
        infoPanel.add(blackClockLabel);
        // Add status spanning two columns below clocks
        // infoPanel.add(statusLabel); // Add empty label for grid alignment? No, use different layout or span.

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoPanel, BorderLayout.CENTER);
        topPanel.add(statusPanel, BorderLayout.SOUTH); // Status below player info/clocks


        add(topPanel, BorderLayout.NORTH);

        // Button Panel (South)
        JPanel buttonPanel = new JPanel(new FlowLayout());
        newGameButton = new JButton("New Game");
        quitButton = new JButton("Quit");
        buttonPanel.add(newGameButton);
        buttonPanel.add(quitButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack(); // Size the frame based on components
        setMinimumSize(getPreferredSize()); // Prevent resizing too small
        setLocationRelativeTo(null); // Center on screen
        // setResizable(false); // Optional: prevent resizing
    }

    public ChessBoardPanel getChessBoardPanel() {
        return chessBoardPanel;
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setPlayerNames(String whiteName, String blackName) {
        whitePlayerLabel.setText("White: " + whiteName);
        blackPlayerLabel.setText("Black: " + blackName);
    }

    public void updateClock(Color color, String time) {
        if (color == Color.WHITE) {
            whiteClockLabel.setText("Time: " + time);
        } else {
            blackClockLabel.setText("Time: " + time);
        }
    }


    // Methods to add listeners (called by Controller)
    public void addNewGameListener(ActionListener listener) {
        newGameButton.addActionListener(listener);
    }

    public void addQuitListener(ActionListener listener) {
        quitButton.addActionListener(listener);
    }

    // Method to display game over messages
    public void showGameOverDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // Method for pawn promotion query
    public PieceType askPromotionChoice() {
        PieceType[] choices = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};
        // Use Icons for a better UI
        ImageIcon[] icons = new ImageIcon[choices.length];
        for(int i=0; i<choices.length; i++){
            Image img = PieceImageLoader.getImage(choices[i], Color.WHITE); // Show white piece icons
            if(img != null) {
                icons[i] = new ImageIcon(img.getScaledInstance(30, 30, Image.SCALE_SMOOTH));
            } else {
                icons[i] = null; // Fallback if image fails
            }
        }

        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose piece for promotion:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, // Use PLAIN_MESSAGE with custom icons
                null, // No parent icon
                icons, // Use the piece icons as options
                icons[0] // Default to Queen icon
        );

        if (choice >= 0 && choice < choices.length) {
            return choices[choice];
        } else {
            return PieceType.QUEEN; // Default to Queen if dialog is closed or invalid choice
        }
    }

}