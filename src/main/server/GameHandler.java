// main/server/GameHandler.java

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        System.out.println("SERVER: New GameHandler created.");
    }

    @Override
    public void run() {
        try {
            // ... (setup is the same)
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

            whiteOut.println("ASSIGN_COLOR WHITE");
            blackOut.println("ASSIGN_COLOR BLACK");
            whiteOut.println("OPPONENT_NAME " + blackPlayerName);
            blackOut.println("OPPONENT_NAME " + whitePlayerName);

            int gameTimeSeconds = (whiteTimePreference + blackTimePreference) / 2;
            broadcastMessage("GAME_START " + gameTimeSeconds + " " + gameTimeSeconds);
            System.out.println("SERVER: Game started between " + whitePlayerName + " and " + blackPlayerName);


            while (true) {
                broadcastState();

                if (!board.hasAnyLegalMoves(board.getTurn())) {
                    // ... (game over logic is the same)
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
                    System.out.println("SERVER: Game over. Result: " + result);
                    sendPgnToClients(result);
                    break;
                }

                if (board.getTurn() == Colour.WHITE) {
                    System.out.println("SERVER: --- It is WHITE's turn ---");
                    handlePlayerTurn(whiteOut, whiteIn, blackOut);
                } else {
                    System.out.println("SERVER: --- It is BLACK's turn ---");
                    handlePlayerTurn(blackOut, blackIn, whiteOut);
                }
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Game handler error: " + e.getMessage());
            sendPgnToClients("*");
        } finally {
            closeConnections();
        }
    }


    private void handlePlayerTurn(PrintWriter activePlayerOut, BufferedReader activePlayerIn, PrintWriter opponentOut) throws IOException {
        activePlayerOut.println("YOUR_TURN");
        opponentOut.println("OPPONENT_TURN");

        while (true) {
            String clientMessage = activePlayerIn.readLine();
            if (clientMessage == null) {
                throw new IOException("Player disconnected.");
            }
            System.out.println("SERVER: Received message: " + clientMessage);

            String[] parts = clientMessage.split(" ");
            String command = parts[0];

            if ("MOVE".equals(command)) {
                Square start = Square.fromAlgebraic(parts[1]);
                Square end = Square.fromAlgebraic(parts[2]);
                Optional<String> promo = parts.length > 3 ? Optional.of(parts[3]) : Optional.empty();

                System.out.println("SERVER: Checking legality of move " + start.toAlgebraic() + " to " + end.toAlgebraic());
                if (board.isLegalMove(start, end)) {
                    System.out.println("SERVER: Move is legal. Applying to board.");

                    // We will use the simplified "debugging" version of applyMove from the last attempt.
                    String san = board.applyMove(start, end, promo);
                    System.out.println("SERVER: Board.applyMove returned: \"" + san + "\"");

                    moveHistory.add(san);
                    System.out.println("SERVER: Move added to history. History size is now: " + moveHistory.size());

                    activePlayerOut.println("VALID_MOVE");
                    break;
                } else {
                    System.out.println("SERVER: Move is ILLEGAL.");
                    activePlayerOut.println("INVALID_MOVE Move is not legal.");
                }
            } else if ("GET_LEGAL_MOVES".equals(command)) {
                // ... (this part is fine)
                Square start = Square.fromAlgebraic(parts[1]);
                List<Square> legalMoves = board.getLegalMovesForPiece(start);
                String movesString = legalMoves.stream().map(Square::toString).collect(Collectors.joining(" "));
                activePlayerOut.println("LEGAL_MOVES " + movesString);
            }
        }
    }

    private void handleLobbyPhase(BufferedReader in, boolean isWhite) {
        // ... (this part is fine)
        try {
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

    private void sendPgnToClients(String result) {
        System.out.println("SERVER: Generating PGN. Final history size: " + moveHistory.size());
        System.out.println("SERVER: Moves in history: " + moveHistory);

        StringBuilder pgn = new StringBuilder();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        pgn.append(String.format("[Event \"Online Chess Match\"]%n"));
        pgn.append(String.format("[Site \"Java Chess Server\"]%n"));
        pgn.append(String.format("[Date \"%s\"]%n", today.format(formatter)));
        pgn.append(String.format("[Round \"1\"]%n"));
        pgn.append(String.format("[White \"%s\"]%n", whitePlayerName));
        pgn.append(String.format("[Black \"%s\"]%n", blackPlayerName));
        pgn.append(String.format("[Result \"%s\"]%n%n", result));

        int moveNumber = 1;
        for (int i = 0; i < moveHistory.size(); i++) {
            if (i % 2 == 0) {
                pgn.append(moveNumber).append(". ");
                moveNumber++;
            }
            pgn.append(moveHistory.get(i)).append(" ");
        }
        pgn.append(result);

        String pgnForTransport = pgn.toString().replace("\n", "|");
        System.out.println("SERVER: Sending PGN data to clients.");
        broadcastMessage("GAME_PGN:::" + pgnForTransport);
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