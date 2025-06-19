package main.view;

import main.model.Board.Board;
import main.model.Square; // Import the correct Square class
import main.model.pieces.Piece;
import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ChessBoardPanel extends JPanel {

    private final SquarePanel[][] squarePanels;
    private Board board;
    private final Set<Square> highlightedSquares = new HashSet<>();
    private Square selectedSquare = null;

    // For drag and drop visuals
    private Image draggedPieceImage = null;
    private Point draggedPieceLocation = null;

    public ChessBoardPanel() {
        setLayout(new GridLayout(8, 8));
        squarePanels = new SquarePanel[8][8];
        initializeBoardSquares();
        setPreferredSize(new Dimension(500, 500)); // Increased size for better visuals
    }

    private void initializeBoardSquares() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squarePanels[r][c] = new SquarePanel(r, c);
                this.add(squarePanels[r][c]);
            }
        }
    }

    public void updateBoard(Board board) {
        this.board = board;
        if (board == null) return;

        Square kingInCheck = board.getKingInCheckSquare();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square currentSq = new Square(r, c);
                Piece piece = board.getPiece(currentSq);
                squarePanels[r][c].setPiece(piece);
                // Set the special "in check" highlight for the king's square
                squarePanels[r][c].setInCheck(currentSq.equals(kingInCheck));
            }
        }
        this.repaint();
    }

    public void highlightLegalMoves(Set<Square> squares) {
        clearHighlights();
        highlightedSquares.addAll(squares);
        for (Square sq : squares) {
            squarePanels[sq.rank()][sq.file()].setHighlighted(true);
        }
        repaint();
    }

    public void clearHighlights() {
        for (Square sq : highlightedSquares) {
            // Check validity before accessing array to prevent rare errors
            if (sq != null && sq.isValid()) {
                squarePanels[sq.rank()][sq.file()].setHighlighted(false);
            }
        }
        highlightedSquares.clear();
        repaint();
    }

    public void selectSquare(Square square) {
        clearSelection();
        if (square != null && square.isValid()) {
            selectedSquare = square;
            squarePanels[square.rank()][square.file()].setSelected(true);
        }
        repaint();
    }

    public void clearSelection() {
        if (selectedSquare != null && selectedSquare.isValid()) {
            squarePanels[selectedSquare.rank()][selectedSquare.file()].setSelected(false);
        }
        selectedSquare = null;
        repaint();
    }

    public void setDraggedPiece(Piece piece, Point location) {
        if (piece != null) {
            this.draggedPieceImage = PieceImageLoader.getImage(piece);
        } else {
            this.draggedPieceImage = null;
        }
        this.draggedPieceLocation = location;
        repaint(); // Repaint the whole panel to show the piece being dragged over other components
    }

    public void clearDraggedPiece() {
        this.draggedPieceImage = null;
        this.draggedPieceLocation = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (draggedPieceImage != null && draggedPieceLocation != null) {
            int x = draggedPieceLocation.x - getSquareWidth() / 2;
            int y = draggedPieceLocation.y - getSquareHeight() / 2;
            g.drawImage(draggedPieceImage, x, y, getSquareWidth(), getSquareHeight(), this);
        }
    }

    public int getSquareWidth() { return getWidth() / 8; }
    public int getSquareHeight() { return getHeight() / 8; }


    public Square getSquareFromPoint(Point point) {
        if (point == null) return null;
        int file = point.x / getSquareWidth();  // file is the column (x-axis)
        int rank = point.y / getSquareHeight(); // rank is the row (y-axis)
        if (rank >= 0 && rank < 8 && file >= 0 && file < 8) {
            return new Square(rank, file);
        }
        return null;
    }
}