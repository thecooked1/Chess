package main.view;
import main.model.Board.Board;
import main.model.Position;
import main.model.pieces.Piece;
import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ChessBoardPanel extends JPanel {

    private final SquarePanel[][] squarePanels;
    private Board board;
    private final Set<Position> highlightedSquares = new HashSet<>();
    private Position selectedSquare = null;

    // For drag and drop visuals
    private Image draggedPieceImage = null;
    private Point draggedPieceLocation = null;

    public ChessBoardPanel() {
        setLayout(new GridLayout(8, 8));
        squarePanels = new SquarePanel[8][8];
        initializeBoardSquares();
        setPreferredSize(new Dimension(400, 400));
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
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPiece(r, c);
                squarePanels[r][c].setPiece(piece);
            }
        }
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
            if (this.board != null && this.board.isValidPosition(pos.getRow(), pos.getCol())) {
                squarePanels[pos.getRow()][pos.getCol()].setHighlighted(false);
            }
        }
        highlightedSquares.clear();
        repaint();
    }

    public void selectSquare(Position position) {
        clearSelection();
        if (position != null && this.board != null && this.board.isValidPosition(position.getRow(), position.getCol())) {
            selectedSquare = position;
            squarePanels[position.getRow()][position.getCol()].setSelected(true);
        }
        repaint();
    }


    public void clearSelection() {
        if (selectedSquare != null && this.board != null && this.board.isValidPosition(selectedSquare.getRow(), selectedSquare.getCol())) {
            squarePanels[selectedSquare.getRow()][selectedSquare.getCol()].setSelected(false);
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
        repaint();
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