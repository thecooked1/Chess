package model;

import java.util.Objects;

public class Move {
    private final Position start;
    private final Position end;
    private final Piece pieceMoved;
    private Piece pieceCaptured; // Can be null
    // Add flags for promotion, castling, en passant if implementing those fully

    public Move(Piece pieceMoved, Position start, Position end) {
        Objects.requireNonNull(pieceMoved, "Piece moved cannot be null");
        Objects.requireNonNull(start, "Start position cannot be null");
        Objects.requireNonNull(end, "End position cannot be null");
        this.pieceMoved = pieceMoved;
        this.start = start;
        this.end = end;
    }

    public Position getStart() { return start; }
    public Position getEnd() { return end; }
    public Piece getPieceMoved() { return pieceMoved; }
    public Piece getPieceCaptured() { return pieceCaptured; }
    public void setPieceCaptured(Piece pieceCaptured) { this.pieceCaptured = pieceCaptured; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        // Basic equality check, might need refinement for special moves
        return start.equals(move.start) && end.equals(move.end) && pieceMoved.equals(move.pieceMoved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, pieceMoved);
    }

    @Override
    public String toString() {
        return pieceMoved.getType() + " from " + start + " to " + end + (pieceCaptured != null ? " captures " + pieceCaptured.getType() : "");
    }
}