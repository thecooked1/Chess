package main.model;

import main.model.Piece;
import main.model.PieceType;

import java.util.List;
import java.util.Optional;

public class GameLogic {

    private GameState gameState;

    public GameLogic() {
        this.gameState = new GameState();
    }

    public GameState getGameState() {
        return gameState;
    }

    /**
     * Attempts to make a move. Validates the move first.
     *
     * @param move The move to attempt.
     * @return true if the move was successful, false otherwise.
     */
    public boolean makeMove(Move move) {
        if (isValidMove(move)) {
            gameState.applyMoveUnsafe(move); // Apply the validated move
            // Reset en passant possibilities for the next turn *after* applying the move.
            // The applyMoveUnsafe method should handle setting the *new* en passant target
            // if applicable, but we clear the previous one here conceptually for the next player.
            // (Correction: applyMoveUnsafe handles setting/clearing EP target based on the move made)
            return true;
        }
        return false;
    }

    /**
     * Checks if a move is valid according to game rules for the current player.
     *
     * @param move The move to check.
     * @return true if the move is valid, false otherwise.
     */
    public boolean isValidMove(Move move) {
        Piece piece = gameState.getBoard().getPieceAt(move.getStartPosition());

        // Basic checks
        if (piece == null) return false; // No piece at start position
        if (piece.getColor() != gameState.getCurrentPlayer()) return false; // Not player's piece

        // Check if the move is among the piece's legal moves
        List<Move> legalMoves = getLegalMovesForPiece(move.getStartPosition());

        // Need to handle promotion equality correctly
        if (move.isPromotion()) {
            // Check if a move to the same end square exists with *any* promotion
            boolean promotionPossible = legalMoves.stream()
                    .anyMatch(lm -> lm.getEndPosition().equals(move.getEndPosition()) && lm.isPromotion());
            if (!promotionPossible) return false;

            // Now check if the *specific* promotion move is valid (it should be if any promo is)
            return legalMoves.contains(move);
        } else {
            // Standard move check
            return legalMoves.contains(move);
        }
    }

    /**
     * Gets all legal moves for the piece at the specified position.
     *
     * @param position The position of the piece.
     * @return A list of legal moves, or an empty list if no piece or no legal moves.
     */
    public List<Move> getLegalMovesForPiece(Position position) {
        Piece piece = gameState.getBoard().getPieceAt(position);
        if (piece == null || piece.getColor() != gameState.getCurrentPlayer()) {
            return List.of(); // Return empty list if no piece or not current player's piece
        }

        // Generate all legal moves for the current player and filter for the specific piece
        List<Move> allLegalMoves = gameState.generateAllLegalMoves(gameState.getCurrentPlayer());
        return allLegalMoves.stream()
                .filter(move -> move.getStartPosition().equals(position))
                .toList(); // Use toList() for Java 16+, or collect(Collectors.toList())
    }

    // --- Getters for game status ---
    public boolean isCheck() {
        return gameState.isCheck(gameState.getCurrentPlayer());
    }

    public boolean isCheckmate() {
        return gameState.isCheckmate(gameState.getCurrentPlayer());
    }

    public boolean isStalemate() {
        return gameState.isStalemate(gameState.getCurrentPlayer());
    }

    public Color getCurrentPlayerColor() {
        return gameState.getCurrentPlayer();
    }

    public Optional<Color> getWinner() {
        if (isCheckmate()) {
            // The winner is the player whose turn it ISN'T
            return Optional.of(gameState.getCurrentPlayer() == Color.WHITE ? Color.BLACK : Color.WHITE);
        }
        // Add other win/draw conditions (stalemate, insufficient material, etc.)
        return Optional.empty();
    }

    public boolean isGameOver() {
        return isCheckmate() || isStalemate(); // Add other draw conditions
    }

    // Optional: Reset the game to the initial state
    public void resetGame() {
        this.gameState = new GameState();
    }
}