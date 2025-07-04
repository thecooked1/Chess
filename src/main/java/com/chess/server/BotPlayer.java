package com.chess.server;

import com.chess.common.Colour;
import com.chess.common.Square;
import com.chess.model.Board.Board;
import com.chess.model.pieces.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 * Represents an AI opponent. It listens for its turn and submits a move to the GameHandler.
 */
public class BotPlayer implements Runnable {
    private final Colour color;
    private final GameHandler gameHandler;
    private final Board board;
    private final Random random = new Random();

    public BotPlayer(Colour color, GameHandler gameHandler, Board board) {
        this.color = color;
        this.gameHandler = gameHandler;
        this.board = board;
    }

    @Override
    public void run() {
        // The bot's main loop is simple: wait until the game tells it to make a move.
        // It doesn't need to read from a socket.
        // We will call a method on it directly from the GameHandler.
    }

    /**
     * This method is called by the GameHandler when it is the bot's turn to move.
     */
    public void makeMove() {
        // To make it feel a bit more human, let's add a small delay.
        try {
            Thread.sleep(1000); // Wait for 1 second before making a move.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // --- Simple "Random Mover" AI Logic ---
        List<Square> movablePieceSquares = new ArrayList<>();

        // 1. Find all the bot's pieces that have at least one legal move.
        // We do this by iterating through every square on the board.
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Square currentSquare = new Square(rank, file);
                Piece piece = board.getPiece(currentSquare);

                // Check if a piece exists on this square and belongs to the bot
                if (piece != null && piece.getColor() == this.color) {
                    // Check if this piece has any legal moves
                    if (!board.getLegalMovesForPiece(currentSquare).isEmpty()) {
                        movablePieceSquares.add(currentSquare);
                    }
                }
            }
        }

        if (movablePieceSquares.isEmpty()) {
            // This case means no legal moves are possible (checkmate or stalemate).
            // The GameHandler's main loop will catch this. We do nothing.
            System.out.println("BOT (" + color + "): No legal moves found.");
            return;
        }

        // 2. Pick a random piece to move from the list of movable pieces.
        Square startSquare = movablePieceSquares.get(random.nextInt(movablePieceSquares.size()));

        // 3. Get the legal destination squares for that chosen piece.
        List<Square> legalDestinations = board.getLegalMovesForPiece(startSquare);

        // 4. Pick a random destination square.
        Square endSquare = legalDestinations.get(random.nextInt(legalDestinations.size()));

        // 5. Construct the move command string.
        String moveCommand = "MOVE " + startSquare.toString() + " " + endSquare.toString();

        // Handle pawn promotion (defaults to Queen for simplicity).
        Piece movingPiece = board.getPiece(startSquare);
        if (movingPiece.getSymbol() == 'P' && (endSquare.rank() == 0 || endSquare.rank() == 7)) {
            moveCommand += " Q";
        }

        // 6. Submit the move to the GameHandler.
        System.out.println("BOT (" + color + "): Submitting move: " + moveCommand);
        gameHandler.submitBotMove(moveCommand);
    }

    public String getUsername() {
        return "Chess Bot";
    }

    public Colour getColor() {
        return color;
    }
}