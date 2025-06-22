package main.client;

import main.model.pieces.*;
import main.FenUtility;
import main.model.Square;
import main.view.GameFrame;
import main.model.Board.*;
import main.model.Clock;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private String opponentName = "Opponent";
    private boolean isMyTurn = false;
    private Square selectedSquare = null;
    private Timer swingTimer;
    private Clock myClock;
    private Clock opponentClock;


    private StringBuilder pgnBuilder = null;
    private String finalPgn = null;


    public GameController() {
        this.view = new GameFrame();
        this.displayBoard = new Board();
        this.networkHandler = new NetworkHandler("127.0.0.1", 6789, this::handleServerMessage);
        this.initListeners();
        this.view.setVisible(true);
        view.setStatus("Connecting to server...");
        new Thread(networkHandler).start();
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {

            // STATE 1: We are currently assembling a PGN string.
            if (pgnBuilder != null) {
                pgnBuilder.append(message);

                // Check if this is the final piece of the PGN.
                String currentPgn = pgnBuilder.toString();
                if (currentPgn.endsWith("1-0") || currentPgn.endsWith("0-1") || currentPgn.endsWith("1/2-1/2") || currentPgn.endsWith("*")) {
                    this.finalPgn = currentPgn.replace("|", "\n");
                    view.getSavePgnMenuItem().setEnabled(true);
                    view.setStatus("Game finished. PGN ready to save from File menu.");
                    pgnBuilder = null; // Exit PGN-reading mode.
                }
                return; // Do not process this message any further.
            }

            // STATE 2: We are not currently reading a PGN. Check if a new PGN is starting.
            if (message.startsWith("GAME_PGN:::")) {
                pgnBuilder = new StringBuilder(); // Enter PGN-reading mode.
                String firstPart = message.substring("GAME_PGN:::".length());
                pgnBuilder.append(firstPart);
                return; // Do not process this message any further.
            }

            // STATE 3: This is a normal, single-line command.
            String[] parts = message.split(" ", 2);
            String command = parts[0];

            switch (command) {
                case "CONNECTED":
                    JTextField nameField = new JTextField("Player");
                    JTextField timeField = new JTextField("10");
                    JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
                    panel.add(new JLabel("Your Name:"));
                    panel.add(nameField);
                    panel.add(new JLabel("Time per side (minutes):"));
                    panel.add(timeField);
                    int result = JOptionPane.showConfirmDialog(view, panel, "Game Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        this.myName = nameField.getText().trim().isEmpty() ? "Guest" : nameField.getText().trim();
                        int timeMinutes = 10;
                        try {
                            timeMinutes = Integer.parseInt(timeField.getText().trim());
                            if (timeMinutes < 1) timeMinutes = 1;
                        } catch (NumberFormatException e) {
                        }
                        networkHandler.sendMessage("SET_NAME " + this.myName);
                        networkHandler.sendMessage("SET_TIME " + (timeMinutes * 60));
                        view.setStatus("Settings sent. Click 'Ready' to start.");
                    } else {
                        System.exit(0);
                    }
                    break;
                case "ASSIGN_COLOR":
                    this.myColor = Colour.valueOf(parts[1]);
                    view.setTitle("Chess Client - Playing as " + parts[1]);
                    break;
                case "OPPONENT_NAME":
                    this.opponentName = parts.length > 1 ? parts[1] : "Opponent";
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
                    view.getReadyButton().setVisible(false);
                    view.setStatus("Game started!");
                    String[] timeParts = message.split(" ");
                    int gameTimeSeconds = Integer.parseInt(timeParts[1]);
                    this.myClock = new Clock(0, 0, gameTimeSeconds);
                    this.opponentClock = new Clock(0, 0, gameTimeSeconds);
                    this.swingTimer = new Timer(1000, e -> updateClocks());
                    this.swingTimer.start();
                    break;
                case "UPDATE_STATE":
                    this.displayBoard.updateFromFen(parts[1]);
                    view.getChessBoardPanel().updateBoard(this.displayBoard);
                    break;
                case "WAITING_FOR_OPPONENT":
                    view.setStatus("Waiting for opponent to be ready...");
                    break;
                case "YOUR_TURN":
                    this.isMyTurn = true;
                    displayBoard.setTurn(myColor);
                    view.setStatus("Your turn.");
                    if (myClock != null) myClock.start();
                    if (opponentClock != null) opponentClock.stop();
                    break;
                case "OPPONENT_TURN":
                    this.isMyTurn = false;
                    displayBoard.setTurn(myColor == Colour.WHITE ? Colour.BLACK : Colour.WHITE);
                    view.setStatus("Opponent's turn.");
                    if (myClock != null) myClock.stop();
                    if (opponentClock != null) opponentClock.start();
                    break;
                case "VALID_MOVE":
                    break;
                case "INVALID_MOVE":
                    view.setStatus("Invalid move: " + (parts.length > 1 ? parts[1] : ""));
                    isMyTurn = true;
                    break;
                case "GAME_OVER":
                    if (swingTimer != null) swingTimer.stop();
                    isMyTurn = false;
                    view.setStatus("Game Over. PGN data is being sent.");
                    view.showGameOverDialog(parts.length > 1 ? parts[1] : "Game is over!", "Game Over");
                    break;
                case "ERROR":
                    view.setStatus("Error: " + (parts.length > 1 ? parts[1] : ""));
                    break;
                case "LEGAL_MOVES":
                    view.getChessBoardPanel().clearHighlights();
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        Set<Square> legalSquares = Arrays.stream(parts[1].split(" ")).map(Square::fromAlgebraic).collect(Collectors.toSet());
                        view.getChessBoardPanel().highlightLegalMoves(legalSquares);
                    }
                    break;
            }
        });
    }

    private void initListeners() {
        view.addReadyListener(e -> handleReadyButton());
        this.view.addQuitListener(e -> System.exit(0));
        this.view.getNewGameMenuItem().setEnabled(false);
        this.view.getLoadPgnMenuItem().setEnabled(false);
        this.view.addSavePgnListener(e -> handleSavePgn());
        BoardMouseListener mouseListener = new BoardMouseListener();
        this.view.getChessBoardPanel().addMouseListener(mouseListener);
        this.view.getChessBoardPanel().addMouseMotionListener(mouseListener);
    }

    private void handleSavePgn() {
        if (finalPgn == null || finalPgn.isEmpty()) {
            JOptionPane.showMessageDialog(view, "No PGN data available to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save PGN");
        String white = myColor == Colour.WHITE ? myName : opponentName;
        String black = myColor == Colour.BLACK ? myName : opponentName;
        fileChooser.setSelectedFile(new File(white + "_vs_" + black + ".pgn"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PGN Files (*.pgn)", "pgn"));
        int userSelection = fileChooser.showSaveDialog(view);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pgn")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".pgn");
            }
            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.write(finalPgn);
                JOptionPane.showMessageDialog(view, "Game saved successfully to:\n" + fileToSave.getAbsolutePath(), "PGN Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(view, "Error saving file: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleReadyButton() {
        networkHandler.sendMessage("PLAYER_READY");
        view.getReadyButton().setEnabled(false);
        view.setStatus("Ready! Waiting for opponent...");
    }

    private void updateClocks() {
        boolean timeout = false;
        boolean didIWin = false;
        if (myClock != null && myClock.isRunning()) {
            if (myClock.decrement()) {
                timeout = true;
                didIWin = false;
            }
        }
        if (opponentClock != null && opponentClock.isRunning()) {
            if (opponentClock.decrement()) {
                timeout = true;
                didIWin = true;
            }
        }
        if (myClock != null) view.updateClock(myColor, myClock.getTime());
        if (opponentClock != null)
            view.updateClock(myColor == Colour.WHITE ? Colour.BLACK : Colour.WHITE, opponentClock.getTime());
        if (timeout) {
            handleTimeout(didIWin);
        }
    }

    private void handleTimeout(boolean didIWin) {
        if (swingTimer != null) swingTimer.stop();
        isMyTurn = false;
        view.setStatus("Time's up! You " + (didIWin ? "win!" : "lose."));
        view.showGameOverDialog("Time's up! You " + (didIWin ? "win!" : "lose."), "Game Over");
    }

    private void attemptUserMove(Square start, Square end) {
        if (!isMyTurn) return;
        Optional<String> promotionChoice = Optional.empty();
        Piece movingPiece = displayBoard.getPiece(start);
        if (movingPiece.getSymbol() == 'P' && (end.rank() == 0 || end.rank() == 7)) {
            promotionChoice = Optional.of(view.askPromotionChoice());
        }
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