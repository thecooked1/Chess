package main.controller;

import main.model.*;
import main.view.*; // Use the settings holder

import javax.swing.*;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GameController {

    private GameLogic gameLogic;
    private GameFrame gameFrame;
    private ChessBoardPanel boardPanel;

    private Position selectedPosition = null; // Track the square the user clicked on
    private List<Move> legalMovesForSelectedPiece = null;

    // Timer related fields
    private Timer whiteTimer;
    private Timer blackTimer;
    private int whiteSecondsLeft;
    private int blackSecondsLeft;
    private final int TIMER_DELAY_MS = 1000; // 1 second

    public GameController() {
        // Initialization will be done after settings are received
    }

    public void setupGame(GameSettings settings) {
        this.gameLogic = new GameLogic(); // Create new game logic
        this.gameFrame = new GameFrame();
        this.boardPanel = gameFrame.getChessBoardPanel();

        // Initialize Clocks based on settings
        initializeTimers(settings.hours, settings.minutes, settings.seconds);
        gameFrame.setPlayerNames(settings.whiteName, settings.blackName);

        // Add listeners
        boardPanel.addMouseListener(new BoardMouseListener());
        boardPanel.addMouseMotionListener(new BoardMouseMotionListener());
        gameFrame.addNewGameListener(e -> restartGame());
        gameFrame.addQuitListener(e -> quitGame());

        // Initial display
        updateView();
        gameFrame.setVisible(true);
        startTimer(gameLogic.getCurrentPlayerColor()); // Start the timer for the first player
    }

    private void initializeTimers(int h, int m, int s) {
        whiteSecondsLeft = h * 3600 + m * 60 + s;
        blackSecondsLeft = whiteSecondsLeft; // Same time for both players

        if (whiteSecondsLeft <= 0) { // Untimed game
            whiteTimer = null;
            blackTimer = null;
            gameFrame.updateClock(Color.WHITE, "Untimed");
            gameFrame.updateClock(Color.BLACK, "Untimed");
            return;
        }

        whiteTimer = new Timer(TIMER_DELAY_MS, e -> {
            whiteSecondsLeft--;
            gameFrame.updateClock(Color.WHITE, formatTime(whiteSecondsLeft));
            if (whiteSecondsLeft <= 0) {
                handleTimeout(Color.WHITE);
            }
        });

        blackTimer = new Timer(TIMER_DELAY_MS, e -> {
            blackSecondsLeft--;
            gameFrame.updateClock(Color.BLACK, formatTime(blackSecondsLeft));
            if (blackSecondsLeft <= 0) {
                handleTimeout(Color.BLACK);
            }
        });

        // Initial display
        gameFrame.updateClock(Color.WHITE, formatTime(whiteSecondsLeft));
        gameFrame.updateClock(Color.BLACK, formatTime(blackSecondsLeft));
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void startTimer(Color player) {
        if (player == Color.WHITE && whiteTimer != null) {
            if (blackTimer != null) blackTimer.stop();
            whiteTimer.start();
        } else if (player == Color.BLACK && blackTimer != null) {
            if (whiteTimer != null) whiteTimer.stop();
            blackTimer.start();
        }
    }

    private void stopTimers() {
        if (whiteTimer != null) whiteTimer.stop();
        if (blackTimer != null) blackTimer.stop();
    }

    private void handleTimeout(Color timedOutPlayer) {
        stopTimers();
        Color winner = (timedOutPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
        gameFrame.showGameOverDialog(winner + " wins on time!", "Game Over");
        disableBoardInteraction(); // Prevent further moves
    }


    private void updateView() {
        boardPanel.updateBoard(gameLogic.getGameState());
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        String status;
        if (gameLogic.isGameOver()) {
            Optional<Color> winner = gameLogic.getWinner();
            if (winner.isPresent()) {
                status = "Checkmate! " + winner.get() + " wins.";
            } else if (gameLogic.isStalemate()) {
                status = "Stalemate! Draw.";
            } else {
                status = "Game Over!"; // Other draw conditions?
            }
            stopTimers(); // Stop clocks on game over
            disableBoardInteraction();
        } else {
            status = gameLogic.getCurrentPlayerColor() + "'s turn";
            if (gameLogic.isCheck()) {
                status += " (Check!)";
            }
        }
        gameFrame.setStatus(status);
    }

    private void handleSquareClick(Position clickedPos) {
        if (gameLogic.isGameOver()) return; // Don't process clicks if game over

        Piece clickedPiece = gameLogic.getGameState().getBoard().getPieceAt(clickedPos);

        if (selectedPosition == null) {
            // --- First Click: Select a piece ---
            if (clickedPiece != null && clickedPiece.getColor() == gameLogic.getCurrentPlayerColor()) {
                selectedPosition = clickedPos;
                legalMovesForSelectedPiece = gameLogic.getLegalMovesForPiece(selectedPosition);

                // Highlight the selected square and potential moves
                boardPanel.selectSquare(selectedPosition);
                Set<Position> targetSquares = new HashSet<>();
                for (Move move : legalMovesForSelectedPiece) {
                    targetSquares.add(move.getEndPosition());
                }
                boardPanel.highlightLegalMoves(targetSquares);
            }
        } else {
            // --- Second Click: Attempt to move or deselect ---
            boardPanel.clearHighlights(); // Clear highlights from previous selection
            boardPanel.clearSelection();

            if (clickedPos.equals(selectedPosition)) {
                // Clicked the same square again: Deselect
                selectedPosition = null;
                legalMovesForSelectedPiece = null;
            } else {
                // Attempt to make a move to the clicked square
                Optional<Move> chosenMove = findMove(selectedPosition, clickedPos);

                if (chosenMove.isPresent()) {
                    Move move = chosenMove.get();

                    // Handle Pawn Promotion
                    if (move.isPromotion()) {
                        PieceType promotionType = gameFrame.askPromotionChoice();
                        // Create the specific promotion move
                        move = new Move(move.getStartPosition(), move.getEndPosition(), promotionType);
                    }

                    if (gameLogic.makeMove(move)) {
                        // Move successful
                        selectedPosition = null;
                        legalMovesForSelectedPiece = null;
                        updateView();
                        startTimer(gameLogic.getCurrentPlayerColor()); // Start next player's timer
                    } else {
                        // Move was illegal (shouldn't happen often if findMove works correctly)
                        System.err.println("Illegal move attempted: " + move);
                        selectedPosition = null;
                        legalMovesForSelectedPiece = null;
                    }
                } else {
                    // Clicked on an invalid target square OR another of own pieces
                    Piece targetPiece = gameLogic.getGameState().getBoard().getPieceAt(clickedPos);
                    if (targetPiece != null && targetPiece.getColor() == gameLogic.getCurrentPlayerColor()){
                        // Clicked another of own pieces - treat as new selection
                        selectedPosition = clickedPos;
                        legalMovesForSelectedPiece = gameLogic.getLegalMovesForPiece(selectedPosition);
                        boardPanel.selectSquare(selectedPosition);
                        Set<Position> targetSquares = new HashSet<>();
                        for (Move legalMove : legalMovesForSelectedPiece) {
                            targetSquares.add(legalMove.getEndPosition());
                        }
                        boardPanel.highlightLegalMoves(targetSquares);
                    } else {
                        // Clicked empty square or opponent piece not reachable - deselect
                        selectedPosition = null;
                        legalMovesForSelectedPiece = null;
                    }
                }
            }
        }
    }

    // Helper to find the move object corresponding to start/end positions
    private Optional<Move> findMove(Position start, Position end) {
        if (legalMovesForSelectedPiece == null) return Optional.empty();

        return legalMovesForSelectedPiece.stream()
                .filter(move -> move.getStartPosition().equals(start) && move.getEndPosition().equals(end))
                .findFirst();

        // Note: This finds the *first* matching move. For promotion, multiple moves
        // might end on the same square but with different promotion types.
        // The promotion choice needs to be handled *after* finding a base move.
        // Let's adjust: find any move to the target square, promotion handled later.
            /*
            return legalMovesForSelectedPiece.stream()
                .filter(move -> move.getEndPosition().equals(end))
                .findFirst(); // Find *any* legal move ending here
             */
    }

    private void restartGame() {
        int confirmation = JOptionPane.showConfirmDialog(gameFrame,
                "Start a new game?", "Confirm New Game", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            stopTimers();
            gameFrame.dispose(); // Close current game window
            // Relaunch the start menu to get new settings
            SwingUtilities.invokeLater(() -> {
                StartMenu startMenu = new StartMenu(this::setupGame); // Pass method reference
                startMenu.run();
            });
        }
    }

    private void quitGame() {
        int confirmation = JOptionPane.showConfirmDialog(gameFrame,
                "Are you sure you want to quit?", "Confirm Quit", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            stopTimers();
            System.exit(0);
        }
    }

    private void disableBoardInteraction() {
        // Remove listeners or set a flag to ignore events
        // For simplicity, we might just rely on the gameLogic.isGameOver() check
    }

    // --- Inner classes for Mouse Handling ---
    private class BoardMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (gameLogic.isGameOver()) return;
            Point point = e.getPoint();
            Position clickedPos = boardPanel.getPositionFromPoint(point);
            if (clickedPos == null) return;

            Piece clickedPiece = gameLogic.getGameState().getBoard().getPieceAt(clickedPos);
            if (clickedPiece != null && clickedPiece.getColor() == gameLogic.getCurrentPlayerColor()) {
                // Start selecting/dragging own piece
                selectedPosition = clickedPos;
                legalMovesForSelectedPiece = gameLogic.getLegalMovesForPiece(selectedPosition); // Get all legal moves for THIS piece
                boardPanel.selectSquare(selectedPosition);
                boardPanel.setDraggedPiece(clickedPiece, point);

                Set<Position> targetSquares = legalMovesForSelectedPiece.stream()
                        .map(Move::getEndPosition)
                        .collect(Collectors.toSet());
                boardPanel.highlightLegalMoves(targetSquares);
            } else {
                // Pressing on empty or opponent square - clear selection
                clearSelectionAndHighlights();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (gameLogic.isGameOver()) return;

            Point point = e.getPoint();
            Position releasePos = boardPanel.getPositionFromPoint(point);

            // Store selection locally and clear visual drag state immediately
            Position startPos = selectedPosition;
            List<Move> movesToTry = legalMovesForSelectedPiece; // Use the stored legal moves
            boardPanel.clearDraggedPiece(); // Stop drawing dragged piece image

            // Reset controller state *after* using them
            selectedPosition = null;
            legalMovesForSelectedPiece = null;

            if (startPos != null && releasePos != null && movesToTry != null) { // Was dragging a valid piece to a valid square
                // Find the specific move(s) ending at the release position from the pre-calculated legal moves
                List<Move> candidateMoves = movesToTry.stream()
                        .filter(move -> move.getEndPosition().equals(releasePos))
                        .toList(); // Find all moves (standard, promo Q, promo R etc) ending here

                if (!candidateMoves.isEmpty()) {
                    Move moveToAction;
                    // Check if it's a promotion scenario
                    if (candidateMoves.stream().anyMatch(Move::isPromotion)) {
                        PieceType chosenPromotion = gameFrame.askPromotionChoice();
                        // Find the specific promotion move matching the choice
                        moveToAction = candidateMoves.stream()
                                .filter(m -> m.isPromotion() && m.getPromotionPieceType() == chosenPromotion)
                                .findFirst()
                                .orElse(null); // Should find one if askPromotionChoice worked
                    } else {
                        // Standard move (only one candidate expected)
                        moveToAction = candidateMoves.get(0);
                    }

                    // Attempt the chosen move
                    if (moveToAction != null && gameLogic.makeMove(moveToAction)) {
                        // Move successful
                        updateView();
                        startTimer(gameLogic.getCurrentPlayerColor());
                    } else {
                        System.err.println("Illegal move attempted or promotion choice failed: " + (moveToAction != null ? moveToAction : "null"));
                        // Move failed - board will repaint to snap back
                        updateView(); // Repaint to show original state if move failed
                    }
                } else {
                    // Released on an invalid square for the selected piece
                    updateView(); // Repaint to snap back
                }
            } else if (startPos != null && releasePos != null && startPos.equals(releasePos)) {
                // Clicked (pressed and released) on the same square - Keep selection
                // Re-select visually
                selectedPosition = startPos; // Restore selection state
                legalMovesForSelectedPiece = gameLogic.getLegalMovesForPiece(selectedPosition);
                boardPanel.selectSquare(selectedPosition);
                Set<Position> targetSquares = legalMovesForSelectedPiece.stream()
                        .map(Move::getEndPosition)
                        .collect(Collectors.toSet());
                boardPanel.highlightLegalMoves(targetSquares);
                return; // Don't clear highlights below if it was just a re-click

            } else {
                // Click/Release was invalid (e.g., started on empty, ended off board)
                updateView(); // Ensure board state is correct
            }

            // Clear visual selections/highlights if move wasn't made or re-selection didn't occur
            clearSelectionAndHighlights(); // Clear after processing release
        }
    }

    private class BoardMouseMotionListener extends MouseAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (gameLogic.isGameOver() || selectedPosition == null) return;
            Piece draggedPiece = gameLogic.getGameState().getBoard().getPieceAt(selectedPosition);
            if (draggedPiece != null) {
                boardPanel.setDraggedPiece(draggedPiece, e.getPoint());
            }
        }
    }


    // Helper to clear selection visuals and state
    private void clearSelectionAndHighlights() {
        boardPanel.clearDraggedPiece();
        boardPanel.clearHighlights();
        boardPanel.clearSelection();
        selectedPosition = null; // Ensure controller state matches visual state
        legalMovesForSelectedPiece = null;
    }

}