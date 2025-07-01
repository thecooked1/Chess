// main/view/GameFrame.java
package com.chess.view;

import com.chess.common.Colour;
import com.chess.model.pieces.*;
import com.chess.model.pieces.*;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
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
    private final JMenuItem loadPgnMenuItem;
    private final JMenuItem savePgnMenuItem;
    private final JButton readyButton;

    // --- MOVE LOG: Add a JTextArea for the move log ---
    private final JTextArea moveLogArea;

    public GameFrame() {
        super("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        Image icon = PieceImageLoader.getApplicationIcon();
        if (icon != null) { setIconImage(icon); }

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        newGameMenuItem = new JMenuItem("New Game");
        loadPgnMenuItem = new JMenuItem("Load PGN...");
        savePgnMenuItem = new JMenuItem("Save PGN...");
        savePgnMenuItem.setEnabled(false);
        fileMenu.add(newGameMenuItem);
        fileMenu.add(loadPgnMenuItem);
        fileMenu.add(savePgnMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        chessBoardPanel = new ChessBoardPanel();
        add(chessBoardPanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5));
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

        JPanel buttonPanel = new JPanel(new FlowLayout());
        readyButton = new JButton("Ready");
        quitButton = new JButton("Quit");
        buttonPanel.add(readyButton);
        buttonPanel.add(quitButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- MOVE LOG: Initialize and configure the move log area ---
        moveLogArea = new JTextArea(10, 15);
        moveLogArea.setEditable(false);
        moveLogArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        moveLogArea.setMargin(new Insets(5, 5, 5, 5));
        // Make the text area scroll automatically to the bottom
        DefaultCaret caret = (DefaultCaret) moveLogArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(moveLogArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Add a title to the move log panel
        JPanel moveLogPanel = new JPanel(new BorderLayout());
        moveLogPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        moveLogPanel.add(new JLabel("Move Log", SwingConstants.CENTER), BorderLayout.NORTH);
        moveLogPanel.add(scrollPane, BorderLayout.CENTER);

        // Add the move log to the right (EAST) side of the window
        add(moveLogPanel, BorderLayout.EAST);

        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(null);
    }

    // --- MOVE LOG: New method to add a move to the log ---
    public void addMoveToLog(int moveNumber, String moveSan, Colour turn) {
        if (turn == Colour.WHITE) {
            // Start a new line for White's move
            moveLogArea.append(String.format("%d. %-8s", moveNumber, moveSan));
        } else {
            // Append Black's move to the existing line
            moveLogArea.append(String.format("%-8s\n", moveSan));
        }
    }

    public JButton getReadyButton() { return readyButton; }
    public void addReadyListener(ActionListener listener) { readyButton.addActionListener(listener); }
    public JMenuItem getNewGameMenuItem() { return newGameMenuItem; }
    public ChessBoardPanel getChessBoardPanel() { return chessBoardPanel; }
    public JMenuItem getLoadPgnMenuItem() { return loadPgnMenuItem; }
    public JMenuItem getSavePgnMenuItem() { return savePgnMenuItem; }
    public void addSavePgnListener(ActionListener listener) { savePgnMenuItem.addActionListener(listener); }
    public void setStatus(String text) { statusLabel.setText(text); }
    public void updatePlayerInfo(Map<String, String> headers) { whitePlayerLabel.setText("White: " + headers.getOrDefault("White", "Unknown")); blackPlayerLabel.setText("Black: " + headers.getOrDefault("Black", "Unknown")); }
    public void updateClock(Colour colour, String time) { if (colour == Colour.WHITE) { whiteClockLabel.setText("Time: " + time); } else { blackClockLabel.setText("Time: " + time); } }
    public void addQuitListener(ActionListener listener) { quitButton.addActionListener(listener); }
    public void showGameOverDialog(String message, String title) { JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE); }
    public String askPromotionChoice() { Piece[] promotionPieces = { new Queen(Colour.WHITE), new Rook(Colour.WHITE), new Bishop(Colour.WHITE), new Knight(Colour.WHITE) }; ImageIcon[] icons = new ImageIcon[promotionPieces.length]; for (int i = 0; i < promotionPieces.length; i++) { Image img = PieceImageLoader.getImage(promotionPieces[i]); if (img != null) { icons[i] = new ImageIcon(img.getScaledInstance(40, 40, Image.SCALE_SMOOTH)); } } int choice = JOptionPane.showOptionDialog(this, "Choose piece for promotion:", "Pawn Promotion", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, icons, icons[0]); if (choice >= 0 && choice < promotionPieces.length) { return String.valueOf(promotionPieces[choice].getSymbol()).toUpperCase(); } return "Q"; }
}