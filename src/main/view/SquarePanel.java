package main.view;

import main.model.Position;

import javax.swing.*;
import java.awt.*;

public class SquarePanel extends JPanel {
    private final int row;
    private final int col;
    private final Color lightColor = new Color(240, 217, 181); // Beige
    private final Color darkColor = new Color(181, 136, 99);  // Brown
    private final Color highlightColor = new Color(135, 152, 106, 180); // Semi-transparent Greenish
    private final Color selectedColor = new Color(255, 255, 0, 100); // Semi-transparent Yellow
    private boolean isHighlighted = false;
    private boolean isSelected = false; // To highlight the selected piece's square

    public SquarePanel(int row, int col) {
        this.row = row;
        this.col = col;
        setPreferredSize(new Dimension(50, 50)); // Adjust size as needed
    }

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
        // Set base square color
        boolean isLight = (row + col) % 2 == 0;
        g.setColor(isLight ? lightColor : darkColor);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw selection highlight (if selected)
        if (isSelected) {
            g.setColor(selectedColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        // Draw move highlight (if highlighted)
        else if (isHighlighted) {
            g.setColor(highlightColor);
            // Draw a circle or different shape for potential moves
            int offset = 5;
            int diameter = getWidth() - 2 * offset;
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fillOval(offset, offset, diameter, diameter);

            // g.fillRect(0, 0, getWidth(), getHeight()); // Or simpler rectangle highlight
        }

        // Piece drawing will be handled by the ChessBoardPanel overlaying images
    }
}