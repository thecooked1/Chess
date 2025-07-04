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

    // --- Player and Game State Fields ---
    private final ClientHandler humanPlayer1;
    private final ClientHandler humanPlayer2; // Is NULL if it's a bot game
    private final BotPlayer botPlayer;       // Is NULL if it's a human vs human game
    private final Board board;
    private final List<String> moveHistory = new ArrayList<>();

    // --- Correctly Scoped Player Information ---
    private final String whitePlayerName;
    private final String blackPlayerName;
    private final ClientHandler whiteHumanHandler; // The human client playing white (can be null)
    private final ClientHandler blackHumanHandler;

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

    // --- CONSTRUCTOR FOR HUMAN vs HUMAN ---
    public GameHandler(ClientHandler player1, ClientHandler player2) {
        this.humanPlayer1 = player1;
        this.humanPlayer2 = player2;
        this.botPlayer = null; // No bot in this game.
        this.board = new Board();

        // Randomly assign colors
        if (Math.random() > 0.5) {
            this.whiteHumanHandler = player1;
            this.blackHumanHandler = player2;
        } else {
            this.whiteHumanHandler = player2;
            this.blackHumanHandler = player1;
        }
        this.whitePlayerName = whiteHumanHandler.getUsername();
        this.blackPlayerName = blackHumanHandler.getUsername();

        int gameTime = (player1.getPreferredTime() + player2.getPreferredTime()) / 2;
        this.whiteClock = new Clock(gameTime);
        this.blackClock = new Clock(gameTime);
    }

    // --- CONSTRUCTOR FOR HUMAN vs BOT ---
    public GameHandler(ClientHandler humanPlayer) {
        this.humanPlayer1 = humanPlayer;
        this.humanPlayer2 = null; // No second human player.
        this.board = new Board();

        // Randomly assign color to the human
        if (Math.random() > 0.5) {
            this.whiteHumanHandler = humanPlayer;
            this.blackHumanHandler = null;
            this.botPlayer = new BotPlayer(Colour.BLACK, this, this.board);
            this.whitePlayerName = humanPlayer.getUsername();
            this.blackPlayerName = botPlayer.getUsername();
        } else {
            this.blackHumanHandler = humanPlayer;
            this.whiteHumanHandler = null;
            this.botPlayer = new BotPlayer(Colour.WHITE, this, this.board);
            this.whitePlayerName = botPlayer.getUsername();
            this.blackPlayerName = humanPlayer.getUsername();
        }

        int gameTime = humanPlayer.getPreferredTime();
        this.whiteClock = new Clock(gameTime);
        this.blackClock = new Clock(gameTime);
    }

    @Override
    public void run() {
        try {
            // Start listener threads for all human players in the game.
            if (whiteHumanHandler != null) new Thread(new ClientMessageListener(whiteHumanHandler)).start();
            if (blackHumanHandler != null) new Thread(new ClientMessageListener(blackHumanHandler)).start();

            startGameSequence();
            mainGameLoop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            endGame("Game interrupted by a server error.", "*");
        } finally {
            if (gameTimer != null) gameTimer.cancel();

            // Signal all human players that the game is over.
            if (whiteHumanHandler != null) whiteHumanHandler.signalGameFinished();
            if (blackHumanHandler != null) blackHumanHandler.signalGameFinished();

            System.out.println("Game concluded: " + whitePlayerName + " vs " + blackPlayerName);
        }
    }

    private void startGameSequence() {
        if (whiteHumanHandler != null) whiteHumanHandler.sendMessage("ASSIGN_COLOR WHITE");
        if (blackHumanHandler != null) blackHumanHandler.sendMessage("ASSIGN_COLOR BLACK");

        if (whiteHumanHandler != null) whiteHumanHandler.sendMessage("OPPONENT_NAME " + blackPlayerName);
        if (blackHumanHandler != null) blackHumanHandler.sendMessage("OPPONENT_NAME " + whitePlayerName);

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

            boolean isWhiteTurn = board.getTurn() == Colour.WHITE;

            // Determine if the current turn belongs to a bot.
            boolean isBotTurn = (isWhiteTurn && botPlayer != null && botPlayer.getColor() == Colour.WHITE) ||
                    (!isWhiteTurn && botPlayer != null && botPlayer.getColor() == Colour.BLACK);

            if (isBotTurn) {
                // If it's the bot's turn, tell it to make a move.
                botPlayer.makeMove();
                // The bot move will be submitted via submitBotMove, so we just wait for it.
                // We need to process the move before the loop continues.
                // For simplicity, we assume the bot move processing is quick and happens
                // before the next iteration of the game loop. A more complex system might
                // use another latch here, but this should work.
            } else {
                // If it's a human's turn, wait for their message.
                ClientHandler activeHuman = isWhiteTurn ? whiteHumanHandler : blackHumanHandler;
                handlePlayerTurn(activeHuman);
            }
        }
    }

    /** This is called by the BotPlayer to submit its move. */
    public synchronized void submitBotMove(String moveCommand) {
        // This runs on the Bot's thread, so we make it synchronized to be safe.
        String[] parts = moveCommand.split(" ");
        Square start = Square.fromAlgebraic(parts[1]);
        Square end = Square.fromAlgebraic(parts[2]);
        Optional<String> promo = (parts.length > 3) ? Optional.of(parts[3]) : Optional.empty();

        if (board.isLegalMove(start, end)) {
            String san = board.applyMove(start, end, promo);
            moveHistory.add(san);
            broadcastMessage("VALID_MOVE " + san);
        } else {
            System.err.println("CRITICAL: Bot attempted an illegal move: " + moveCommand);
            // In a real system, you might force the bot to try again or forfeit.
        }
    }

    /**
     * Handles all logic for a single player's turn, including processing commands
     * from the message queue until a valid move is made.
     */
    private void handlePlayerTurn(ClientHandler activeHuman) throws InterruptedException {
        if (board.getTurn() == Colour.WHITE) {
            whiteClock.start();
            blackClock.stop();
        } else {
            blackClock.start();
            whiteClock.stop();
        }
        activeHuman.sendMessage("YOUR_TURN");
        // Tell the other player(s) it's the opponent's turn
        if (botPlayer == null) { // H-vs-H game
            ClientHandler opponent = (activeHuman == whiteHumanHandler) ? blackHumanHandler : whiteHumanHandler;
            opponent.sendMessage("OPPONENT_TURN");
        } // In H-vs-Bot, there's no other human to notify.

        // Loop indefinitely until a valid MOVE command is processed or a player disconnects.
        while (true) {
            PlayerMessage playerMessage = messageQueue.take(); // This blocks until a message arrives.
            if (playerMessage.player() != activeHuman) {
                playerMessage.player().sendMessage("ERROR Not_your_turn");
                continue;
            }
            String message = playerMessage.message();

            // Handle a player disconnecting during their turn.
            if (message.equals("PLAYER_DISCONNECTED")) {
                String winner = (playerMessage.player() == whiteHumanHandler) ? "Black" : "White";
                endGame(playerMessage.player().getUsername() + " disconnected. " + winner + " wins.", winner.equals("White") ? "1-0" : "0-1");
                return;
            }

            // Ignore messages from the player whose turn it isn't.
            if (playerMessage.player() != activeHuman) {
                playerMessage.player().sendMessage("ERROR Not_your_turn");
                continue;
            }

            String[] parts = message.split(" ");
            String command = parts[0];

            if ("MOVE".equals(command)) {
                if (parts.length < 3) {
                    activeHuman.sendMessage("INVALID_MOVE Malformed_move_command");
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
                    activeHuman.sendMessage("INVALID_MOVE Move_is_not_legal");
                }
            } else if ("GET_LEGAL_MOVES".equals(command)) {
                if (parts.length < 2) continue;
                Square start = Square.fromAlgebraic(parts[1]);
                List<Square> legalMoves = board.getLegalMovesForPiece(start);
                String movesString = legalMoves.stream().map(Square::toString).collect(Collectors.joining(" "));
                activeHuman.sendMessage("LEGAL_MOVES " + movesString);
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
        String pgn = PGNGenerator.generate(this.whitePlayerName, this.blackPlayerName, result, moveHistory);
        String gameDate = java.time.format.DateTimeFormatter
                .ofPattern("yyyy.MM.dd").format(java.time.LocalDate.now());

        DatabaseManager.saveGame(this.whitePlayerName, this.blackPlayerName, result, pgn, gameDate);

        String pgnForTransport = pgn.replace("\n", "|");
        broadcastMessage("GAME_PGN:::" + pgnForTransport);
    }

    private void broadcastState() {
        String fen = FenUtility.toFen(board);
        broadcastMessage("UPDATE_STATE " + fen);
    }

    private void broadcastMessage(String message) {
        if (whiteHumanHandler != null) whiteHumanHandler.sendMessage(message);
        if (blackHumanHandler != null) blackHumanHandler.sendMessage(message);    }

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