package main.view;

import main.model.Board;
import main.model.GameState;
import main.model.Position;
import main.model.Piece;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChessBoardPanel extends JPanel {

    private final SquarePanel[][] squarePanels;
    private GameState currentGameState;
    private final Set<Position> highlightedSquares = new HashSet<>();
    private Position selectedSquare = null;

    // For drag and drop visuals
    private Image draggedPieceImage = null;
    private Point draggedPieceLocation = null;

    public ChessBoardPanel() {
        setLayout(new GridLayout(8, 8));
        squarePanels = new SquarePanel[8][8];
        initializeBoardSquares();
        setPreferredSize(new Dimension(400, 400)); // 8 * 50
    }

    private void initializeBoardSquares() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squarePanels[r][c] = new SquarePanel(r, c);
                this.add(squarePanels[r][c]);
            }
        }
    }

    public void updateBoard(GameState gameState) {
        this.currentGameState = gameState;
        this.repaint();
    }

    public void highlightLegalMoves(Set<Position> positions) {
        clearHighlights();
        highlightedSquares.addAll(positions);
        for (Position pos : positions) {
            squarePanels[pos.getRow()][pos.getCol()].setHighlighted(true);
        }
        repaint();
    }

    public void clearHighlights() {
        for (Position pos : highlightedSquares) {
            if (currentGameState.getBoard().isValidPosition(pos)) { // Check bounds just in case
                squarePanels[pos.getRow()][pos.getCol()].setHighlighted(false);
            }
        }
        highlightedSquares.clear();
        repaint();
    }

    public void selectSquare(Position position) {
        clearSelection(); // Clear previous selection
        if (position != null && currentGameState.getBoard().isValidPosition(position)) {
            selectedSquare = position;
            squarePanels[position.getRow()][position.getCol()].setSelected(true);
        }
        repaint();
    }

    public void clearSelection() {
        if (selectedSquare != null && currentGameState.getBoard().isValidPosition(selectedSquare)) {
            squarePanels[selectedSquare.getRow()][selectedSquare.getCol()].setSelected(false);
        }
        selectedSquare = null;
        repaint();
    }

    // Methods for drag visuals
    public void setDraggedPiece(Piece piece, Point location) {
        if (piece != null) {
            this.draggedPieceImage = PieceImageLoader.getImage(piece.getType(), piece.getColor());
        } else {
            this.draggedPieceImage = null;
        }
        this.draggedPieceLocation = location;
        repaint(); // Trigger repaint to draw the dragged piece
    }

    public void clearDraggedPiece() {
        this.draggedPieceImage = null;
        this.draggedPieceLocation = null;
        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Paints the child SquarePanels

        if (currentGameState == null) return;

        // Draw pieces on top of squares
        Board board = currentGameState.getBoard();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPieceAt(r, c);
                if (piece != null) {
                    // Don't draw the piece being dragged at its original square
                    boolean isBeingDragged = selectedSquare != null && selectedSquare.equals(new Position(r,c)) && draggedPieceImage != null;
                    if (!isBeingDragged) {
                        Image img = PieceImageLoader.getImage(piece.getType(), piece.getColor());
                        if (img != null) {
                            g.drawImage(img, c * getSquareWidth(), r * getSquareHeight(), getSquareWidth(), getSquareHeight(), this);
                        }
                    }
                }
            }
        }

        // Draw the dragged piece at the mouse cursor location
        if (draggedPieceImage != null && draggedPieceLocation != null) {
            int x = draggedPieceLocation.x - getSquareWidth() / 2;
            int y = draggedPieceLocation.y - getSquareHeight() / 2;
            g.drawImage(draggedPieceImage, x, y, getSquareWidth(), getSquareHeight(), this);
        }
    }

    public int getSquareWidth() {
        return getWidth() / 8;
    }

    public int getSquareHeight() {
        return getHeight() / 8;
    }

    // Helper to convert pixel coordinates to board position
    public Position getPositionFromPoint(Point point) {
        if (point == null) return null;
        int col = point.x / getSquareWidth();
        int row = point.y / getSquareHeight();
        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
            return new Position(row, col);
        }
        return null;
    }
}