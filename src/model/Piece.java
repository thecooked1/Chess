package model;

import java.util.List;
import java.util.Objects;

public abstract class Piece {
    protected final PlayerColor color;

    public Piece(PlayerColor color) {
        this.color = color;
    }

    public PlayerColor getColor() {
        return color;
    }

    public abstract PieceType getType();

    /**
     * Generates a list of pseudo-legal moves for this piece.
     * Pseudo-legal means moves that are valid according to the piece's movement rules,
     * ignoring whether the move would leave the king in check.
     *
     * @param board The current board state.
     * @param currentPos The current position of this piece.
     * @return A list of target Positions the piece could potentially move to.
     */
    public abstract List<Position> getPseudoLegalMoves(BoardModel board, Position currentPos);

    // Helper method for linear moves (Rook, Queen)
    protected void addLinearMoves(BoardModel board, Position currentPos, List<Position> moves, int dRow, int dCol) {
        int r = currentPos.getRow() + dRow;
        int c = currentPos.getCol() + dCol;
        while (Position.isValid(r, c)) {
            Piece targetPiece = board.getPieceAt(new Position(r, c));
            if (targetPiece == null) {
                moves.add(new Position(r, c)); // Empty square
            } else {
                if (targetPiece.getColor() != this.color) {
                    moves.add(new Position(r, c)); // Capture opponent
                }
                break; // Blocked by own or opponent piece
            }
            r += dRow;
            c += dCol;
        }
    }

    // Helper method for diagonal moves (Bishop, Queen)
    protected void addDiagonalMoves(BoardModel board, Position currentPos, List<Position> moves, int dRow, int dCol) {
        addLinearMoves(board, currentPos, moves, dRow, dCol); // Same logic as linear but different directions passed in
    }

    @Override
    public boolean equals(Object o) {
        // Basic equality based on type and color - useful for lists but not unique identity
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return color == piece.color && getType() == piece.getType(); // Comparing type is essential
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, getType());
    }

    @Override
    public String toString() {
        return color + " " + getType();
    }
}