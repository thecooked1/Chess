package model;

import java.util.Objects;

/**
 * Represents a single square on the chessboard in the model layer.
 * It knows its coordinates and the piece occupying it (if any).
 * It contains no UI logic.
 */
public class Square {

    private final int file; // Column (0-7, representing a-h)
    private final int rank; // Row (0-7, representing 1-8)
    private Piece occupyingPiece;

    /**
     * Creates a new Square.
     *
     * @param file The file (column) index (0-7).
     * @param rank The rank (row) index (0-7).
     */
    public Square(int file, int rank) {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            throw new IllegalArgumentException("Invalid square coordinates: " + file + ", " + rank);
        }
        this.file = file;
        this.rank = rank;
        this.occupyingPiece = null;
    }

    /**
     * Gets the file (column) index (0-7).
     * @return The file index.
     */
    public int getFile() {
        return file;
    }

    /**
     * Gets the rank (row) index (0-7).
     * @return The rank index.
     */
    public int getRank() {
        return rank;
    }

    /**
     * Gets the piece currently occupying this square.
     * @return The occupying Piece, or null if the square is empty.
     */
    public Piece getOccupyingPiece() {
        return occupyingPiece;
    }

    /**
     * Checks if this square is currently occupied by a piece.
     * @return true if occupied, false otherwise.
     */
    public boolean isOccupied() {
        return (this.occupyingPiece != null);
    }

    /**
     * Places a piece on this square. If a piece was already present,
     * it is simply replaced (capture logic is handled elsewhere).
     * Updates the piece's internal position reference.
     *
     * @param piece The Piece to place on this square. Can be null to clear the square.
     */
    void put(Piece piece) {
        this.occupyingPiece = piece;
        if (piece != null && piece.getPosition() != this) {
            // Ensure piece's internal state is consistent
            piece.setPosition(this);
        }
    }

    /**
     * Removes the piece from this square.
     * Does not update the removed piece's internal position reference.
     *
     * @return The Piece that was removed, or null if the square was empty.
     */
    Piece removePiece() {
        Piece p = this.occupyingPiece;
        this.occupyingPiece = null;
        return p;
    }

    /**
     * Returns a string representation (e.g., "e4").
     * @return Algebraic notation of the square.
     */
    @Override
    public String toString() {
        return "" + (char)('a' + file) + (char)('1' + rank);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Square square = (Square) o;
        return file == square.file && rank == square.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, rank);
    }
}