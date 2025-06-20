package main.client;

import main.model.pieces.*;
import main.FenUtility;
import main.model.Square;
import main.view.ChessBoardPanel;
import main.view.GameFrame;
import main.model.Board.*;
import main.model.Clock;
import main.model.Square;
import javax.swing.*;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GameController {

    private final GameFrame view;
    private final NetworkHandler networkHandler;
    private final Board displayBoard;

    private String myName;

    private Colour myColor;
    private boolean isMyTurn = false;
    private Square selectedSquare = null;

    private Timer swingTimer;
    private Clock myClock;
    private Clock opponentClock;

    public GameController() {
        this.view = new GameFrame();
        this.displayBoard = new Board();
        
        // When a message is received from the server, call the handleServerMessage method
        this.networkHandler = new NetworkHandler("127.0.0.1", 6789, this::handleServerMessage);
        
        this.initListeners();
        this.view.setVisible(true);
        view.setStatus("Connecting to server...");
        
        // Start the network handler on a new thread
        new Thread(networkHandler).start();
    }

    private void initListeners() {
        view.addReadyListener(e -> handleReadyButton());
        this.view.addQuitListener(e -> System.exit(0));
        // We can disable PGN/New Game for the client for now
        this.view.getNewGameMenuItem().setEnabled(false);
        this.view.getLoadPgnMenuItem().setEnabled(false);
        
        BoardMouseListener mouseListener = new BoardMouseListener();
        this.view.getChessBoardPanel().addMouseListener(mouseListener);
        this.view.getChessBoardPanel().addMouseMotionListener(mouseListener);
    }

    private void handleReadyButton() {
        networkHandler.sendMessage("PLAYER_READY");
        view.getReadyButton().setEnabled(false); // Disable after clicking
        view.setStatus("Ready! Waiting for opponent...");
    }

    private void updateClocks() {
        boolean timeout = false;
        boolean didIWin = false;

        if (myClock != null && myClock.isRunning()) {
            if (myClock.decrement()) {
                timeout = true;
                didIWin = false; // My time ran out, I did not win
            }
        }
        if (opponentClock != null && opponentClock.isRunning()) {
            if (opponentClock.decrement()) {
                timeout = true;
                didIWin = true; // Opponent's time ran out, I won
            }
        }

        // Update view labels
        if (myClock != null) view.updateClock(myColor, myClock.getTime());
        if (opponentClock != null) view.updateClock(myColor == Colour.WHITE ? Colour.BLACK : Colour.WHITE, opponentClock.getTime());

        if (timeout) {
            handleTimeout(didIWin);
        }
    }
    
    private void handleTimeout(boolean didIWin) {
        if (swingTimer != null) swingTimer.stop();
        isMyTurn = false; // Game is over, no more turns
        view.setStatus("Time's up! You " + (didIWin ? "win!" : "lose."));
        view.showGameOverDialog("Time's up! You " + (didIWin ? "win!" : "lose."), "Game Over");
    }

    /**
     * This is the master method that processes all incoming messages from the server.
     */
    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> { // Ensure UI updates are on the Swing thread
            String[] parts = message.split(" ", 2);
            String command = parts[0];

            switch (command) {
                case "CONNECTED":
                    JTextField nameField = new JTextField("Player");
                    JTextField timeField = new JTextField("10"); // Default 10 minutes
                    JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
                    panel.add(new JLabel("Your Name:"));
                    panel.add(nameField);
                    panel.add(new JLabel("Time per side (minutes):"));
                    panel.add(timeField);

                    int result = JOptionPane.showConfirmDialog(view, panel, "Game Setup",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                    if (result == JOptionPane.OK_OPTION) {
                        this.myName = nameField.getText().trim().isEmpty() ? "Guest" : nameField.getText().trim();
                        int timeMinutes = 10;
                        try {
                            timeMinutes = Integer.parseInt(timeField.getText().trim());
                            if (timeMinutes < 1) timeMinutes = 1; // Minimum 1 minute
                        } catch (NumberFormatException e) {
                            // Default to 10 if input is invalid
                        }

                        // Send both name and time preference to server
                        networkHandler.sendMessage("SET_NAME " + this.myName);
                        networkHandler.sendMessage("SET_TIME " + (timeMinutes * 60)); // Send time in seconds
                        view.setStatus("Settings sent. Click 'Ready' to start.");
                    } else {
                        // User canceled, maybe close the connection or app
                        System.exit(0);
                    }
                    break;
                case "ASSIGN_COLOR":
                    this.myColor = Colour.valueOf(parts[1]);
                    view.setTitle("Chess Client - Playing as " + parts[1]);
                    break;
                case "OPPONENT_NAME":
                    String opponentName = parts.length > 1 ? parts[1] : "Opponent";
                    Map<String, String> names = new HashMap<>();
                    if (myColor == Colour.WHITE) {
                        names.put("White", myName + " (You)");
                        names.put("Black", opponentName);
                    } else {
                        names.put("White", opponentName);
                        names.put("Black", myName + " (You)");
                    }
                    view.updatePlayerInfo(names);
                    break;
                case "GAME_START":
                    view.getReadyButton().setVisible(false); // Hide ready button
                    view.setStatus("Game started!");
                    // Initialize and start the timer

                    String[] timeParts = message.split(" ");
                    int gameTimeSeconds = Integer.parseInt(timeParts[1]);

                    // Initialize clocks with the time dictated by the server
                    this.myClock = new Clock(0, 0, gameTimeSeconds);    
                    this.opponentClock = new Clock(0, 0, gameTimeSeconds);
    
                    // Create and start the master timer
                    this.swingTimer = new Timer(1000, e -> updateClocks());
                    this.swingTimer.start();

                    break;
                case "UPDATE_STATE":
                    this.displayBoard.updateFromFen(parts[1]); // Let's assume we create this method
                    view.getChessBoardPanel().updateBoard(this.displayBoard); // Pass the whole board
                    break;
                case "WAITING_FOR_OPPONENT":
                    view.setStatus("Waiting for opponent to be ready...");
                    break;
                case "YOUR_TURN":
                    this.isMyTurn = true;
                    displayBoard.setTurn(myColor);
                    view.setStatus("Your turn.");
                    if(myClock != null) myClock.start();
                    if(opponentClock != null) opponentClock.stop();
                    break;
                case "OPPONENT_TURN":
                    this.isMyTurn = false;
                    displayBoard.setTurn(myColor == Colour.WHITE ? Colour.BLACK : Colour.WHITE);
                    view.setStatus("Opponent's turn.");
                    if(myClock != null) myClock.stop();
                    if(opponentClock != null) opponentClock.start();
                    break;
                case "VALID_MOVE":
                    // Our move was good, nothing to do but wait for next state update
                    break;
                case "INVALID_MOVE":
                    view.setStatus("Invalid move: " + (parts.length > 1 ? parts[1] : ""));
                    isMyTurn = true;
                    break;
                case "GAME_OVER":
                    if (swingTimer != null) swingTimer.stop();
                    isMyTurn = false;
                    view.setStatus("Game Over.");
                    view.showGameOverDialog(parts.length > 1 ? parts[1] : "Game is over!", "Game Over");
                    break;
                case "ERROR":
                     view.setStatus("Error: " + (parts.length > 1 ? parts[1] : ""));
                    break;
                case "LEGAL_MOVES":
                view.getChessBoardPanel().clearHighlights(); // Clear any previous highlights
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    // Parse the space-separated list of moves
                    Set<Square> legalSquares = Arrays.stream(parts[1].split(" "))
                                                     .map(Square::fromAlgebraic)
                                                     .collect(Collectors.toSet());
                    // Tell the view to highlight these squares
                    view.getChessBoardPanel().highlightLegalMoves(legalSquares);
                }
                break;    
            }
        });
    }

    private void attemptUserMove(Square start, Square end) {
        if (!isMyTurn) return;

        Optional<String> promotionChoice = Optional.empty();
        Piece movingPiece = displayBoard.getPiece(start);

        // Simplified promotion check
        if (movingPiece.getSymbol() == 'P' && (end.rank() == 0 || end.rank() == 7)) {
            promotionChoice = Optional.of(view.askPromotionChoice());
        }

        // Construct the move message and send it to the server
        String moveMessage = "MOVE " + start + " " + end;
        if (promotionChoice.isPresent()) {
            moveMessage += " " + promotionChoice.get();
        }
        networkHandler.sendMessage(moveMessage);
        view.setStatus("Sent move " + start + " to " + end + "...");
    }

    private class BoardMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (!isMyTurn) return;

            Square clickedSquare = view.getChessBoardPanel().getSquareFromPoint(e.getPoint());
            if (clickedSquare == null) return;

            view.getChessBoardPanel().clearHighlights();

            Piece clickedPiece = displayBoard.getPiece(clickedSquare);
            if (clickedPiece != null && clickedPiece.getColor() == myColor) {
                selectedSquare = clickedSquare;
                view.getChessBoardPanel().selectSquare(selectedSquare);
                view.getChessBoardPanel().setDraggedPiece(clickedPiece, e.getPoint());

                // We can get legal moves LOCALLY now! No network call needed.
                // NOTE: We still need to ask the server to *make* the move.
                Set<Square> legalMoves = new HashSet<>(displayBoard.getLegalMovesForPiece(selectedSquare));
                view.getChessBoardPanel().highlightLegalMoves(legalMoves);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {

            view.getChessBoardPanel().clearHighlights();

            if (selectedSquare == null || !isMyTurn) return;

            Square releaseSquare = view.getChessBoardPanel().getSquareFromPoint(e.getPoint());
            view.getChessBoardPanel().clearDraggedPiece();
            view.getChessBoardPanel().clearSelection();

             Set<Square> legalMoves = new HashSet<>(displayBoard.getLegalMovesForPiece(selectedSquare));
            if (releaseSquare != null && legalMoves.contains(releaseSquare)) {
                attemptUserMove(selectedSquare, releaseSquare);
            }
            selectedSquare = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedSquare != null) {
                view.getChessBoardPanel().setDraggedPiece(displayBoard.getPiece(selectedSquare), e.getPoint());
            }
        }
    }
}