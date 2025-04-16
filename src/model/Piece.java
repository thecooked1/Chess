package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for all chess pieces in the model layer.
 * Contains common properties like color, type, and position,
 * and defines the interface for move generation.
 * Contains no UI logic.
 */
public abstract class Piece {
    private final PieceColor color;
    private final PieceType type;
    private Square currentSquare;
    private final String imageFileName; // Keep filename for view reference

    /**
     * Constructs a new Piece.
     * @param color The color of the piece (WHITE or BLACK).
     * @param type The type of the piece (PAWN, ROOK, etc.).
     * @param initSq The initial square of the piece.
     * @param imageFileName The base filename for the piece's image (e.g., "wpawn.png").
     */
    public Piece(PieceColor color, PieceType type, Square initSq, String imageFileName) {
        this.color = color;
        this.type = type;
        this.currentSquare = initSq; // Will be set correctly by Board/Square.put
        this.imageFileName = imageFileName;
        // No image loading here - View handles that
    }

    public PieceColor getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public Square getPosition() {
        return currentSquare;
    }

    /**
     * Sets the current square for this piece.
     * This should generally only be called by Square.put() or Board logic.
     * @param sq The new square.
     */
    void setPosition(Square sq) {
        this.currentSquare = sq;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    /**
     * Calculates and returns a list of squares this piece can *potentially* move to,
     * based *only* on its movement rules and the board layout (including blocking pieces).
     * This list does *not* account for rules like check. The GameLogic class will
     * filter these moves further.
     *
     * @param board The current state of the board.
     * @return A List of potentially legal destination Squares.
     */
    public abstract List<Square> getPotentialMoves(Board board);


    // --- Helper methods for calculating moves (extracted and slightly adapted) ---

    /**
     * Finds potential moves along diagonals.
     * Stops at the first piece encountered. Includes capture square, excludes friendly piece square.
     */
    protected List<Square> getPotentialDiagonalMoves(Board b) {
        List<Square> moves = new ArrayList<>();
        Square[][] grid = b.getSquareGrid();
        int startFile = this.getPosition().getFile();
        int startRank = this.getPosition().getRank();

        int[] fileDirections = {1, 1, -1, -1};
        int[] rankDirections = {1, -1, 1, -1};

        for (int i = 0; i < 4; i++) {
            int currentFile = startFile + fileDirections[i];
            int currentRank = startRank + rankDirections[i];

            while (Board.isValidCoordinate(currentFile, currentRank)) {
                Square targetSquare = grid[currentFile][currentRank];
                if (targetSquare.isOccupied()) {
                    if (targetSquare.getOccupyingPiece().getColor() != this.color) {
                        moves.add(targetSquare); // Can capture
                    }
                    break; // Stop searching in this direction (blocked)
                } else {
                    moves.add(targetSquare); // Empty square, continue
                }
                currentFile += fileDirections[i];
                currentRank += rankDirections[i];
            }
        }
        return moves;
    }

    /**
     * Finds potential moves along ranks and files (horizontally/vertically).
     * Stops at the first piece encountered. Includes capture square, excludes friendly piece square.
     */
    protected List<Square> getPotentialLinearMoves(Board b) {
        List<Square> moves = new ArrayList<>();
        Square[][] grid = b.getSquareGrid();
        int startFile = this.getPosition().getFile();
        int startRank = this.getPosition().getRank();

        int[] fileDirections = {1, -1, 0, 0};
        int[] rankDirections = {0, 0, 1, -1};

        for (int i = 0; i < 4; i++) {
            int currentFile = startFile + fileDirections[i];
            int currentRank = startRank + rankDirections[i];

            while (Board.isValidCoordinate(currentFile, currentRank)) {
                Square targetSquare = grid[currentFile][currentRank];
                if (targetSquare.isOccupied()) {
                    if (targetSquare.getOccupyingPiece().getColor() != this.color) {
                        moves.add(targetSquare); // Can capture
                    }
                    break; // Stop searching in this direction (blocked)
                } else {
                    moves.add(targetSquare); // Empty square, continue
                }
                currentFile += fileDirections[i];
                currentRank += rankDirections[i];
            }
        }
        return moves;
    }

    @Override
    public String toString() {
        return color.name().charAt(0) + "" + type.name().charAt(0) + "@" + (currentSquare != null ? currentSquare.toString() : "null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        // Equality based on type, color, and position should be sufficient for most cases
        // If we need unique IDs later, add them.
        return color == piece.color && type == piece.type && Objects.equals(currentSquare, piece.currentSquare);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, type, currentSquare);
    }
}