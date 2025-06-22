// main/view/GameFrame.java

package main.view;

import main.model.pieces.*;
import main.common.Colour;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Map;

public class GameFrame extends JFrame {


    private final ChessBoardPanel chessBoardPanel;
    private final JLabel statusLabel;
    private final JLabel whitePlayerLabel;
    private final JLabel blackPlayerLabel;
    private final JLabel whiteClockLabel;
    private final JLabel blackClockLabel;
    private final JButton quitButton;
    private final JMenuItem newGameMenuItem;


    // PGN and Replay components
    private final JMenuItem loadPgnMenuItem;
    // --- PGN CHANGE 1: Added a menu item for saving the PGN ---
    private final JMenuItem savePgnMenuItem;
    private final JButton nextMoveButton;
    private final JButton prevMoveButton;
    private final JButton readyButton;

    public GameFrame() {
        super("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Set application icon
        Image icon = PieceImageLoader.getApplicationIcon();
        if (icon != null) { setIconImage(icon); }

        // --- Create Menu Bar ---
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        newGameMenuItem = new JMenuItem("New Game");
        loadPgnMenuItem = new JMenuItem("Load PGN...");
        // --- PGN CHANGE 2: Initialize and add the save menu item ---
        savePgnMenuItem = new JMenuItem("Save PGN...");
        savePgnMenuItem.setEnabled(false); // Disabled until a game is over

        fileMenu.add(newGameMenuItem);
        fileMenu.add(loadPgnMenuItem);
        fileMenu.add(savePgnMenuItem); // Add to menu
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // --- Board Panel (Center) ---
        chessBoardPanel = new ChessBoardPanel(); // Initialization
        add(chessBoardPanel, BorderLayout.CENTER);

        // --- Status/Info Panel (North) ---
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5)); // 3 rows, 2 columns
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        whitePlayerLabel = new JLabel("White: Player 1");
        blackPlayerLabel = new JLabel("Black: Player 2");
        whiteClockLabel = new JLabel("Time: 00:00:00");
        blackClockLabel = new JLabel("Time: 00:00:00");
        statusLabel = new JLabel("White's turn to move.");

        whitePlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackPlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        whiteClockLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackClockLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));

        infoPanel.add(whitePlayerLabel);
        infoPanel.add(blackPlayerLabel);
        infoPanel.add(whiteClockLabel);
        infoPanel.add(blackClockLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoPanel, BorderLayout.CENTER);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // --- Button Panel (South) ---
        JPanel buttonPanel = new JPanel(new FlowLayout());
        readyButton = new JButton("Ready");
        nextMoveButton = new JButton("Next >");
        prevMoveButton = new JButton("< Prev");
        quitButton = new JButton("Quit");

        nextMoveButton.setEnabled(false);
        prevMoveButton.setEnabled(false);

        buttonPanel.add(readyButton);
        buttonPanel.add(prevMoveButton);
        buttonPanel.add(nextMoveButton);
        buttonPanel.add(quitButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(null);
        pack();
    }

    public JButton getReadyButton() {
        return readyButton;
    }
    public void addReadyListener(ActionListener listener) {
        readyButton.addActionListener(listener);
    }


    public JMenuItem getNewGameMenuItem() {
        return newGameMenuItem;
    }

    public ChessBoardPanel getChessBoardPanel() { return chessBoardPanel; }
    public JMenuItem getLoadPgnMenuItem() { return loadPgnMenuItem; }
    public JButton getNextMoveButton() { return nextMoveButton; }
    public JButton getPrevMoveButton() { return prevMoveButton; }
    // --- PGN CHANGE 3: Getter and listener for the save menu item ---
    public JMenuItem getSavePgnMenuItem() { return savePgnMenuItem; }
    public void addSavePgnListener(ActionListener listener) {
        savePgnMenuItem.addActionListener(listener);
    }


    public void setStatus(String text) { statusLabel.setText(text); }
    public void updatePlayerInfo(Map<String, String> headers) {
        whitePlayerLabel.setText("White: " + headers.getOrDefault("White", "Unknown"));
        blackPlayerLabel.setText("Black: " + headers.getOrDefault("Black", "Unknown"));
    }
    public void enableReplayControls(boolean enabled) {
        nextMoveButton.setEnabled(enabled);
        prevMoveButton.setEnabled(enabled);
    }
    public void updateClock(Colour colour, String time) {
        if (colour == Colour.WHITE) {
            whiteClockLabel.setText("Time: " + time);
        } else {
            blackClockLabel.setText("Time: " + time);
        }
    }

    // --- Listener registration methods ---
    public void addQuitListener(ActionListener listener) {
        quitButton.addActionListener(listener);
    }

    // --- Dialogs ---
    public void showGameOverDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public String askPromotionChoice() {
        Piece[] promotionPieces = { new Queen(Colour.WHITE), new Rook(Colour.WHITE), new Bishop(Colour.WHITE), new Knight(Colour.WHITE) };
        ImageIcon[] icons = new ImageIcon[promotionPieces.length];
        for (int i = 0; i < promotionPieces.length; i++) {
            Image img = PieceImageLoader.getImage(promotionPieces[i]);
            if (img != null) {
                icons[i] = new ImageIcon(img.getScaledInstance(40, 40, Image.SCALE_SMOOTH));
            }
        }
        int choice = JOptionPane.showOptionDialog(this, "Choose piece for promotion:", "Pawn Promotion", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, icons, icons[0]);
        if (choice >= 0 && choice < promotionPieces.length) {
            return String.valueOf(promotionPieces[choice].getSymbol()).toUpperCase();
        }
        return "Q"; // Default to Queen
    }
}