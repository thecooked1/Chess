// main/server/GameHandler.java

package main.server;

import main.common.FenUtility;
import main.common.Square;
import main.model.Board.Board;
import main.common.Colour;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import main.PGNGenerator;

/**
 * Manages the logic for a single chess game between two connected players.
 */
public class GameHandler implements Runnable {

    private final Socket whitePlayerSocket;
    private final Socket blackPlayerSocket;
    private final Board board;
    private final List<String> moveHistory = new ArrayList<>();

    private String whitePlayerName = "White";
    private String blackPlayerName = "Black";
    private int whiteTimePreference = 600;
    private int blackTimePreference = 600;

    private PrintWriter whiteOut;
    private BufferedReader whiteIn;
    private PrintWriter blackOut;
    private BufferedReader blackIn;

    public GameHandler(Socket whitePlayerSocket, Socket blackPlayerSocket) {
        this.whitePlayerSocket = whitePlayerSocket;
        this.blackPlayerSocket = blackPlayerSocket;
        this.board = new Board();
    }

    @Override
    public void run() {
        try {
            setupLobby();
            startGameSequence();
            mainGameLoop();
        } catch (IOException | InterruptedException e) {
            System.out.println("Game handler error: " + e.getMessage());
            sendPgnToClients("*"); // Game terminated unexpectedly
        } finally {
            closeConnections();
        }
    }

    /**
     * Sets up IO streams and handles the pre-game lobby phase for both players.
     */
    private void setupLobby() throws IOException, InterruptedException {
        whiteOut = new PrintWriter(whitePlayerSocket.getOutputStream(), true);
        whiteIn = new BufferedReader(new InputStreamReader(whitePlayerSocket.getInputStream()));
        blackOut = new PrintWriter(blackPlayerSocket.getOutputStream(), true);
        blackIn = new BufferedReader(new InputStreamReader(blackPlayerSocket.getInputStream()));

        Thread whiteLobbyThread = new Thread(() -> handleLobbyPhase(whiteIn, true));
        Thread blackLobbyThread = new Thread(() -> handleLobbyPhase(blackIn, false));
        whiteLobbyThread.start();
        blackLobbyThread.start();

        whiteLobbyThread.join();
        blackLobbyThread.join();
    }

    /**
     * Sends the initial game start information to both clients.
     */
    private void startGameSequence() {
        whiteOut.println("ASSIGN_COLOR WHITE");
        blackOut.println("ASSIGN_COLOR BLACK");
        whiteOut.println("OPPONENT_NAME " + blackPlayerName);
        blackOut.println("OPPONENT_NAME " + whitePlayerName);

        int gameTimeSeconds = (whiteTimePreference + blackTimePreference) / 2;
        broadcastMessage("GAME_START " + gameTimeSeconds + " " + gameTimeSeconds);
    }

    /**
     * Contains the main loop that alternates turns until the game is over.
     */
    private void mainGameLoop() throws IOException {
        while (true) {
            broadcastState();

            if (isGameOver()) {
                break;
            }

            if (board.getTurn() == Colour.WHITE) {
                handlePlayerTurn(whiteOut, whiteIn, blackOut);
            } else {
                handlePlayerTurn(blackOut, blackIn, whiteOut);
            }
        }
    }

    /**
     * Checks for game-ending conditions like checkmate or stalemate and handles them.
     *
     * @return true if the game is over, false otherwise.
     */
    private boolean isGameOver() {
        if (board.hasAnyLegalMoves(board.getTurn())) {
            return false;
        }

        String result;
        String message;
        if (board.isInCheck(board.getTurn())) {
            message = "Checkmate! " + (board.getTurn() == Colour.WHITE ? "Black" : "White") + " wins.";
            result = (board.getTurn() == Colour.WHITE) ? "0-1" : "1-0";
        } else {
            message = "Stalemate! The game is a draw.";
            result = "1/2-1/2";
        }
        broadcastMessage("GAME_OVER " + message);
        sendPgnToClients(result);
        return true;
    }

    private void handlePlayerTurn(PrintWriter activePlayerOut, BufferedReader activePlayerIn, PrintWriter opponentOut) throws IOException {
        // ... This method is already clean and focused, no changes needed ...
        activePlayerOut.println("YOUR_TURN");
        opponentOut.println("OPPONENT_TURN");
        while (true) {
            String clientMessage = activePlayerIn.readLine();
            if (clientMessage == null) {
                throw new IOException("Player disconnected.");
            }
            String[] parts = clientMessage.split(" ");
            String command = parts[0];
            if ("MOVE".equals(command)) {
                Square start = Square.fromAlgebraic(parts[1]);
                Square end = Square.fromAlgebraic(parts[2]);
                Optional<String> promo = parts.length > 3 ? Optional.of(parts[3]) : Optional.empty();
                if (board.isLegalMove(start, end)) {
                    String san = board.applyMove(start, end, promo);
                    moveHistory.add(san);
                    activePlayerOut.println("VALID_MOVE");
                    break;
                } else {
                    activePlayerOut.println("INVALID_MOVE Move is not legal.");
                }
            } else if ("GET_LEGAL_MOVES".equals(command)) {
                Square start = Square.fromAlgebraic(parts[1]);
                List<Square> legalMoves = board.getLegalMovesForPiece(start);
                String movesString = legalMoves.stream().map(Square::toString).collect(Collectors.joining(" "));
                activePlayerOut.println("LEGAL_MOVES " + movesString);
            }
        }
    }

    /**
     * Generates the PGN string using the PgnGenerator and sends it to the clients.
     */
    private void sendPgnToClients(String result) {
        String pgn = PGNGenerator.generate(whitePlayerName, blackPlayerName, result, moveHistory);
        // Replace newlines with a special character for transport over a single line.
        String pgnForTransport = pgn.replace("\n", "|");
        broadcastMessage("GAME_PGN:::" + pgnForTransport);
    }

    // ... other helper methods (handleLobbyPhase, broadcastState, etc.) are unchanged ...
    private void handleLobbyPhase(BufferedReader in, boolean isWhite) {
        try {
            boolean readySet = false;
            String line;
            while (!readySet && (line = in.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                String command = parts[0];
                String payload = parts.length > 1 ? parts[1] : "";
                switch (command) {
                    case "SET_NAME":
                        if (isWhite) whitePlayerName = payload;
                        else blackPlayerName = payload;
                        break;
                    case "SET_TIME":
                        try {
                            int time = Integer.parseInt(payload);
                            if (isWhite) whiteTimePreference = time;
                            else blackTimePreference = time;
                        } catch (NumberFormatException e) { /* Ignore */ }
                        break;
                    case "PLAYER_READY":
                        PrintWriter out = isWhite ? whiteOut : blackOut;
                        out.println("WAITING_FOR_OPPONENT");
                        readySet = true;
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Lobby error for one player: " + e.getMessage());
        }
    }

    private void broadcastState() {
        String fen = FenUtility.toFen(board);
        whiteOut.println("UPDATE_STATE " + fen);
        blackOut.println("UPDATE_STATE " + fen);
    }

    private void broadcastMessage(String message) {
        if (whiteOut != null) whiteOut.println(message);
        if (blackOut != null) blackOut.println(message);
    }

    private void closeConnections() {
        try {
            if (whitePlayerSocket != null) whitePlayerSocket.close();
            if (blackPlayerSocket != null) blackPlayerSocket.close();
        } catch (IOException e) { /* Ignore */ }
        System.out.println("Game finished. Connections closed.");
    }
}