package model;

/**
 * Represents the color of a chess piece or player.
 */
public enum PieceColor {
    WHITE, BLACK;

    /**
     * Returns the opposite color.
     * @return The opposite PieceColor.
     */
    public PieceColor opposite() {
        return (this == WHITE) ? BLACK : WHITE;
    }
}