package com.chess.view;

import com.chess.model.Board.Board;
import com.chess.common.*;
import com.chess.model.pieces.Piece;
import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ChessBoardPanel extends JPanel {

    private Colour perspective = Colour.WHITE;

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
            }
        }

        reorderSquaresForPerspective();

    }

    /**
     * Re-arranges the square panels in the grid layout based on the current perspective.
     */
    private void reorderSquaresForPerspective() {
        this.removeAll(); // Clear the panel of all components.

        if (perspective == Colour.WHITE) {
            // Add squares from rank 0 to 7 (top to bottom).
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    this.add(squarePanels[r][c]);
                }
            }
        } else { // Black's perspective
            // Add squares from rank 7 to 0 (bottom to top, effectively flipping the board).
            for (int r = 7; r >= 0; r--) {
                for (int c = 0; c < 8; c++) {
                    // You could also flip the files if desired (c = 7 to 0),
                    // but vertical flip is standard.
                    this.add(squarePanels[r][c]);
                }
            }
        }

        // Tell the layout manager to re-validate the component hierarchy.
        this.revalidate();
        this.repaint();
    }

    // This is the old method, which we can keep for compatibility or internal use.
    public void updateBoard(Board board) {
        updateBoard(board, this.perspective); // Call the new method with the existing perspective
    }

    public void updateBoard(Board board, Colour playerPerspective) {
        this.board = board;

        // If the perspective has changed, re-order the board panels.
        if (this.perspective != playerPerspective) {
            this.perspective = playerPerspective;
            reorderSquaresForPerspective();
        }

        if (board == null) return;
        Square kingInCheck = board.getKingInCheckSquare();

        // Update the state of each individual square panel.
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square currentSq = new Square(r, c);
                Piece piece = board.getPiece(currentSq);
                squarePanels[r][c].setPiece(piece);
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
        Component comp = getComponentAt(point);
        if (comp instanceof SquarePanel) {
            SquarePanel panel = (SquarePanel) comp;
            // The panel already knows its logical rank and file!
            return new Square(panel.getRank(), panel.getFile());
        }
        return null;
    }
}