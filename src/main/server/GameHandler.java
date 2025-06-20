package main.server;

import main.FenUtility;
import main.model.Square;
import main.model.Board.Board;
import main.model.pieces.Colour;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

public class GameHandler implements Runnable {

    private final Socket whitePlayerSocket;
    private final Socket blackPlayerSocket;
    private final Board board;

    private String whitePlayerName = "White";
    private String blackPlayerName = "Black";
    private int whiteTimePreference = 600; // Default 10 mins in seconds
    private int blackTimePreference = 600;
    private boolean whiteIsReady = false;
    private boolean blackIsReady = false;

    private PrintWriter whiteOut;
    private BufferedReader whiteIn;
    private PrintWriter blackOut;
    private BufferedReader blackIn;

    public GameHandler(Socket whitePlayerSocket, Socket blackPlayerSocket) {
        this.whitePlayerSocket = whitePlayerSocket;
        this.blackPlayerSocket = blackPlayerSocket;
        this.board = new Board(); // The server's authoritative board
    }

    @Override
    public void run() {
        try {
            // Setup streams for communication
            whiteOut = new PrintWriter(whitePlayerSocket.getOutputStream(), true);
            whiteIn = new BufferedReader(new InputStreamReader(whitePlayerSocket.getInputStream()));
            blackOut = new PrintWriter(blackPlayerSocket.getOutputStream(), true);
            blackIn = new BufferedReader(new InputStreamReader(blackPlayerSocket.getInputStream()));

            // Create and start a separate thread for each player's lobby setup
            Thread whiteLobbyThread = new Thread(() -> handleLobbyPhase(whiteIn, whiteOut, blackOut, true));
            Thread blackLobbyThread = new Thread(() -> handleLobbyPhase(blackIn, blackOut, whiteOut, false));
            whiteLobbyThread.start();
            blackLobbyThread.start();

            // Wait for both lobby threads to complete (meaning both players are ready)
            whiteLobbyThread.join();
            blackLobbyThread.join();

            // 1. Assign colors
            whiteOut.println("ASSIGN_COLOR WHITE");
            blackOut.println("ASSIGN_COLOR BLACK");

            // Let's use the average of the two preferences
            int gameTimeSeconds = (whiteTimePreference + blackTimePreference) / 2;
            broadcastMessage("GAME_START " + gameTimeSeconds + " " + gameTimeSeconds);
            
            // 2. Main game loop
            while (true) {
                broadcastState(); // Send current board state to both players

                // Check for game over conditions
                if (!board.hasAnyLegalMoves(board.getTurn())) {
                    String message = board.isInCheck(board.getTurn()) ? "Checkmate! " : "Stalemate!";
                    if (board.isInCheck(board.getTurn())) {
                        message += (board.getTurn() == Colour.WHITE ? "Black" : "White") + " wins.";
                    }
                    broadcastMessage("GAME_OVER " + message);
                    break; // End game
                }
                
                // 3. Handle a single turn
                if (board.getTurn() == Colour.WHITE) {
                    handlePlayerTurn(whiteOut, whiteIn, blackOut);
                } else {
                    handlePlayerTurn(blackOut, blackIn, whiteOut);
                }
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Game handler error: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }


    private void handlePlayerTurn(PrintWriter activePlayerOut, BufferedReader activePlayerIn, PrintWriter opponentOut) throws IOException {
        activePlayerOut.println("YOUR_TURN");
        opponentOut.println("OPPONENT_TURN");

        while (true) {
            String clientMessage = activePlayerIn.readLine();
            if (clientMessage == null) continue;

            String[] parts = clientMessage.split(" ");
            String command = parts[0];

            // --- MODIFICATION HERE ---
            // Handle two different types of commands from the active player
            if ("MOVE".equals(command)) {
                // This is the existing move logic
                Square start = Square.fromAlgebraic(parts[1]);
                Square end = Square.fromAlgebraic(parts[2]);
                Optional<String> promo = parts.length > 3 ? Optional.of(parts[3]) : Optional.empty();

                if (board.isLegalMove(start, end)) {
                    board.applyMove(start, end, promo);
                    activePlayerOut.println("VALID_MOVE");
                    break; // Move was valid, break the loop to end the turn
                } else {
                    activePlayerOut.println("INVALID_MOVE Move is not legal.");
                    // Do NOT break, wait for a valid move
                }
            } else if ("GET_LEGAL_MOVES".equals(command)) {
                // This is the new logic for providing move indicators
                Square start = Square.fromAlgebraic(parts[1]);
                List<Square> legalMoves = board.getLegalMovesForPiece(start);

                // Convert the list of Square objects to a single space-separated string
                String movesString = legalMoves.stream()
                                           .map(Square::toString)
                                           .collect(Collectors.joining(" "));

                activePlayerOut.println("LEGAL_MOVES " + movesString);
                // Do NOT break, the turn is not over yet. Wait for a MOVE command.
            }
            // --- END OF MODIFICATION ---
        }
    }

    private void handleLobbyPhase(BufferedReader in, PrintWriter out, PrintWriter opponentOut, boolean isWhite) {
        try {
            boolean nameSet = false;
            boolean timeSet = false;
            boolean readySet = false;

            String line;
            while(!readySet && (line = in.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                String command = parts[0];
                String payload = parts.length > 1 ? parts[1] : "";

                switch(command) {
                    case "SET_NAME":
                        if (isWhite) whitePlayerName = payload;
                        else blackPlayerName = payload;
                        opponentOut.println("OPPONENT_NAME " + payload);
                        nameSet = true;
                        break;
                    
                    case "SET_TIME":
                        try {
                            int time = Integer.parseInt(payload);
                            if (isWhite) whiteTimePreference = time;
                            else blackTimePreference = time;
                            timeSet = true;
                        } catch (NumberFormatException e) {
                            // Ignore invalid time
                        }
                        break;

                    case "PLAYER_READY":
                        if (isWhite) whiteIsReady = true;
                        else blackIsReady = true;
                        out.println("WAITING_FOR_OPPONENT");
                        readySet = true; // This player is done with the lobby
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Lobby error for one player: " + e.getMessage());
            // This will cause the thread to end, and join() will unblock.
        }
    }

    
    private void broadcastState() {
        String fen = FenUtility.toFen(board);
        whiteOut.println("UPDATE_STATE " + fen);
        blackOut.println("UPDATE_STATE " + fen);
    }
    
    private void broadcastMessage(String message) {
        whiteOut.println(message);
        blackOut.println(message);
    }

    private void closeConnections() {
        try {
            whitePlayerSocket.close();
            blackPlayerSocket.close();
        } catch (IOException e) {
            // Ignore
        }
        System.out.println("Game finished. Connections closed.");
    }
}