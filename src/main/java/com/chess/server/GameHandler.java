// In main.server.GameHandler.java
package com.chess.server;

import com.chess.common.Colour;
import com.chess.common.FenUtility;
import com.chess.common.Square;
import com.chess.model.Board.Board;
import com.chess.model.Clock;
import com.chess.PGNGenerator; // Assuming you have this class

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Manages the logic and communication for a single chess game between two
 * authenticated players. This class is the central "brain" for a game instance.
 */
public class GameHandler implements Runnable {

    // Player and game state fields
    private final ClientHandler whitePlayer;
    private final ClientHandler blackPlayer;
    private final Board board;
    private final List<String> moveHistory = new ArrayList<>();

    // Clocks and timing
    private final Clock whiteClock;
    private final Clock blackClock;
    private Timer gameTimer;
    private volatile boolean isGameOver = false;

    // Communication
    // A thread-safe queue to hold incoming messages from both clients.
    private final BlockingQueue<PlayerMessage> messageQueue = new LinkedBlockingQueue<>();

    // A simple record to associate an incoming message with the player who sent it.
    private record PlayerMessage(ClientHandler player, String message) {}

    /**
     * Constructor for the GameHandler.
     * @param player1 The first authenticated client.
     * @param player2 The second authenticated client.
     */
    public GameHandler(ClientHandler player1, ClientHandler player2) {
        // Randomly assign colors to the players.
        if (Math.random() > 0.5) {
            this.whitePlayer = player1;
            this.blackPlayer = player2;
        } else {
            this.whitePlayer = player2;
            this.blackPlayer = player1;
        }

        this.board = new Board();

        // Calculate the game time based on players' preferences.
        int gameTimeSeconds = (whitePlayer.getPreferredTime() + blackPlayer.getPreferredTime()) / 2;
        this.whiteClock = new Clock(gameTimeSeconds);
        this.blackClock = new Clock(gameTimeSeconds);
    }

    @Override
    public void run() {
        try {
            // Start two dedicated listener threads. Each one reads from one client's socket
            // and puts messages into our shared queue. This prevents blocking.
            Thread whiteListener = new Thread(new ClientMessageListener(whitePlayer));
            Thread blackListener = new Thread(new ClientMessageListener(blackPlayer));
            whiteListener.start();
            blackListener.start();

            // Send initial game information to clients.
            startGameSequence();

            // The main loop that processes turns until the game is over.
            mainGameLoop();

        } catch (InterruptedException e) {
            // This can happen if the queue.take() operation is interrupted.
            Thread.currentThread().interrupt(); // Preserve the interrupted status.
            System.err.println("GameHandler was interrupted.");
            endGame("Game interrupted by a server error.", "*");
        } finally {
            // This block ALWAYS runs when the run() method exits.
            // It's crucial for cleanup.
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            // Signal the ClientHandler threads that the game is over, so they can
            // terminate gracefully and close their sockets.
            whitePlayer.signalGameFinished();
            blackPlayer.signalGameFinished();
            System.out.println("Game between " + whitePlayer.getUsername() + " and " + blackPlayer.getUsername() + " has concluded.");
        }
    }

    /**
     * Informs clients about their opponent, color, and starts the game timers.
     */
    private void startGameSequence() {
        whitePlayer.sendMessage("ASSIGN_COLOR WHITE");
        blackPlayer.sendMessage("ASSIGN_COLOR BLACK");

        whitePlayer.sendMessage("OPPONENT_NAME " + blackPlayer.getUsername());
        blackPlayer.sendMessage("OPPONENT_NAME " + whitePlayer.getUsername());

        // Send the initial time state.
        broadcastMessage("GAME_START " + whiteClock.getTime() + " " + blackClock.getTime());

        startServerTimer();
    }

    /**
     * The core game loop. It alternates turns, handles incoming client messages from the
     * queue, and checks for game-ending conditions.
     */
    private void mainGameLoop() throws InterruptedException {
        while (!isGameOver) {
            broadcastState(); // Send the current board state to both players.

            // Check for checkmate or stalemate BEFORE waiting for a move.
            if (!board.hasAnyLegalMoves(board.getTurn())) {
                if (board.isInCheck(board.getTurn())) {
                    String winner = (board.getTurn() == Colour.WHITE) ? "Black" : "White";
                    endGame("Checkmate! " + winner + " wins.", winner.equals("White") ? "1-0" : "0-1");
                } else {
                    endGame("Stalemate! The game is a draw.", "1/2-1/2");
                }
                return; // Exit the loop since the game is over.
            }

            ClientHandler activePlayer;
            ClientHandler opponent;

            if (board.getTurn() == Colour.WHITE) {
                activePlayer = whitePlayer;
                opponent = blackPlayer;
            } else {
                activePlayer = blackPlayer;
                opponent = whitePlayer;
            }

            // This method will block until a valid move is received from the active player.
            handlePlayerTurn(activePlayer, opponent);
        }
    }

    /**
     * Handles all logic for a single player's turn, including processing commands
     * from the message queue until a valid move is made.
     */
    private void handlePlayerTurn(ClientHandler activePlayer, ClientHandler opponent) throws InterruptedException {
        if (board.getTurn() == Colour.WHITE) {
            whiteClock.start();
            blackClock.stop();
        } else {
            blackClock.start();
            whiteClock.stop();
        }
        activePlayer.sendMessage("YOUR_TURN");
        opponent.sendMessage("OPPONENT_TURN");

        // Loop indefinitely until a valid MOVE command is processed or a player disconnects.
        while (true) {
            PlayerMessage playerMessage = messageQueue.take(); // This blocks until a message arrives.
            String message = playerMessage.message();

            // Handle a player disconnecting during their turn.
            if (message.equals("PLAYER_DISCONNECTED")) {
                String winner = (playerMessage.player() == whitePlayer) ? "Black" : "White";
                endGame(playerMessage.player().getUsername() + " disconnected. " + winner + " wins.", winner.equals("White") ? "1-0" : "0-1");
                return;
            }

            // Ignore messages from the player whose turn it isn't.
            if (playerMessage.player() != activePlayer) {
                playerMessage.player().sendMessage("ERROR Not_your_turn");
                continue;
            }

            String[] parts = message.split(" ");
            String command = parts[0];

            if ("MOVE".equals(command)) {
                if (parts.length < 3) {
                    activePlayer.sendMessage("INVALID_MOVE Malformed_move_command");
                    continue;
                }
                Square start = Square.fromAlgebraic(parts[1]);
                Square end = Square.fromAlgebraic(parts[2]);
                Optional<String> promo = (parts.length > 3) ? Optional.of(parts[3]) : Optional.empty();

                if (board.isLegalMove(start, end)) {
                    String san = board.applyMove(start, end, promo);
                    moveHistory.add(san);
                    broadcastMessage("VALID_MOVE " + san);
                    break; // Valid move received, exit the loop to end the turn.
                } else {
                    activePlayer.sendMessage("INVALID_MOVE Move_is_not_legal");
                }
            } else if ("GET_LEGAL_MOVES".equals(command)) {
                if (parts.length < 2) continue;
                Square start = Square.fromAlgebraic(parts[1]);
                List<Square> legalMoves = board.getLegalMovesForPiece(start);
                String movesString = legalMoves.stream().map(Square::toString).collect(Collectors.joining(" "));
                activePlayer.sendMessage("LEGAL_MOVES " + movesString);
            }
        }
    }

    /**
     * A centralized method to end the game, stop timers, and notify clients.
     * This method is thread-safe.
     */
    private synchronized void endGame(String message, String result) {
        if (isGameOver) return; // Prevent this from being called multiple times.

        this.isGameOver = true;

        sendPgnToClients(result);
        broadcastMessage("GAME_OVER " + message.replace(" ", "_"));
    }

    /**
     * Starts the master server-side timer that decrements clocks and checks for timeout.
     */
    private void startServerTimer() {
        this.gameTimer = new Timer(true); // true = daemon thread
        this.gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isGameOver) {
                    gameTimer.cancel();
                    return;
                }

                boolean whiteTimedOut = whiteClock.decrement();
                boolean blackTimedOut = blackClock.decrement();

                broadcastMessage("UPDATE_TIME " + whiteClock.getTime() + " " + blackClock.getTime());

                if (whiteTimedOut) {
                    endGame("Time's up! Black wins.", "0-1");
                } else if (blackTimedOut) {
                    endGame("Time's up! White wins.", "1-0");
                }
            }
        }, 1000, 1000);
    }

    private void sendPgnToClients(String result) {
        String pgn = PGNGenerator.generate(whitePlayer.getUsername(), blackPlayer.getUsername(), result, moveHistory);
        String gameDate = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd").format(java.time.LocalDate.now());

        // This assumes you have a static saveGame method in DatabaseManager.
        // If DatabaseManager is not static, you'll need an instance.
        DatabaseManager.saveGame(whitePlayer.getUsername(), blackPlayer.getUsername(), result, pgn, gameDate);

        String pgnForTransport = pgn.replace("\n", "|");
        broadcastMessage("GAME_PGN:::" + pgnForTransport);
    }

    private void broadcastState() {
        String fen = FenUtility.toFen(board);
        broadcastMessage("UPDATE_STATE " + fen);
    }

    private void broadcastMessage(String message) {
        whitePlayer.sendMessage(message);
        blackPlayer.sendMessage(message);
    }

    /**
     * An inner class that runs on its own thread. Its sole purpose is to listen
     * for messages from a single client and add them to the GameHandler's blocking queue.
     */
    private class ClientMessageListener implements Runnable {
        private final ClientHandler clientHandler;
        private final BufferedReader in;

        ClientMessageListener(ClientHandler clientHandler) {
            this.clientHandler = clientHandler;
            this.in = clientHandler.getReader();
        }

        @Override
        public void run() {
            try {
                String message;
                // As long as the game isn't over and the client is connected...
                while (!isGameOver && (message = in.readLine()) != null) {
                    // ...put the received message into the shared queue for processing.
                    messageQueue.put(new PlayerMessage(clientHandler, message));
                }
            } catch (IOException e) {
                // This happens if the client disconnects (e.g., closes the window).
                if (!isGameOver) {
                    System.out.println("Player " + clientHandler.getUsername() + " has disconnected.");
                    // Put a special message in the queue to signal the disconnect.
                    try {
                        messageQueue.put(new PlayerMessage(clientHandler, "PLAYER_DISCONNECTED"));
                    } catch (InterruptedException ex) { /* This thread is ending anyway */ }
                }
            } catch (InterruptedException e) {
                // This happens if the main game thread is interrupted.
                Thread.currentThread().interrupt();
            }
        }
    }
}