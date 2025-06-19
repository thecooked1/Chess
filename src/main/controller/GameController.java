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
import main.view.GameSettings;
import main.model.Clock;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

public class GameController {

    private final GameFrame view;
    private final Board board;
    private final Interpreter pgnInterpreter;
    private final Clock whiteClock;
    private final Clock blackClock;
    private final Timer swingTimer;
    private final GameSettings gameSettings;


    private Square selectedSquare = null;
    private List<String> pgnMoveTokens;
    private int currentPgnMoveIndex;
    private boolean inReplayMode = false;

    public GameController(GameSettings settings) {
        this.gameSettings = settings;
        this.board = new Board();
        this.view = new GameFrame();
        this.pgnInterpreter = new Interpreter();

        this.whiteClock = new Clock(settings.initialHours, settings.initialMinutes, settings.initialSeconds);
        this.blackClock = new Clock(settings.initialHours, settings.initialMinutes, settings.initialSeconds);
        this.swingTimer = new Timer(1000, e -> updateClocks());

        this.initListeners();

        Map<String, String> playerNames = new HashMap<>();
        playerNames.put("White", settings.whitePlayerName);
        playerNames.put("Black", settings.blackPlayerName);
        this.view.updatePlayerInfo(playerNames);

        this.updateView();
        this.swingTimer.start();
        this.view.setVisible(true);
    }

    private void initListeners() {
        this.view.getNewGameMenuItem().addActionListener(e -> handleNewGame());
        this.view.getLoadPgnMenuItem().addActionListener(e -> handleLoadPgn());
        this.view.getNextMoveButton().addActionListener(e -> handlePgnNextMove());
        this.view.getPrevMoveButton().addActionListener(e -> handlePgnPrevMove());
        this.view.addQuitListener(e -> quitGame());

        BoardMouseListener mouseListener = new BoardMouseListener();
        this.view.getChessBoardPanel().addMouseListener(mouseListener);
        this.view.getChessBoardPanel().addMouseMotionListener(mouseListener);
    }

    private void updateClocks() {
        if (board.getTurn() == Colour.WHITE) {
            if (whiteClock.decrement()) {
                handleTimeout(Colour.BLACK);
            }
        } else {
            if (blackClock.decrement()) {
                handleTimeout(Colour.WHITE);
            }
        }
        view.updateClock(Colour.WHITE, whiteClock.getTime());
        view.updateClock(Colour.BLACK, blackClock.getTime());
    }


    private void handleNewGame() {
        swingTimer.stop();
        view.dispose();
        main.Main.launchStartMenu();
    }

    private void handleTimeout(Colour winner) {
        swingTimer.stop(); // Stop the clock
        view.setStatus("Time's up! " + winner + " wins.");
        view.showGameOverDialog("Time's up! " + winner + " wins.", "Game Over");
    }

    private void handleLoadPgn() {
        // Logic for loading PGN files remains useful
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
                Parser.PGNGame firstGame = pgnParser.getGames().get(0);
                this.pgnMoveTokens = firstGame.getMoves();
                this.currentPgnMoveIndex = -1;
                this.inReplayMode = true;
                board.setupInitialPosition();
                view.updatePlayerInfo(firstGame.getHeaders());
                view.enableReplayControls(true);
                updateView();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view, "Failed to load or parse PGN file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handlePgnNextMove() {
        if (!inReplayMode || currentPgnMoveIndex >= pgnMoveTokens.size() - 1) return;
        currentPgnMoveIndex++;
        String moveToken = pgnMoveTokens.get(currentPgnMoveIndex);
        try {
            Move pgnMove = pgnInterpreter.parseMove(moveToken);
            board.applyMove(pgnMove);
            updateView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePgnPrevMove() {
        if (!inReplayMode || currentPgnMoveIndex < 0) return;
        currentPgnMoveIndex--;
        board.setupInitialPosition();
        for (int i = 0; i <= currentPgnMoveIndex; i++) {
            try {
                Move pgnMove = pgnInterpreter.parseMove(pgnMoveTokens.get(i));
                board.applyMove(pgnMove);
            } catch (Exception e) { break; }
        }
        updateView();
    }

    private void attemptUserMove(Square start, Square end) {
        List<Square> legalMoves = board.getLegalMovesForPiece(start);
        if (legalMoves.contains(end)) {
            Optional<String> promotionChoice = Optional.empty();
            Piece movingPiece = board.getPiece(start);
            if (movingPiece instanceof Pawn) {
                int promotionRank = (movingPiece.getColor() == Colour.WHITE) ? 0 : 7;
                if (end.rank() == promotionRank) {
                    promotionChoice = Optional.of(view.askPromotionChoice());
                }
            }
            board.applyMove(start, end, promotionChoice);
        }
        updateView();
    }

    private void updateView() {
        view.getChessBoardPanel().updateBoard(board);
        if (!inReplayMode) { // Only run game clocks if not in PGN replay
            if (board.getTurn() == Colour.WHITE) {
                whiteClock.start();
                blackClock.stop();
            } else {
                whiteClock.stop();
                blackClock.start();
            }
        } else { // In replay mode, stop both clocks
            whiteClock.stop();
            blackClock.stop();
        }
        // Update the clock labels immediately
        view.updateClock(Colour.WHITE, whiteClock.getTime());
        view.updateClock(Colour.BLACK, blackClock.getTime());

        Colour currentTurn = board.getTurn();
        String status;

        if (board.isInCheck(currentTurn)) {
            if (board.hasAnyLegalMoves(currentTurn)) {
                status = "Check! " + currentTurn + "'s turn.";
            } else {
                status = "Checkmate! " + (currentTurn == Colour.WHITE ? "Black" : "White") + " wins.";
            }
        } else {
            if (board.hasAnyLegalMoves(currentTurn)) {
                status = currentTurn + "'s turn.";
            } else {
                status = "Stalemate! It's a draw.";
            }
        }
        view.setStatus(status);

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
            if (inReplayMode) return;
            Square clickedSquare = view.getChessBoardPanel().getSquareFromPoint(e.getPoint());
            if (clickedSquare == null) return;

            Piece clickedPiece = board.getPiece(clickedSquare);
            if (clickedPiece != null && clickedPiece.getColor() == board.getTurn()) {
                selectedSquare = clickedSquare;
                view.getChessBoardPanel().setDraggedPiece(clickedPiece, e.getPoint());
                view.getChessBoardPanel().selectSquare(selectedSquare);
                Set<Square> legalTargets = new HashSet<>(board.getLegalMovesForPiece(selectedSquare));
                view.getChessBoardPanel().highlightLegalMoves(legalTargets);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (selectedSquare == null) return;
            view.getChessBoardPanel().clearDraggedPiece();
            Square releaseSquare = view.getChessBoardPanel().getSquareFromPoint(e.getPoint());
            if (releaseSquare != null && !releaseSquare.equals(selectedSquare)) {
                attemptUserMove(selectedSquare, releaseSquare);
            } else {
                updateView();
            }
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