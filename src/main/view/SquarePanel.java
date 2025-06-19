package main.view;

import main.model.pieces.Piece;
import javax.swing.*;
import java.awt.*;

public class SquarePanel extends JPanel {
    private final int rank;
    private final int file;
    private Piece piece;

    // --- Visuals ---
    private static final Color LIGHT_COLOR = new Color(240, 217, 181);
    private static final Color DARK_COLOR = new Color(181, 136, 99);
    private static final Color HIGHLIGHT_COLOR = new Color(135, 152, 106, 180); // Transparent green
    private static final Color SELECTED_COLOR = new Color(255, 255, 0, 100);    // Transparent yellow
    private static final Color CHECK_COLOR = new Color(255, 0, 0, 90);         // Transparent red

    // --- State ---
    private boolean isHighlighted = false;
    private boolean isSelected = false;
    private boolean isInCheck = false;

    public SquarePanel(int rank, int file) {
        this.rank = rank;
        this.file = file;
        setPreferredSize(new Dimension(60, 60));
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public void setHighlighted(boolean highlighted) {
        this.isHighlighted = highlighted;
        repaint();
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        repaint();
    }

    public void setInCheck(boolean inCheck) {
        this.isInCheck = inCheck;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 1. Draw base square color
        boolean isLight = (rank + file) % 2 == 0;
        g2d.setColor(isLight ? LIGHT_COLOR : DARK_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 2. Draw highlights in order of precedence (check > selected > legal move)
        if (isInCheck) {
            g2d.setColor(CHECK_COLOR);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        } else if (isSelected) {
            g2d.setColor(SELECTED_COLOR);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        } else if (isHighlighted) {
            g2d.setColor(HIGHLIGHT_COLOR);
            // Draw a circle for legal moves
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fillOval(getWidth()/4, getHeight()/4, getWidth()/2, getHeight()/2);
        }

        // 3. Draw the piece image on top of everything
        if (piece != null) {
            Image img = PieceImageLoader.getImage(piece);
            if (img != null) {
                // Draw the image centered and scaled
                int padding = (int) (getWidth() * 0.1);
                g2d.drawImage(img, padding, padding, getWidth() - (2*padding), getHeight() - (2*padding), this);
            }
        }
    }
}