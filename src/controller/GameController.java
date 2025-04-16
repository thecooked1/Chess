package controller;

import model.*;
import view.ChessView;

import javax.swing.Timer; // Use Swing Timer for GUI updates
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class GameController {

    private final GameLogic gameLogic; // Model Logic
    private final ChessView chessView; // View
    private Timer gameTimer; // Timer for player clocks

    public GameController(GameLogic logic, ChessView view) {
        this.gameLogic = logic;
        this.chessView = view;
        initializeTimer();
    }

    private void initializeTimer() {
        // Timer updates every second (1000 ms)
        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameLogic.getGameState().isGameOver()) {
                    gameTimer.stop();
                    return;
                }

                GameState gameState = gameLogic.getGameState();
                PieceColor currentPlayer = gameState.getCurrentPlayerTurn();
                Clock currentClock = gameState.getClock(currentPlayer);

                if (currentClock.isRunning()) {
                    currentClock.decrementSecond();
                    chessView.updateClocks(); // Update only clock display

                    if (currentClock.isOutOfTime()) {
                        gameTimer.stop();
                        // Model determines game status based on time out
                        // Set the status in the model
                        GameState.GameStatus newStatus = (currentPlayer == PieceColor.WHITE)
                                ? GameState.GameStatus.BLACK_WINS_TIME // White ran out, Black wins
                                : GameState.GameStatus.WHITE_WINS_TIME; // Black ran out, White wins
                        gameState.setGameStatus(newStatus);

                        // Update the entire view to show game over message
                        chessView.updateView();
                    }
                }
            }
        });

        // Start timer only if game is timed
        GameState gs = gameLogic.getGameState();
        if (gs.getClock(PieceColor.WHITE).getTotalSeconds() > 0 ||
                gs.getClock(PieceColor.BLACK).getTotalSeconds() > 0) {
            // Start clock for White initially if timer exists
            if (gs.getCurrentPlayerTurn() == PieceColor.WHITE && gs.getClock(PieceColor.WHITE).getTotalSeconds() > 0) {
                gs.getClock(PieceColor.WHITE).start();
            }
            // Start the Swing timer
            gameTimer.start();
        }
    }

    public void stopTimers() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
        // Also stop model clocks
        gameLogic.getGameState().getClock(PieceColor.WHITE).stop();
        gameLogic.getGameState().getClock(PieceColor.BLACK).stop();
    }


    /**
     * Handles the user attempting to make a move by releasing the mouse.
     *
     * @param startSquare The square where the piece was picked up.
     * @param endSquare   The square where the piece was dropped.
     */
    public void handleMoveAttempt(Square startSquare, Square endSquare) {
        if (gameLogic.getGameState().isGameOver()) return;

        Piece piece = startSquare.getOccupyingPiece();
        if (piece == null) return; // Should not happen if logic is correct

        PieceType promotionType = null;

        // Check if this is a potential promotion move
        if (piece.getType() == PieceType.PAWN) {
            int promotionRank = (piece.getColor() == PieceColor.WHITE) ? 7 : 0;
            if (endSquare.getRank() == promotionRank) {
                // Check if this specific move (start->end) is potentially legal
                // We need to generate potential moves first to see if endSquare is reachable
                List<Move> potentialMovesFromLogic = gameLogic.getLegalMovesForPiece(piece); // Use legal moves check
                boolean isPromotionPossible = false;
                for (Move m : potentialMovesFromLogic) {
                    if (m.getEndSquare().equals(endSquare) && m.isPromotion()) {
                        isPromotionPossible = true;
                        break;
                    }
                }

                if (isPromotionPossible) {
                    promotionType = chessView.getPromotionChoice(piece.getColor());
                    if (promotionType == null) {
                        // User cancelled promotion, cancel move
                        chessView.updateView(); // Restore board visually
                        return;
                    }
                }
            }
        }

        // Attempt to make the move in the model
        boolean moveMade = gameLogic.makeMove(startSquare, endSquare, promotionType);

        // Update the view regardless of success to show result or revert visual state
        chessView.updateView();

        // If move was successful, the game state (turn, status) changed,
        // and the view update will reflect that.
    }

    /**
     * Gets the list of legal destination squares for a given piece.
     * Used by the view (BoardPanel) to highlight moves.
     *
     * @param piece The piece selected by the user.
     * @return A list of Squares the piece can legally move to.
     */
    public List<Square> getLegalMovesForPiece(Piece piece) {
        if (piece == null || piece.getColor() != gameLogic.getGameState().getCurrentPlayerTurn()) {
            return List.of(); // Return empty list if invalid piece or not their turn
        }
        // Convert List<Move> to List<Square> (destination squares)
        return gameLogic.getLegalMovesForPiece(piece)
                .stream()
                .map(Move::getEndSquare)
                .distinct() // Avoid duplicates if multiple moves end on same square (e.g., promotion)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Handles a player resigning.
     * @param resigningPlayer The color of the player who resigned.
     */
    public void handleResignation(PieceColor resigningPlayer) {
        if (gameLogic.getGameState().isGameOver()) return;

        GameState.GameStatus newStatus = (resigningPlayer == PieceColor.WHITE)
                ? GameState.GameStatus.BLACK_WINS_RESIGNATION // White resigns, Black wins
                : GameState.GameStatus.WHITE_WINS_RESIGNATION; // Black resigns, White wins

        gameLogic.getGameState().setGameStatus(newStatus);
        stopTimers(); // Stop clocks on resignation
        chessView.updateView(); // Update view to show result
    }

    // Add methods here later for handling draw offers, new game requests etc.
    // These would interact with gameLogic and update the view.
}