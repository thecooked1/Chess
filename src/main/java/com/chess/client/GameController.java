
package com.chess.client;

import com.chess.model.Board.Board;
import com.chess.model.pieces.Piece;
import com.chess.common.Colour;
import com.chess.model.pieces.*;
import com.chess.common.Square;
import com.chess.view.GameFrame;
import com.chess.model.Board.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
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
    private boolean isGameActive = false;

    private int moveNumber = 1;
    private Colour currentTurnForLog = Colour.WHITE;

    private StringBuilder pgnBuilder = null;
    private String finalPgn = null;

    public GameController(String loggedInUsername, int timeInSeconds, NetworkHandler handler) {
        this.myName = loggedInUsername;
        this.networkHandler = handler; // Use the existing handler
        this.networkHandler.setMessageConsumer(this::handleServerMessage); // Switch message handling to this class

        // Standard setup
        this.view = new GameFrame();
        this.displayBoard = new Board();
        initListeners();
        view.setVisible(true);

        // Send post-login setup info to the server
        this.networkHandler.sendMessage("SET_TIME " + timeInSeconds);
        view.setStatus("Logged in as " + myName + ". Click 'Ready' to find a match.");
    }

    private void initListeners() {
        view.addReadyListener(e -> handleReadyButton());
        view.addPlayBotListener(e -> handlePlayBotButton());
        this.view.addQuitListener(e -> System.exit(0));
        this.view.getNewGameMenuItem().setEnabled(false);
        this.view.getLoadPgnMenuItem().setEnabled(false);
        this.view.addSavePgnListener(e -> handleSavePgn());
        BoardMouseListener mouseListener = new BoardMouseListener();
        this.view.getChessBoardPanel().addMouseListener(mouseListener);
        this.view.getChessBoardPanel().addMouseMotionListener(mouseListener);
    }

    /**
     * This is the master method that processes all incoming messages from the server.
     * It acts as a dispatcher, calling specific handler methods based on the command.
     */
    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (handlePgnStream(message)) {
                return; // If it was part of a PGN message, stop processing.
            }

            String[] parts = message.split(" ", 2);
            String command = parts[0];
            String payload = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "UPDATE_TIME"  -> handleUpdateTime(payload);
                case "ASSIGN_COLOR" -> handleAssignColor(payload);
                case "OPPONENT_NAME" -> handleOpponentName(payload);
                case "GAME_START" -> handleGameStart(payload);
                case "UPDATE_STATE" -> handleUpdateState(payload);
                case "YOUR_TURN" -> handleYourTurn();
                case "OPPONENT_TURN" -> handleOpponentTurn();
                case "GAME_OVER" -> handleGameOver(payload);
                case "LEGAL_MOVES" -> handleLegalMoves(payload);
                case "WAITING_FOR_OPPONENT" -> view.setStatus("Waiting for opponent to be ready...");
                case "VALID_MOVE" -> handleValidMove(payload);
                case "INVALID_MOVE" -> {
                    view.setStatus("Invalid move: " + payload);
                    isMyTurn = true;
                }
                case "ERROR" -> view.setStatus("Error: " + payload);
            }
        });
    }

    // In main/client/GameController.java

    private void handleValidMove(String san) {
        // A move was validated by the server. Add it to our GUI log.
        if (san != null && !san.isEmpty()) {
            view.addMoveToLog(moveNumber, san, currentTurnForLog);

            // If it was Black's move, increment the move number for the next pair.
            if (currentTurnForLog == Colour.BLACK) {
                moveNumber++;
            }

            // Toggle the turn for the next log entry.
            currentTurnForLog = (currentTurnForLog == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
        }
    }

    private void handleUpdateTime(String payload) {
        String[] times = payload.split(" ");
        if (times.length == 2) {
            String whiteTime = times[0];
            String blackTime = times[1];
            view.updateClock(Colour.WHITE, whiteTime);
            view.updateClock(Colour.BLACK, blackTime);
        }
    }

    /**
     * Handles the stream of PGN data, which may arrive in multiple packets.
     *
     * @return true if the message was part of a PGN stream, false otherwise.
     */
    private boolean handlePgnStream(String message) {
        if (pgnBuilder != null) {
            pgnBuilder.append(message);
            String currentPgn = pgnBuilder.toString();
            if (currentPgn.endsWith("1-0") || currentPgn.endsWith("0-1") || currentPgn.endsWith("1/2-1/2") || currentPgn.endsWith("*")) {
                this.finalPgn = currentPgn.replace("|", "\n");
                view.getSavePgnMenuItem().setEnabled(true);
                view.setStatus("Game finished. PGN ready to save from File menu.");
                pgnBuilder = null;
            }
            return true;
        }
        if (message.startsWith("GAME_PGN:::")) {
            pgnBuilder = new StringBuilder();
            pgnBuilder.append(message.substring("GAME_PGN:::".length()));
            return true;
        }
        return false;
    }

    private void handleAssignColor(String payload) {
        this.myColor = Colour.valueOf(payload);
        view.setTitle("Chess Client - Playing as " + payload);
    }

    private void handleOpponentName(String payload) {
        this.opponentName = payload;
        Map<String, String> names = new HashMap<>();
        if (myColor == Colour.WHITE) {
            names.put("White", myName + " (You)");
            names.put("Black", opponentName);
        } else {
            names.put("White", opponentName);
            names.put("Black", myName + " (You)");
        }
        view.updatePlayerInfo(names);
    }

    private void handlePlayBotButton() {
        // Send a new, specific command to the server.
        networkHandler.sendMessage("PLAY_BOT");
        view.getReadyButton().setEnabled(false);
        view.getPlayBotButton().setEnabled(false); // Disable both buttons
        view.setStatus("Requesting a game against the bot...");
    }

    private void handleGameStart(String payload) {
        view.getReadyButton().setVisible(false);
        view.getPlayBotButton().setVisible(false);
        view.setStatus("Game started!");
        handleUpdateTime(payload);

    }

    private void handleUpdateState(String payload) {
        this.displayBoard.updateFromFen(payload);
        view.getChessBoardPanel().updateBoard(this.displayBoard);
    }

    private void handleYourTurn() {
        this.isMyTurn = true;
        this.isGameActive = true;
        displayBoard.setTurn(myColor);
        view.setStatus("Your turn.");
    }

    private void handleOpponentTurn() {
        this.isMyTurn = false;
        this.isGameActive = true;
        displayBoard.setTurn(myColor == Colour.WHITE ? Colour.BLACK : Colour.WHITE);
        view.setStatus("Opponent's turn.");
    }

    private void handleGameOver(String payload) {
        isMyTurn = false;
        view.setStatus("Game Over. PGN data is being sent.");
        view.showGameOverDialog(payload, "Game Over");
    }

    private void handleLegalMoves(String payload) {
        view.getChessBoardPanel().clearHighlights();
        if (!payload.isEmpty()) {
            Set<Square> legalSquares = Arrays.stream(payload.split(" ")).map(Square::fromAlgebraic).collect(Collectors.toSet());
            view.getChessBoardPanel().highlightLegalMoves(legalSquares);
        }
    }

    // ... other methods (handleSavePgn, BoardMouseListener, etc.) are unchanged ...
    private void handleSavePgn() {
        if (finalPgn == null || finalPgn.isEmpty()) {
            JOptionPane.showMessageDialog(view, "No PGN data available to save. The file might be empty.", "Error", JOptionPane.ERROR_MESSAGE);
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
        view.getPlayBotButton().setEnabled(false);
        view.setStatus("Ready signal sent. Waiting for match...");
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

            if (!isGameActive) return;

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

            if (!isGameActive) return;

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

            if (!isGameActive) return;

            if (selectedSquare != null) {
                view.getChessBoardPanel().setDraggedPiece(displayBoard.getPiece(selectedSquare), e.getPoint());
            }
        }
    }
}