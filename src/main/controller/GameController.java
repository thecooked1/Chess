package main.controller;

import main.model.Board.Board;
import main.model.PGNParser.Interpreter;
import main.model.PGNParser.Move;
import main.model.PGNParser.Parser;
import main.model.Square;
import main.model.pieces.Colour;
import main.model.pieces.Pawn;
import main.model.pieces.Piece;
import main.view.ChessBoardPanel;
import main.view.GameFrame;
import main.model.Clock;
import javax.swing.*;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class GameController {

    private final GameFrame view;
    private final Board board;
    private final Interpreter pgnInterpreter;
    private final Clock whiteClock;
    private final Clock blackClock;
    private final Timer swingTimer;

    private Square selectedSquare = null; // The currently selected square for a user move

    // State for PGN Replay
    private List<String> pgnMoveTokens;
    private int currentPgnMoveIndex;
    private boolean inReplayMode = false;

    public GameController() {
        // 1. Create the Model and the View
        this.board = new Board();
        this.view = new GameFrame();
        this.pgnInterpreter = new Interpreter();

        this.whiteClock = new Clock(0, 10, 0);
        this.blackClock = new Clock(0, 10, 0);
        this.swingTimer = new Timer(1000, e -> updateClocks());

        // 2. Connect Controller to View (add event listeners)
        this.initListeners();

        // 3. Initial display update
        this.updateView();

        this.swingTimer.start();

        // 4. Make the application visible
        this.view.setVisible(true);
    }

    private void updateClocks() {
        if (board.getTurn() == Colour.WHITE) {
            if (whiteClock.decrement()) {
                handleTimeout(Colour.BLACK); // Black wins if White's time runs out
            }
        } else {
            if (blackClock.decrement()) {
                handleTimeout(Colour.WHITE); // White wins if Black's time runs out
            }
        }
        // Update the labels in the view
        view.updateClock(Colour.WHITE, whiteClock.getTime());
        view.updateClock(Colour.BLACK, blackClock.getTime());
    }

    private void handleTimeout(Colour winner) {
        swingTimer.stop(); // Stop the clock
        view.setStatus("Time's up! " + winner + " wins.");
        // Optionally, disable the board or show a game over dialog
        view.showGameOverDialog("Time's up! " + winner + " wins.", "Game Over");
    }

    private void initListeners() {
        this.view.getLoadPgnMenuItem().addActionListener(e -> handleLoadPgn());
        this.view.getNextMoveButton().addActionListener(e -> handlePgnNextMove());
        this.view.getPrevMoveButton().addActionListener(e -> handlePgnPrevMove());
        this.view.addQuitListener(e -> quitGame());

        BoardMouseListener mouseListener = new BoardMouseListener();
        this.view.getChessBoardPanel().addMouseListener(mouseListener);
        this.view.getChessBoardPanel().addMouseMotionListener(mouseListener);
    }

    private void handleLoadPgn() {
        JFileChooser fileChooser = new JFileChooser("./");
        int result = fileChooser.showOpenDialog(view);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File pgnFile = fileChooser.getSelectedFile();
                Parser pgnParser = new Parser();
                pgnParser.loadPGN(pgnFile);

                if (pgnParser.getGames().isEmpty()) {
                    JOptionPane.showMessageDialog(view, "No games found in the PGN file.", "Parsing Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Prepare for replay
                Parser.PGNGame firstGame = pgnParser.getGames().get(0);
                this.pgnMoveTokens = firstGame.getMoves();
                this.currentPgnMoveIndex = -1; // -1 means the start position
                this.inReplayMode = true;

                // Reset the board engine to the initial position
                this.board.setupInitialPosition();

                // Update the GUI
                this.view.updatePlayerInfo(firstGame.getHeaders());
                this.view.enableReplayControls(true);
                updateView(); // Update board and status

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view, "Failed to load or parse PGN file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handlePgnNextMove() {
        if (!inReplayMode || currentPgnMoveIndex >= pgnMoveTokens.size() - 1) {
            return; // No more moves
        }

        currentPgnMoveIndex++;
        String moveToken = pgnMoveTokens.get(currentPgnMoveIndex);

        try {
            Move pgnMove = pgnInterpreter.parseMove(moveToken);
            boolean success = board.applyMove(pgnMove);

            if (!success) {
                String errorMsg = "Illegal move encountered during replay: " + moveToken;
                JOptionPane.showMessageDialog(view, errorMsg, "Illegal Move", JOptionPane.ERROR_MESSAGE);
            }
            updateView();
        } catch (Exception e) {
            String errorMsg = "Error parsing move token: " + moveToken;
            e.printStackTrace();
            JOptionPane.showMessageDialog(view, errorMsg, "Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handlePgnPrevMove() {
        if (!inReplayMode || currentPgnMoveIndex < 0) {
            return;
        }

        currentPgnMoveIndex--;
        board.setupInitialPosition(); // Reset to start

        // Replay all moves up to the new current index
        for (int i = 0; i <= currentPgnMoveIndex; i++) {
            String moveToken = pgnMoveTokens.get(i);
            try {
                Move pgnMove = pgnInterpreter.parseMove(moveToken);
                board.applyMove(pgnMove);
            } catch (Exception e) {
                // Should not happen if PGN was valid
                break;
            }
        }
        updateView();
    }

    private void attemptUserMove(Square start, Square end) {
        // Ask the model for the list of legal moves for the selected piece.
        List<Square> legalMoves = board.getLegalMovesForPiece(start);

        // If the intended destination is in the list, proceed.
        if (legalMoves.contains(end)) {
            Optional<String> promotionChoice = Optional.empty();

            // Check if this move is a promotion
            Piece movingPiece = board.getPiece(start);
            if (movingPiece instanceof Pawn) {
                int promotionRank = (movingPiece.getColor() == Colour.WHITE) ? 0 : 7;
                if (end.rank() == promotionRank) {
                    promotionChoice = Optional.of(view.askPromotionChoice());
                }
            }

            // Tell the model to apply the move.
            board.applyMove(start, end, promotionChoice);
        }

        // After any move attempt (legal or not), update the entire view to reflect the board's true state.
        updateView();
    }

    private void updateView() {
        // Update the visual board
        view.getChessBoardPanel().updateBoard(board);

        if (board.getTurn() == Colour.WHITE) {
            whiteClock.start();
            blackClock.stop();
        } else {
            whiteClock.stop();
            blackClock.start();
        }
        // Update the clock labels immediately after a move
        view.updateClock(Colour.WHITE, whiteClock.getTime());
        view.updateClock(Colour.BLACK, blackClock.getTime());


        // Update the status label (Check, Checkmate, Stalemate, Whose turn)
        Colour currentTurn = board.getTurn();
        String status;

        if (board.isInCheck(currentTurn)) {
            if (board.hasAnyLegalMoves(currentTurn)) {
                status = "Check! " + currentTurn + "'s turn.";
            } else {
                status = "Checkmate! " + (currentTurn == Colour.WHITE ? "Black" : "White") + " wins.";
                swingTimer.stop();
            }
        } else {
            if (board.hasAnyLegalMoves(currentTurn)) {
                status = currentTurn + "'s turn.";
            } else {
                status = "Stalemate! It's a draw.";
                swingTimer.stop();
            }
        }
        view.setStatus(status);

        // Clear any leftover visual artifacts
        view.getChessBoardPanel().clearHighlights();
        view.getChessBoardPanel().clearSelection();
    }

    private void quitGame() {
        int confirmation = JOptionPane.showConfirmDialog(view, "Are you sure you want to quit?", "Confirm Quit", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private class BoardMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (inReplayMode) return; // Don't allow interaction during PGN replay

            ChessBoardPanel boardPanel = view.getChessBoardPanel();
            Square clickedSquare = boardPanel.getSquareFromPoint(e.getPoint());
            if (clickedSquare == null) return;

            // Check if the player clicked their own piece
            Piece clickedPiece = board.getPiece(clickedSquare);
            if (clickedPiece != null && clickedPiece.getColor() == board.getTurn()) {
                selectedSquare = clickedSquare;

                // Show visual feedback
                boardPanel.setDraggedPiece(clickedPiece, e.getPoint());
                boardPanel.selectSquare(selectedSquare);
                Set<Square> legalTargets = new HashSet<>(board.getLegalMovesForPiece(selectedSquare));
                boardPanel.highlightLegalMoves(legalTargets);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (selectedSquare == null) return;

            ChessBoardPanel boardPanel = view.getChessBoardPanel();
            Square releaseSquare = boardPanel.getSquareFromPoint(e.getPoint());

            // Always clear dragging visuals first
            boardPanel.clearDraggedPiece();

            if (releaseSquare != null && !releaseSquare.equals(selectedSquare)) {
                // Delegate the actual move logic to the controller
                attemptUserMove(selectedSquare, releaseSquare);
            } else {
                // If the move was invalid or canceled, we still need to refresh the view to clear highlights
                updateView();
            }

            // The move is done or canceled, so clear the selection state
            selectedSquare = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedSquare != null) {
                view.getChessBoardPanel().setDraggedPiece(board.getPiece(selectedSquare), e.getPoint());
            }
        }
    }
}