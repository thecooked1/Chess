package main.view;

import main.model.pieces.Piece; // <-- IMPORT PIECE
import main.model.Position;

import javax.swing.*;
import java.awt.*;

public class SquarePanel extends JPanel {
    private final int row;
    private final int col;
    private Piece piece;

    private final Color lightColor = new Color(240, 217, 181);
    private final Color darkColor = new Color(181, 136, 99);
    private final Color highlightColor = new Color(135, 152, 106, 180);
    private final Color selectedColor = new Color(255, 255, 0, 100);
    private boolean isHighlighted = false;
    private boolean isSelected = false;

    public SquarePanel(int row, int col) {
        this.row = row;
        this.col = col;
        setPreferredSize(new Dimension(50, 50));
    }

    // --- ADD THIS METHOD to allow the board to set the piece ---
    public void setPiece(Piece piece) {
        this.piece = piece;
    }


    // Your existing methods are perfect
    public Position getPosition() {
        return new Position(row, col);
    }
    public void setHighlighted(boolean highlighted) {
        this.isHighlighted = highlighted;
        repaint();
    }
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw base square color
        boolean isLight = (row + col) % 2 == 0;
        g.setColor(isLight ? lightColor : darkColor);
        g.fillRect(0, 0, getWidth(), getHeight());

        //  Draw highlights
        if (isSelected) {
            g.setColor(selectedColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        } else if (isHighlighted) {
            g.setColor(highlightColor);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
        }

        // DRAW THE PIECE
        if (piece != null) {
            Image img = PieceImageLoader.getImage(piece);
            if (img != null) {
                // Draw the image to fill the square
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}