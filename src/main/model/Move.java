package main.model;

import main.model.PieceType; // Needed for promotion

import java.util.Objects;

public class Move {
    private final Position startPosition;
    private final Position endPosition;
    private final PieceType promotionPieceType; // Null if not a promotion move

    public Move(Position start, Position end) {
        this(start, end, null);
    }

    public Move(Position start, Position end, PieceType promotionPieceType) {
        Objects.requireNonNull(start, "Start position cannot be null");
        Objects.requireNonNull(end, "End position cannot be null");
        this.startPosition = start;
        this.endPosition = end;
        this.promotionPieceType = promotionPieceType;
    }

    public Position getStartPosition() {
        return startPosition;
    }

    public Position getEndPosition() {
        return endPosition;
    }

    public PieceType getPromotionPieceType() {
        return promotionPieceType;
    }

    public boolean isPromotion() {
        return promotionPieceType != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return startPosition.equals(move.startPosition) &&
                endPosition.equals(move.endPosition) &&
                promotionPieceType == move.promotionPieceType; // handles null correctly
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPosition, endPosition, promotionPieceType);
    }

    @Override
    public String toString() {
        String s = startPosition + "" + endPosition;
        if (isPromotion()) {
            s += "=" + promotionPieceType.name().charAt(0); // e.g., e7e8=Q
        }
        return s;
    }
}