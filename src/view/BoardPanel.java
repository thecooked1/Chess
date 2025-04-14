package view;

import controller.GameController; // Need controller ref
import model.*; // Need model classes

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class BoardPanel extends JPanel {

    private static final int SQUARE_SIZE = 50; // Size of each square
    private static final Color LIGHT_SQUARE = new Color(221, 192, 127);
    private static final Color DARK_SQUARE = new Color(101, 67, 33);
    private static final Color HIGHLIGHT_SELECT_COLOR = new Color(30, 144, 255, 150); // Dodger blue transparent
    private static final Color HIGHLIGHT_LEGAL_COLOR = new Color(0, 255, 0, 100);   // Green transparent

    private GameController controller;
    private Piece[][] boardState; // Local copy of board state for drawing
    private Map<String, BufferedImage> pieceImages;

    private Position selectedSquare = null; // Track which square is clicked
    private List<Position> legalMoveHighlights = new ArrayList<>();

    public BoardPanel(GameController controller) {
        this.controller = controller;
        this.boardState = new Piece[8][8]; // Initialize empty
        this.pieceImages = loadPieceImages();

        setPreferredSize(new Dimension(8 * SQUARE_SIZE, 8 * SQUARE_SIZE));
        setMaximumSize(getPreferredSize());
        setMinimumSize(getPreferredSize());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (controller == null) return; // Should not happen
                int col = e.getX() / SQUARE_SIZE;
                int row = e.getY() / SQUARE_SIZE;
                if (Position.isValid(row, col)) {
                    controller.handleSquareClick(new Position(row, col));
                }
            }
            // Add mouseDragged / mouseReleased here if implementing drag-and-drop
        });
    }

    // Called by Controller to update the display
    public void setBoardState(Piece[][] newBoardState) {
        this.boardState = newBoardState;
        repaint();
    }

    public void setSelectedSquare(Position pos) {
        this.selectedSquare = pos;
        repaint();
    }

    public void setLegalMoveHighlights(List<Position> targets) {
        this.legalMoveHighlights = (targets != null) ? new ArrayList<>(targets) : new ArrayList<>();
        repaint();
    }

    public void clearHighlights() {
        this.selectedSquare = null;
        this.legalMoveHighlights.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Draw Square
                Color squareColor = ((row + col) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                g2d.setColor(squareColor);
                g2d.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);

                // Draw Piece
                Piece piece = (boardState != null && Position.isValid(row, col)) ? boardState[row][col] : null;
                if (piece != null) {
                    BufferedImage img = getPieceImage(piece);
                    if (img != null) {
                        g2d.drawImage(img, col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE, null);
                    }
                }
            }
        }

        // Draw Highlights
        if (selectedSquare != null) {
            g2d.setColor(HIGHLIGHT_SELECT_COLOR);
            g2d.fillRect(selectedSquare.getCol() * SQUARE_SIZE, selectedSquare.getRow() * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
        }

        g2d.setColor(HIGHLIGHT_LEGAL_COLOR);
        for (Position target : legalMoveHighlights) {
            g2d.fillOval(target.getCol() * SQUARE_SIZE + SQUARE_SIZE / 4,
                    target.getRow() * SQUARE_SIZE + SQUARE_SIZE / 4,
                    SQUARE_SIZE / 2, SQUARE_SIZE / 2); // Draw small circle for legal moves
        }

    }

    private Map<String, BufferedImage> loadPieceImages() {
        Map<String, BufferedImage> images = new HashMap<>();
        String[] types = {"pawn", "rook", "knight", "bishop", "queen", "king"};
        String[] colors = {"b", "w"}; // black, white prefixes

        for (String type : types) {
            for (String color : colors) {
                String filename = "/assets/" + color + type + ".png"; // Assuming images are in resources/assets folder
                try {
                    BufferedImage img = ImageIO.read(getClass().getResource(filename));
                    if (img != null) {
                        images.put(color + type, img);
                    } else {
                        System.err.println("Warning: Could not load image " + filename);
                    }
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("Error loading image " + filename + ": " + e.getMessage());
                }
            }
        }
        return images;
    }

    private BufferedImage getPieceImage(Piece piece) {
        if (piece == null) return null;
        String colorPrefix = (piece.getColor() == PlayerColor.BLACK) ? "b" : "w";
        String typeName = piece.getType().name().toLowerCase();
        return pieceImages.get(colorPrefix + typeName);
    }
}