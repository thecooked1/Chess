package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the state of the chess game, including whose turn it is,
 * castling rights, en passant target, move history, clocks, and game status.
 */
public class GameState {

    public enum GameStatus {
        ONGOING,
        WHITE_WINS_CHECKMATE, BLACK_WINS_CHECKMATE,
        WHITE_WINS_TIME, BLACK_WINS_TIME,
        WHITE_WINS_RESIGNATION, BLACK_WINS_RESIGNATION,
        STALEMATE,
        DRAW_INSUFFICIENT_MATERIAL,
        DRAW_FIFTY_MOVE_RULE,
        DRAW_THREEFOLD_REPETITION,
        DRAW_AGREEMENT
    }

    private PieceColor currentPlayerTurn;
    private boolean whiteCanCastleKingside;
    private boolean whiteCanCastleQueenside;
    private boolean blackCanCastleKingside;
    private boolean blackCanCastleQueenside;
    private Square enPassantTargetSquare; // Square *behind* the pawn that moved double
    private int halfmoveClock; // For fifty-move rule
    private int fullmoveNumber; // Increments after Black moves
    private GameStatus gameStatus;
    private Clock whiteClock;
    private Clock blackClock;
    private final List<Move> moveHistory; // Optional: for history tracking/repetition checks

    // Used for threefold repetition check
    private final List<Long> positionHistoryHashes; // Store Zobrist hashes or similar board representations


    public GameState(int clockHours, int clockMinutes, int clockSeconds) {
        this.currentPlayerTurn = PieceColor.WHITE;
        this.whiteCanCastleKingside = true;
        this.whiteCanCastleQueenside = true;
        this.blackCanCastleKingside = true;
        this.blackCanCastleQueenside = true;
        this.enPassantTargetSquare = null;
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
        this.gameStatus = GameStatus.ONGOING;
        this.whiteClock = new Clock(clockHours, clockMinutes, clockSeconds);
        this.blackClock = new Clock(clockHours, clockMinutes, clockSeconds);
        this.moveHistory = new ArrayList<>();
        this.positionHistoryHashes = new ArrayList<>();
        // Start the clock for the first player if time > 0
        if (whiteClock.getTotalSeconds() > 0) {
            whiteClock.start();
        }
    }

    // --- Getters ---

    public PieceColor getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }

    public boolean canCastleKingside(PieceColor color) {
        return (color == PieceColor.WHITE) ? whiteCanCastleKingside : blackCanCastleKingside;
    }

    public boolean canCastleQueenside(PieceColor color) {
        return (color == PieceColor.WHITE) ? whiteCanCastleQueenside : blackCanCastleQueenside;
    }

    public Square getEnPassantTargetSquare() {
        return enPassantTargetSquare;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public boolean isGameOver() {
        return gameStatus != GameStatus.ONGOING;
    }

    public Clock getClock(PieceColor color) {
        return (color == PieceColor.WHITE) ? whiteClock : blackClock;
    }

    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory); // Return copy
    }

    public Move getLastMove() {
        return moveHistory.isEmpty() ? null : moveHistory.get(moveHistory.size() - 1);
    }


    // --- Mutators (typically called by GameLogic after a move) ---

    /**
     * Switches the current player turn and manages clocks.
     */
    void switchTurn() {
        Clock previousPlayerClock = getClock(currentPlayerTurn);
        previousPlayerClock.stop();

        currentPlayerTurn = currentPlayerTurn.opposite();

        Clock nextPlayerClock = getClock(currentPlayerTurn);
        if (nextPlayerClock.getTotalSeconds() > 0 && !isGameOver()) {
            nextPlayerClock.start();
        }

        if (currentPlayerTurn == PieceColor.WHITE) {
            fullmoveNumber++; // Increment full move number after Black moves
        }
    }

    /**
     * Updates the game state after a move has been made.
     * This is the primary method for advancing the game state.
     *
     * @param move The move that was just made.
     * @param boardHash A hash representing the board state *after* the move.
     */
    void recordMove(Move move, long boardHash) {
        Piece movedPiece = move.getPieceMoved();
        Square startSquare = move.getStartSquare();
        Square endSquare = move.getEndSquare();

        // 1. Reset En Passant Target
        // EP target is only valid for one turn immediately after the double push
        this.enPassantTargetSquare = null;

        // 2. Set New En Passant Target (if pawn double moved)
        if (movedPiece.getType() == PieceType.PAWN && Math.abs(startSquare.getRank() - endSquare.getRank()) == 2) {
            int targetRank = (movedPiece.getColor() == PieceColor.WHITE) ? startSquare.getRank() + 1 : startSquare.getRank() - 1;
            // Need Board context to get the actual Square object - Assume GameLogic passes it in?
            // For now, just store coordinates - GameLogic needs to resolve to Square if needed.
            // Let's refine this: GameLogic should create the Square object for the target.
            // We assume the Square object IS passed correctly if applicable
            // Example: If white pawn moves e2->e4, target is e3.
            // This 'targetSquare' should be created based on file=endSquare.getFile(), rank=targetRank
            // This requires the Board context, so perhaps GameLogic should calculate and set this.
            // **Decision:** Let GameLogic calculate the EP square object and pass it here.
            // For now, we clear it, and assume GameLogic sets it below if needed.
        }

        // 3. Update Castling Rights
        // If King moved
        if (movedPiece.getType() == PieceType.KING) {
            if (movedPiece.getColor() == PieceColor.WHITE) {
                whiteCanCastleKingside = false;
                whiteCanCastleQueenside = false;
            } else {
                blackCanCastleKingside = false;
                blackCanCastleQueenside = false;
            }
        }
        // If Rook moved (or was captured)
        if (movedPiece.getType() == PieceType.ROOK) {
            updateCastlingRightsForRookMove(movedPiece.getColor(), startSquare);
        }
        if (move.isCapture() && move.getPieceCaptured().getType() == PieceType.ROOK) {
            // If a rook is captured on its starting square, castling rights are lost
            updateCastlingRightsForRookMove(move.getPieceCaptured().getColor(), endSquare); // Check capture square
        }

        // 4. Update Halfmove Clock
        if (movedPiece.getType() == PieceType.PAWN || move.isCapture()) {
            halfmoveClock = 0; // Reset on pawn move or capture
        } else {
            halfmoveClock++;
        }

        // 5. Add move to history
        moveHistory.add(move);

        // 6. Add position hash to history
        positionHistoryHashes.add(boardHash);


        // 7. Switch turn (and update fullmove counter if needed)
        switchTurn();

        // 8. Check for game end conditions (Checkmate, Stalemate, Draw rules)
        // This should be done by GameLogic *after* calling recordMove and potentially setting EP target.
    }


    /**
     * Explicitly sets the en passant target square. Called by GameLogic.
     * @param target The square behind the pawn that just double-pushed, or null.
     */
    void setEnPassantTargetSquare(Square target) {
        this.enPassantTargetSquare = target;
    }

    /**
     * Helper to update castling rights when a rook might have moved from its starting square.
     * @param color Color of the rook
     * @param rookSquare The square the rook moved FROM (or was captured ON)
     */
    private void updateCastlingRightsForRookMove(PieceColor color, Square rookSquare) {
        int rank = (color == PieceColor.WHITE) ? 0 : 7;
        if (rookSquare.getRank() == rank) {
            if (rookSquare.getFile() == 0) { // Queenside Rook
                if (color == PieceColor.WHITE) whiteCanCastleQueenside = false;
                else blackCanCastleQueenside = false;
            } else if (rookSquare.getFile() == 7) { // Kingside Rook
                if (color == PieceColor.WHITE) whiteCanCastleKingside = false;
                else blackCanCastleKingside = false;
            }
        }
    }

    /**
     * Sets the game status (e.g., to checkmate, stalemate, etc.). Stops clocks.
     * @param status The new GameStatus.
     */
    public void setGameStatus(GameStatus status) {
        if (this.gameStatus == GameStatus.ONGOING) { // Only set if not already ended
            this.gameStatus = status;
            whiteClock.stop();
            blackClock.stop();
        }
    }

    /**
     * Checks if the current board position (represented by its hash) has occurred
     * three or more times.
     * @param currentBoardHash The hash of the current board position.
     * @return True if the position is a threefold repetition, false otherwise.
     */
    boolean isThreefoldRepetition(long currentBoardHash) {
        int count = 0;
        // Include the current position hash in the check
        if (currentBoardHash != 0) { // Assuming 0 is not a valid hash
            for (long hash : positionHistoryHashes) {
                if (hash == currentBoardHash) {
                    count++;
                }
            }
            // The check occurs *before* adding the current hash for the *next* move,
            // so we check if the hash already exists count >= 2 times in the history,
            // meaning the *current* state makes the 3rd occurrence.
            // If we check *after* adding, we look for count >= 3.
            // Let's assume we check *before* adding the current state hash.
            // **Correction:** The hash list *does* include the latest move's result.
            // So we check if the count is >= 3.
            return count >= 3;
        }
        return false; // Cannot determine repetition without a valid hash
    }


    /**
     * Checks if the fifty-move rule applies (100 half-moves).
     * @return True if the rule applies, false otherwise.
     */
    boolean isFiftyMoveRule() {
        // 50 moves by each player = 100 half-moves
        return halfmoveClock >= 100;
    }

    /**
     * Creates a deep copy of the game state. Useful for move validation simulation.
     * @return A new GameState object with the same values.
     */
    public GameState copy() {
        GameState copy = new GameState(0, 0, 0); // Clocks will be copied below
        copy.currentPlayerTurn = this.currentPlayerTurn;
        copy.whiteCanCastleKingside = this.whiteCanCastleKingside;
        copy.whiteCanCastleQueenside = this.whiteCanCastleQueenside;
        copy.blackCanCastleKingside = this.blackCanCastleKingside;
        copy.blackCanCastleQueenside = this.blackCanCastleQueenside;
        copy.enPassantTargetSquare = this.enPassantTargetSquare; // Square objects are immutable enough for shallow copy
        copy.halfmoveClock = this.halfmoveClock;
        copy.fullmoveNumber = this.fullmoveNumber;
        copy.gameStatus = this.gameStatus; // Status enum is fine

        // Copy clocks (create new instances)
        copy.whiteClock = new Clock(0, 0, 0);
        copy.whiteClock.setTime(this.whiteClock.hh, this.whiteClock.mm, this.whiteClock.ss);
        if(this.whiteClock.isRunning()) copy.whiteClock.start(); else copy.whiteClock.stop();

        copy.blackClock = new Clock(0, 0, 0);
        copy.blackClock.setTime(this.blackClock.hh, this.blackClock.mm, this.blackClock.ss);
        if(this.blackClock.isRunning()) copy.blackClock.start(); else copy.blackClock.stop();

        // Copy move history (shallow copy of Move objects is usually okay unless Move becomes mutable)
        copy.moveHistory.addAll(this.moveHistory);

        // Copy position history hashes
        copy.positionHistoryHashes.addAll(this.positionHistoryHashes);


        return copy;
    }

    // hashCode and equals can be useful for state comparison, especially with history hashes
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState = (GameState) o;
        // Compare critical state components. Clocks and full history might not be needed for functional equality.
        return whiteCanCastleKingside == gameState.whiteCanCastleKingside &&
                whiteCanCastleQueenside == gameState.whiteCanCastleQueenside &&
                blackCanCastleKingside == gameState.blackCanCastleKingside &&
                blackCanCastleQueenside == gameState.blackCanCastleQueenside &&
                halfmoveClock == gameState.halfmoveClock &&
                currentPlayerTurn == gameState.currentPlayerTurn &&
                Objects.equals(enPassantTargetSquare, gameState.enPassantTargetSquare);
        // Note: Comparing board state requires comparing the Board object itself or its hash.
    }

    @Override
    public int hashCode() {
        // Hash based on critical state components.
        return Objects.hash(currentPlayerTurn, whiteCanCastleKingside, whiteCanCastleQueenside, blackCanCastleKingside, blackCanCastleQueenside, enPassantTargetSquare, halfmoveClock);
        // Note: A full state hash should incorporate the board position (e.g., Zobrist hash).
    }
}