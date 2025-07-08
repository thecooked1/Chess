package com.chess.view;

import com.chess.model.Board.Board;
import com.chess.common.*;
import com.chess.model.pieces.Piece;
import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ChessBoardPanel extends JPanel {

    private final int BOARD_SIZE = 8;
    private Colour perspective = Colour.WHITE;
    private static final int LABEL_OFFSET = 20;

    private Board board;
    private final Set<Square> highlightedSquares = new HashSet<>();
    private final Set<Square> highlightedMoveSquares = new HashSet<>();
    private final Set<Square> highlightedCaptureSquares = new HashSet<>();
    private Square selectedSquare = null;
    private Square lastMoveFrom = null;
    private Square lastMoveTo = null;
    private boolean isCheckmate = false;

    private Image draggedPieceImage = null;
    private Point draggedPieceLocation = null;

    public ChessBoardPanel() {
        setPreferredSize(new Dimension(500, 500));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBoardAndPieces(g);
        drawHighlights(g);
        drawLabels(g);
        drawDraggedPiece(g);
    }

    private void drawBoardAndPieces(Graphics g) {
        for (int rank = 0; rank < BOARD_SIZE; rank++) {
            for (int file = 0; file < BOARD_SIZE; file++) {
                Square logicalSquare = new Square(rank, file);
                Point coords = getPointFromSquare(logicalSquare);

                // --- Step 1: Draw base square color ---
                if ((rank + file) % 2 == 0) {
                    g.setColor(new Color(238, 238, 210)); // Light
                } else {
                    g.setColor(new Color(118, 150, 86));  // Dark
                }
                g.fillRect(coords.x, coords.y, getSquareWidth(), getSquareHeight());

                // --- Step 2: Draw last move highlight (if applicable) ---
                if (logicalSquare.equals(lastMoveFrom) || logicalSquare.equals(lastMoveTo)) {
                    g.setColor(new Color(255, 255, 51, 90)); // Transparent yellow
                    g.fillRect(coords.x, coords.y, getSquareWidth(), getSquareHeight());
                }

                // --- Step 3: Draw king in check/checkmate highlight (if applicable) ---
                if (board != null) {
                    Square kingInCheck = board.getKingInCheckSquare();
                    if (logicalSquare.equals(kingInCheck)) {
                        g.setColor(isCheckmate ? new Color(220, 20, 60) : new Color(255, 0, 0, 120)); // Solid red for checkmate
                        g.fillRect(coords.x, coords.y, getSquareWidth(), getSquareHeight());
                    }
                }

                // --- Step 4: Draw the piece ---
                if (board != null) {
                    Piece piece = board.getPiece(logicalSquare);
                    boolean isBeingDragged = logicalSquare.equals(selectedSquare) && draggedPieceImage != null;
                    if (piece != null && !isBeingDragged) {
                        Image pieceImage = PieceImageLoader.getImage(piece);
                        g.drawImage(pieceImage, coords.x, coords.y, getSquareWidth(), getSquareHeight(), this);
                    }
                }
            }
        }
    }

    private void drawHighlights(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int squareWidth = getSquareWidth();
        int squareHeight = getSquareHeight();

        // Highlight for legal moves (small dot)
        for (Square sq : highlightedMoveSquares) {
            Point coords = getPointFromSquare(sq);
            g.setColor(new Color(0, 0, 0, 50));
            g.fillOval(coords.x + squareWidth / 2 - 10, coords.y + squareHeight / 2 - 10, 20, 20);
        }

        // Highlight for legal captures (circle outline)
        g2d.setColor(new Color(0, 0, 0, 70));
        g2d.setStroke(new BasicStroke(4)); // Thicker outline
        for (Square sq : highlightedCaptureSquares) {
            Point coords = getPointFromSquare(sq);
            g2d.drawOval(coords.x + 4, coords.y + 4, squareWidth - 8, squareHeight - 8);
        }
    }

    private void drawLabels(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        int squareWidth = getSquareWidth();
        int squareHeight = getSquareHeight();

        for (int i = 0; i < BOARD_SIZE; i++) {
            // Draw File labels (a-h)
            String fileLabel = (perspective == Colour.WHITE)
                    ? String.valueOf((char)('a' + i))
                    : String.valueOf((char)('h' - i));
            g.drawString(fileLabel, LABEL_OFFSET + i * squareWidth + squareWidth / 2 - 4, getHeight() - 5);

            // Draw Rank labels (1-8)
            String rankLabel = (perspective == Colour.WHITE)
                    ? String.valueOf(8 - i)
                    : String.valueOf(i + 1);
            g.drawString(rankLabel, 5, LABEL_OFFSET + i * squareHeight + squareHeight / 2 + 4);
        }
    }

    private void drawDraggedPiece(Graphics g) {
        if (draggedPieceImage != null && draggedPieceLocation != null) {
            int x = draggedPieceLocation.x - getSquareWidth() / 2;
            int y = draggedPieceLocation.y - getSquareHeight() / 2;
            g.drawImage(draggedPieceImage, x, y, getSquareWidth(), getSquareHeight(), this);
        }
    }

    public void updateBoard(Board board, Colour playerPerspective) {
        this.board = board;
        this.perspective = playerPerspective;
        this.repaint(); // Just repaint, no need to re-add components.
    }

    public void highlightLegalMoves(Set<Square> squares, Board currentBoard) {
        clearHighlights();
        for (Square sq : squares) {
            if (currentBoard.getPiece(sq) != null) {
                highlightedCaptureSquares.add(sq);
            } else {
                highlightedMoveSquares.add(sq);
            }
        }
        repaint();
    }

    public void clearHighlights() {
        highlightedMoveSquares.clear();
        highlightedCaptureSquares.clear();
        repaint();
    }

    public void setLastMove(Square from, Square to) {
        this.lastMoveFrom = from;
        this.lastMoveTo = to;
        repaint();
    }

    public void setCheckmate(boolean isCheckmate) {
        this.isCheckmate = isCheckmate;
        repaint();
    }

    public void selectSquare(Square square) {
        selectedSquare = square;
        repaint();
    }

    public void clearSelection() {
        selectedSquare = null;
        repaint();
    }

    public void setDraggedPiece(Piece piece, Point location) {
        this.draggedPieceImage = (piece != null) ? PieceImageLoader.getImage(piece) : null;
        this.draggedPieceLocation = location;
        repaint();
    }

    public void clearDraggedPiece() {
        setDraggedPiece(null, null);
    }

    public int getSquareWidth() { return (int) (getWidth() / 8.8); }
    public int getSquareHeight() { return (int) (getHeight() / 8.8); }

    private Point getPointFromSquare(Square square) {
        int squareWidth = getSquareWidth();
        int squareHeight = getSquareHeight();

        int x, y;
        if (perspective == Colour.WHITE) {
            x = square.file() * squareWidth;
            y = square.rank() * squareHeight;
        } else { // Black's perspective
            x = (7 - square.file()) * squareWidth;
            y = (7 - square.rank()) * squareHeight;
        }
        return new Point(x + LABEL_OFFSET, y + LABEL_OFFSET);
    }

    public Square getSquareFromPoint(Point point) {
        if (point == null) return null;

        int squareWidth = getSquareWidth();
        int squareHeight = getSquareHeight();

        // Adjust for the label offset
        int adjustedX = point.x - LABEL_OFFSET;
        int adjustedY = point.y - LABEL_OFFSET;

        if (adjustedX < 0 || adjustedY < 0) return null;

        int file = adjustedX / squareWidth;
        int rank = adjustedY / squareHeight;

        if (file > 7 || rank > 7) return null;

        if (perspective == Colour.WHITE) {
            return new Square(rank, file);
        } else { // Black's perspective (invert the coordinates)
            return new Square(7 - rank, 7 - file);
        }
    }
}