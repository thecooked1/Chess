package main.model;

import main.model.Board;
import main.model.Color;
import main.model.Position;

import java.util.List;
import java.util.Objects;

public abstract class Piece {
    protected final Color color;
    protected final PieceType type;
    protected boolean hasMoved; // Important for Pawns, Kings, Rooks (castling)

    protected Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
        this.hasMoved = false; // Initially, no piece has moved
    }

    public Color getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    /**
     * Calculates all pseudo-legal moves for this piece from the given position
     * on the board. Pseudo-legal means it doesn't consider whether the move
     * leaves the king in check. Check validation happens at a higher level (GameLogic).
     *
     * @param board   The current state of the board model.
     * @param currentPosition The position of this piece.
     * @return A list of potential target Positions.
     */
    public abstract List<Position> getPseudoLegalMoves(Board board, Position currentPosition);

    // Helper method for Rook, Bishop, Queen move generation
    protected void addMovesInDirection(List<Position> moves, Board board, Position current, int dRow, int dCol) {
        int nextRow = current.getRow() + dRow;
        int nextCol = current.getCol() + dCol;

        // Loop while the calculated coordinates are within the board bounds
        while (nextRow >= 0 && nextRow < 8 && nextCol >= 0 && nextCol < 8) {
            // Only create Position object if coordinates are valid
            Position nextPos = new Position(nextRow, nextCol);
            Piece targetPiece = board.getPieceAt(nextPos);

            if (targetPiece == null) {
                moves.add(nextPos); // Empty square, can move here
            } else {
                if (targetPiece.getColor() != this.color) {
                    moves.add(nextPos); // Can capture opponent's piece
                }
                break; // Path blocked (either by own or opponent's piece), stop searching in this direction
            }

            // Move to the next square in the same direction for the next iteration
            nextRow += dRow;
            nextCol += dCol;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return hasMoved == piece.hasMoved && color == piece.color && type == piece.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, type, hasMoved);
    }

    @Override
    public String toString() {
        return color + " " + type;
    }
}