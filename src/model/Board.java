package model;

import java.util.*;

import model.piece.*;

/**
 * Represents the chessboard in the model layer.
 * It holds the grid of Squares and the Pieces on them.
 * It provides methods for accessing squares and pieces, and for applying moves,
 * but does not contain game rules logic itself (that's in GameLogic).
 * It also holds a reference to the current GameState.
 */
public class Board {
    // Use File (column 0-7) then Rank (row 0-7) for indexing: grid[file][rank]
    private final Square[][] grid;
    private GameState gameState; // Reference to the game state
    private final Map<PieceColor, Square> kingSquares; // Track king positions efficiently

    // Resource location constants for piece images (used during initialization)
    private static final String RESOURCES_WBISHOP_PNG = "wbishop.png";
    private static final String RESOURCES_BBISHOP_PNG = "bbishop.png";
    private static final String RESOURCES_WKNIGHT_PNG = "wknight.png";
    private static final String RESOURCES_BKNIGHT_PNG = "bknight.png";
    private static final String RESOURCES_WROOK_PNG = "wrook.png";
    private static final String RESOURCES_BROOK_PNG = "brook.png";
    private static final String RESOURCES_WKING_PNG = "wking.png";
    private static final String RESOURCES_BKING_PNG = "bking.png";
    private static final String RESOURCES_BQUEEN_PNG = "bqueen.png";
    private static final String RESOURCES_WQUEEN_PNG = "wqueen.png";
    private static final String RESOURCES_WPAWN_PNG = "wpawn.png";
    private static final String RESOURCES_BPAWN_PNG = "bpawn.png";


    /**
     * Creates a new Board and initializes it with the standard chess starting position.
     * @param gameState The initial GameState object.
     */
    public Board(GameState gameState) {
        this.grid = new Square[8][8];
        this.kingSquares = new HashMap<>();
        this.gameState = gameState; // Store the reference
        initializeBoard();
        initializePieces();
        // Add initial position hash to game state history
        this.gameState.recordMove(null, calculateBoardHash()); // Record initial state with null move
    }

    /**
     * Private constructor for creating copies (used in simulations).
     */
    private Board(Square[][] grid, GameState gameState, Map<PieceColor, Square> kingSquares) {
        this.grid = grid;
        this.gameState = gameState;
        this.kingSquares = kingSquares;
    }


    private void initializeBoard() {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                grid[file][rank] = new Square(file, rank);
            }
        }
    }

    /**
     * Sets up the pieces in their standard starting positions.
     */
    private void initializePieces() {
        // Clear existing pieces first (if any)
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                grid[f][r].removePiece();
            }
        }
        kingSquares.clear();

        // Place Pawns
        for (int file = 0; file < 8; file++) {
            placePiece(new Pawn(PieceColor.BLACK, getSquare(file, 6), RESOURCES_BPAWN_PNG));
            placePiece(new Pawn(PieceColor.WHITE, getSquare(file, 1), RESOURCES_WPAWN_PNG));
        }

        // Place Rooks
        placePiece(new Rook(PieceColor.BLACK, getSquare(0, 7), RESOURCES_BROOK_PNG));
        placePiece(new Rook(PieceColor.BLACK, getSquare(7, 7), RESOURCES_BROOK_PNG));
        placePiece(new Rook(PieceColor.WHITE, getSquare(0, 0), RESOURCES_WROOK_PNG));
        placePiece(new Rook(PieceColor.WHITE, getSquare(7, 0), RESOURCES_WROOK_PNG));

        // Place Knights
        placePiece(new Knight(PieceColor.BLACK, getSquare(1, 7), RESOURCES_BKNIGHT_PNG));
        placePiece(new Knight(PieceColor.BLACK, getSquare(6, 7), RESOURCES_BKNIGHT_PNG));
        placePiece(new Knight(PieceColor.WHITE, getSquare(1, 0), RESOURCES_WKNIGHT_PNG));
        placePiece(new Knight(PieceColor.WHITE, getSquare(6, 0), RESOURCES_WKNIGHT_PNG));

        // Place Bishops
        placePiece(new Bishop(PieceColor.BLACK, getSquare(2, 7), RESOURCES_BBISHOP_PNG));
        placePiece(new Bishop(PieceColor.BLACK, getSquare(5, 7), RESOURCES_BBISHOP_PNG));
        placePiece(new Bishop(PieceColor.WHITE, getSquare(2, 0), RESOURCES_WBISHOP_PNG));
        placePiece(new Bishop(PieceColor.WHITE, getSquare(5, 0), RESOURCES_WBISHOP_PNG));

        // Place Queens
        placePiece(new Queen(PieceColor.BLACK, getSquare(3, 7), RESOURCES_BQUEEN_PNG));
        placePiece(new Queen(PieceColor.WHITE, getSquare(3, 0), RESOURCES_WQUEEN_PNG));

        // Place Kings
        King bk = new King(PieceColor.BLACK, getSquare(4, 7), RESOURCES_BKING_PNG);
        King wk = new King(PieceColor.WHITE, getSquare(4, 0), RESOURCES_WKING_PNG);
        placePiece(bk);
        placePiece(wk);
        kingSquares.put(PieceColor.BLACK, bk.getPosition());
        kingSquares.put(PieceColor.WHITE, wk.getPosition());

    }


    /**
     * Helper method to place a piece on the board during initialization or setup.
     * Ensures the piece and square are consistent.
     * @param piece The piece to place.
     */
    private void placePiece(Piece piece) {
        if (piece != null && piece.getPosition() != null) {
            Square sq = piece.getPosition();
            grid[sq.getFile()][sq.getRank()].put(piece); // Use Square's put method
            if (piece.getType() == PieceType.KING) {
                kingSquares.put(piece.getColor(), sq);
            }
        }
    }


    /**
     * Gets the Square object at the given coordinates.
     * @param file The file index (0-7).
     * @param rank The rank index (0-7).
     * @return The Square object.
     * @throws IllegalArgumentException if coordinates are invalid.
     */
    public Square getSquare(int file, int rank) {
        if (!isValidCoordinate(file, rank)) {
            throw new IllegalArgumentException("Invalid coordinates: " + file + ", " + rank);
        }
        return grid[file][rank];
    }

    /**
     * Gets the Piece at the given coordinates.
     * @param file The file index (0-7).
     * @param rank The rank index (0-7).
     * @return The Piece at the coordinates, or null if the square is empty or coordinates invalid.
     */
    public Piece getPiece(int file, int rank) {
        if (!isValidCoordinate(file, rank)) {
            return null;
        }
        return grid[file][rank].getOccupyingPiece();
    }

    /**
     * Provides direct access to the internal grid. Use with caution.
     * Primarily for methods that need to iterate the board efficiently (like move generation).
     * @return The 8x8 grid of Squares.
     */
    public Square[][] getSquareGrid() {
        return grid;
    }

    /**
     * Gets the GameState associated with this board.
     * @return The GameState object.
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Gets the square currently occupied by the king of the specified color.
     * @param color The color of the king.
     * @return The Square where the king is located.
     */
    public Square getKingSquare(PieceColor color) {
        return kingSquares.get(color);
    }

    /**
     * Checks if the given file and rank coordinates are within the board boundaries.
     * @param file File index (0-7).
     * @param rank Rank index (0-7).
     * @return true if valid, false otherwise.
     */
    public static boolean isValidCoordinate(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    /**
     * Returns a list of all pieces currently on the board for a given color.
     * @param color The color of pieces to find.
     * @return A new List containing the pieces.
     */
    public List<Piece> getPieces(PieceColor color) {
        List<Piece> pieces = new ArrayList<>();
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = grid[f][r].getOccupyingPiece();
                if (p != null && p.getColor() == color) {
                    pieces.add(p);
                }
            }
        }
        return pieces;
    }

    /**
     * Returns a list of all pieces currently on the board.
     * @return A new List containing all pieces.
     */
    public List<Piece> getAllPieces() {
        List<Piece> pieces = new ArrayList<>();
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = grid[f][r].getOccupyingPiece();
                if (p != null) {
                    pieces.add(p);
                }
            }
        }
        return pieces;
    }


    /**
     * Applies a given validated move to the board state.
     * This method assumes the move has already been validated by GameLogic.
     * It handles piece movement, captures, castling rook movement,
     * en passant pawn removal, and promotion piece replacement.
     * It also updates the 'hasMoved' status for Kings, Rooks, and Pawns.
     *
     * IMPORTANT: This method modifies the board state directly.
     *
     * @param move The validated move to apply.
     */
    void applyMove(Move move) {
        Square start = move.getStartSquare();
        Square end = move.getEndSquare();
        Piece movedPiece = start.removePiece(); // Take piece from start square

        if (movedPiece == null) {
            System.err.println("Error applying move: No piece found at start square " + start);
            // In a real scenario, might throw an exception or handle more gracefully
            // Try to recover by getting the piece info from the Move object itself
            movedPiece = move.getPieceMoved();
            if(movedPiece == null) return; // Cannot proceed
        }

        // Handle captures (including En Passant)
        Piece capturedPiece = move.getPieceCaptured();
        if (move.isEnPassantCapture()) {
            // The captured pawn is *not* on the end square
            if(capturedPiece != null) {
                Square capturedPawnSquare = capturedPiece.getPosition();
                if (capturedPawnSquare != null) {
                    grid[capturedPawnSquare.getFile()][capturedPawnSquare.getRank()].removePiece();
                } else {
                    System.err.println("Error applying EP move: Captured piece has no position.");
                }
            } else {
                System.err.println("Error applying EP move: Captured piece in move object is null.");
            }
        } else if (move.isCapture() && capturedPiece != null) {
            // Standard capture: remove piece from the end square
            // The piece should already be identified in the move object.
            // Just ensure the square is cleared before placing the moved piece.
            end.removePiece();
        }

        // Place the moved piece on the end square
        end.put(movedPiece);

        // Update King's position tracker if King moved
        if (movedPiece.getType() == PieceType.KING) {
            kingSquares.put(movedPiece.getColor(), end);
            // Set King's hasMoved flag
            ((King) movedPiece).setHasMoved();
        }

        // Set Pawn's hasMoved flag
        if (movedPiece.getType() == PieceType.PAWN) {
            ((Pawn) movedPiece).setHasMoved();
        }

        // Set Rook's hasMoved flag
        if (movedPiece.getType() == PieceType.ROOK) {
            ((Rook) movedPiece).setHasMoved();
        }


        // Handle Castling Rook Movement
        if (move.isCastle()) {
            Square rookStart, rookEnd;
            if (move.isCastleKingside()) {
                // Move Kingside Rook (file 7)
                rookStart = getSquare(7, start.getRank()); // H1 or H8
                rookEnd = getSquare(5, start.getRank());   // F1 or F8
            } else { // Queenside
                // Move Queenside Rook (file 0)
                rookStart = getSquare(0, start.getRank()); // A1 or A8
                rookEnd = getSquare(3, start.getRank());   // D1 or D8
            }
            Piece rook = rookStart.removePiece();
            if (rook != null && rook.getType() == PieceType.ROOK) {
                rookEnd.put(rook);
                ((Rook) rook).setHasMoved(); // Mark rook as moved
            } else {
                System.err.println("Error applying castling: Rook not found at " + rookStart);
                // Attempt to recover? Might indicate corrupted state.
            }
        }

        // Handle Promotion
        if (move.isPromotion()) {
            PieceType promotionType = move.getPromotionType();
            Piece promotedPiece = createPromotedPiece(movedPiece.getColor(), end, promotionType);
            if (promotedPiece != null) {
                end.put(promotedPiece); // Replace the pawn with the new piece
            } else {
                System.err.println("Error applying promotion: Invalid promotion type " + promotionType);
                // Default to Queen? Or throw error? Let's default to Queen.
                end.put(createPromotedPiece(movedPiece.getColor(), end, PieceType.QUEEN));
            }
        }

        // Note: Updating GameState (turn, clocks, history, fifty-move, etc.)
        // is handled by GameLogic *after* calling applyMove.
    }

    /**
     * Creates the appropriate piece instance for a promotion.
     * @param color Color of the promoting pawn.
     * @param square The square where promotion occurs.
     * @param type The type to promote to (QUEEN, ROOK, BISHOP, KNIGHT).
     * @return The new Piece instance.
     */
    private Piece createPromotedPiece(PieceColor color, Square square, PieceType type) {
        String imgFile;
        switch (type) {
            case QUEEN:
                imgFile = (color == PieceColor.WHITE) ? RESOURCES_WQUEEN_PNG : RESOURCES_BQUEEN_PNG;
                return new Queen(color, square, imgFile);
            case ROOK:
                imgFile = (color == PieceColor.WHITE) ? RESOURCES_WROOK_PNG : RESOURCES_BROOK_PNG;
                Rook rook = new Rook(color, square, imgFile);
                rook.setHasMoved(); // Promoted rooks count as having moved
                return rook;
            case BISHOP:
                imgFile = (color == PieceColor.WHITE) ? RESOURCES_WBISHOP_PNG : RESOURCES_BBISHOP_PNG;
                return new Bishop(color, square, imgFile);
            case KNIGHT:
                imgFile = (color == PieceColor.WHITE) ? RESOURCES_WKNIGHT_PNG : RESOURCES_BKNIGHT_PNG;
                return new Knight(color, square, imgFile);
            default:
                return null; // Invalid promotion type
        }
    }


    /**
     * Creates a deep copy of the board and its associated GameState.
     * Necessary for move validation lookahead without modifying the actual game.
     *
     * @return A new Board object representing a copy of the current state.
     */
    public Board copy() {
        // 1. Copy the GameState
        GameState stateCopy = this.gameState.copy();

        // 2. Copy the Square grid and Pieces
        Square[][] gridCopy = new Square[8][8];
        Map<PieceColor, Square> kingSquaresCopy = new HashMap<>();

        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                gridCopy[f][r] = new Square(f, r); // Create new empty squares
                Piece originalPiece = this.grid[f][r].getOccupyingPiece();
                if (originalPiece != null) {
                    // Create a copy of the piece and place it on the new square
                    Piece pieceCopy = copyPiece(originalPiece, gridCopy[f][r]);
                    gridCopy[f][r].put(pieceCopy);
                    if (pieceCopy.getType() == PieceType.KING) {
                        kingSquaresCopy.put(pieceCopy.getColor(), gridCopy[f][r]);
                    }
                }
            }
        }

        // 3. Create the new Board instance
        Board boardCopy = new Board(gridCopy, stateCopy, kingSquaresCopy);
        // Ensure the GameState reference within the copied board points to the copied state
        boardCopy.gameState = stateCopy;

        return boardCopy;
    }

    /**
     * Helper method to create a copy of a Piece object.
     * @param original The piece to copy.
     * @param targetSquare The square the copied piece will occupy on the new board.
     * @return A new Piece instance that is a copy of the original.
     */
    private Piece copyPiece(Piece original, Square targetSquare) {
        PieceColor color = original.getColor();
        String imgFile = original.getImageFileName();
        Piece copy;

        switch (original.getType()) {
            case PAWN:
                Pawn originalPawn = (Pawn) original;
                Pawn pawnCopy = new Pawn(color, targetSquare, imgFile);
                if (originalPawn.hasMoved()) pawnCopy.setHasMoved();
                copy = pawnCopy;
                break;
            case ROOK:
                Rook originalRook = (Rook) original;
                Rook rookCopy = new Rook(color, targetSquare, imgFile);
                if (originalRook.hasMoved()) rookCopy.setHasMoved();
                copy = rookCopy;
                break;
            case KNIGHT:
                copy = new Knight(color, targetSquare, imgFile);
                break;
            case BISHOP:
                copy = new Bishop(color, targetSquare, imgFile);
                break;
            case QUEEN:
                copy = new Queen(color, targetSquare, imgFile);
                break;
            case KING:
                King originalKing = (King) original;
                King kingCopy = new King(color, targetSquare, imgFile);
                if (originalKing.hasMoved()) kingCopy.setHasMoved();
                copy = kingCopy;
                break;
            default:
                throw new IllegalStateException("Unknown piece type: " + original.getType());
        }
        // The targetSquare.put() in the main copy loop will set the position correctly.
        return copy;
    }


    /**
     * Calculates a simple hash code for the current board position.
     * This is NOT cryptographically secure or guaranteed unique like Zobrist hashing,
     * but can be used for basic repetition checks.
     * A proper implementation would use Zobrist hashing.
     *
     * For now, uses Objects.hash on piece placements and key game state aspects.
     * @return A long hash code representing the position.
     */
    public long calculateBoardHash() {
        // Simple hash based on piece placement and key state.
        // WARNING: Prone to collisions. Use Zobrist hashing for robust checks.
        int prime = 31;
        long result = 1;

        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                Piece p = grid[f][r].getOccupyingPiece();
                // Incorporate piece type, color, and position into hash
                result = prime * result + (p == null ? 0 : p.hashCode());
                // Also include square itself? No, piece hash includes square.
            }
        }

        // Include key game state elements that define the position for repetition rules
        result = prime * result + gameState.getCurrentPlayerTurn().hashCode();
        result = prime * result + Boolean.hashCode(gameState.canCastleKingside(PieceColor.WHITE));
        result = prime * result + Boolean.hashCode(gameState.canCastleQueenside(PieceColor.WHITE));
        result = prime * result + Boolean.hashCode(gameState.canCastleKingside(PieceColor.BLACK));
        result = prime * result + Boolean.hashCode(gameState.canCastleQueenside(PieceColor.BLACK));
        result = prime * result + Objects.hashCode(gameState.getEnPassantTargetSquare()); // Include EP target

        return result;
    }


    /**
     * Provides a simple text representation of the board, useful for debugging.
     * @return A String showing the board layout.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int r = 7; r >= 0; r--) {
            sb.append(r + 1).append(" ");
            for (int f = 0; f < 8; f++) {
                Piece p = grid[f][r].getOccupyingPiece();
                if (p == null) {
                    sb.append(((f + r) % 2 == 0) ? "." : "."); // Simple empty square marker
                } else {
                    char pieceChar;
                    switch (p.getType()) {
                        case PAWN: pieceChar = 'p'; break;
                        case ROOK: pieceChar = 'r'; break;
                        case KNIGHT: pieceChar = 'n'; break;
                        case BISHOP: pieceChar = 'b'; break;
                        case QUEEN: pieceChar = 'q'; break;
                        case KING: pieceChar = 'k'; break;
                        default: pieceChar = '?'; break;
                    }
                    sb.append(p.getColor() == PieceColor.WHITE ? Character.toUpperCase(pieceChar) : pieceChar);
                }
                sb.append(" ");
            }
            sb.append(r + 1).append("\n");
        }
        sb.append("  a b c d e f g h\n");
        sb.append("Turn: ").append(gameState.getCurrentPlayerTurn());
        sb.append(" | EP: ").append(gameState.getEnPassantTargetSquare());
        sb.append(" | Castling: ");
        sb.append(gameState.canCastleKingside(PieceColor.WHITE) ? "K" : "-");
        sb.append(gameState.canCastleQueenside(PieceColor.WHITE) ? "Q" : "-");
        sb.append(gameState.canCastleKingside(PieceColor.BLACK) ? "k" : "-");
        sb.append(gameState.canCastleQueenside(PieceColor.BLACK) ? "q" : "-");
        sb.append(" | 50-move: ").append(gameState.getHalfmoveClock());
        sb.append(" | Move: ").append(gameState.getFullmoveNumber());
        sb.append(" | Status: ").append(gameState.getGameStatus());
        sb.append("\n");
        return sb.toString();
    }

}