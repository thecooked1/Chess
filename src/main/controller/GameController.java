package main.controller;

import main.model.Board.Board;
import main.model.PGNParser.Interpreter;
import main.model.PGNParser.Move;
import main.model.PGNParser.Parser;

// --- Imports for the View ---
import main.model.Position;
import main.model.pieces.Colour;
import main.model.pieces.Pawn;
import main.model.pieces.Piece;
import main.view.ChessBoardPanel;
import main.view.GameFrame;
import main.view.StartMenu; // For restarting the game

// --- Standard Java/Swing Imports ---
import javax.swing.*;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;



public class GameController {

    private final GameFrame view;
    private final Board board; // Your powerful Board engine is the primary model
    private final Interpreter pgnInterpreter;

    private Position selectedPosition = null;
    private List<Move> legalMovesForSelectedPiece = null;

    // State for PGN Replay
    private List<String> pgnMoveTokens;
    private int currentPgnMoveIndex;
    private boolean isWhiteTurn = true;

    public GameController() {
        // 1. Create the View and the Model
        this.view = new GameFrame();
        this.board = new Board(); // The Board's constructor calls setupInitialPosition()
        this.pgnInterpreter = new Interpreter();

        // 2. Initial display
        this.view.getChessBoardPanel().updateBoard(this.board);
        //this.view.setStatus("Load a PGN file to start replay.");

        // 3. Connect Controller to View (add event listeners)
        this.initListeners();

        // 4. Make the application visible
        this.view.setVisible(true);
    }

    /**
     * Wires up all the buttons from the View to methods in this Controller.
     */
    private void initListeners() {
        this.view.getLoadPgnMenuItem().addActionListener(e -> handleLoadPgn());
        this.view.getNextMoveButton().addActionListener(e -> handlePgnNextMove());
        this.view.getPrevMoveButton().addActionListener(e -> handlePgnPrevMove()); // For later
        this.view.addQuitListener(e -> quitGame());
        BoardMouseListener mouseListener = new BoardMouseListener();
        this.view.getChessBoardPanel().addMouseListener(mouseListener);
        this.view.getChessBoardPanel().addMouseMotionListener(mouseListener);
    }

    /**
     * Handles the "Load PGN..." menu item click.
     * Opens a file chooser, parses the PGN, and prepares for replay.
     */
    private void handleLoadPgn() {
        JFileChooser fileChooser = new JFileChooser("./"); // Start in the project directory
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

                // Load the first game from the file
                Parser.PGNGame firstGame = pgnParser.getGames().get(0);
                this.pgnMoveTokens = firstGame.getMoves();
                this.currentPgnMoveIndex = -1; // -1 means we are at the start position
                this.isWhiteTurn = true;

                // Reset the board engine to the initial position
                this.board.setupInitialPosition();

                // Update the GUI
                this.view.getChessBoardPanel().updateBoard(this.board);
                this.view.updatePlayerInfo(firstGame.getHeaders());
                this.view.setStatus("Game loaded. Click 'Next' to start.");
                this.view.enableReplayControls(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view, "Failed to load or parse PGN file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles the "Next >" button click.
     * Applies the next move from the loaded PGN to the board.
     */
    private void handlePgnNextMove() {
        if (pgnMoveTokens == null || currentPgnMoveIndex >= pgnMoveTokens.size() - 1) {
            view.setStatus("End of game.");
            return; // No more moves
        }

        currentPgnMoveIndex++;
        String moveToken = pgnMoveTokens.get(currentPgnMoveIndex);

        try {
            // Use your PGN interpreter to parse the move string
            Move pgnMove = pgnInterpreter.parseMove(moveToken);

            // Use your board engine to apply the move
            boolean success = board.applyMove(pgnMove, isWhiteTurn);

            if (success) {
                // Update the GUI with the new board state
                view.getChessBoardPanel().updateBoard(board);
                isWhiteTurn = !isWhiteTurn; // Toggle turn
                String nextPlayer = isWhiteTurn ? "White" : "Black";
                view.setStatus("Move " + (currentPgnMoveIndex + 1) + ": " + moveToken + ". " + nextPlayer + "'s turn.");
            } else {
                String errorMsg = "Illegal move encountered during replay: " + moveToken;
                System.err.println(errorMsg);
                view.setStatus(errorMsg);
                JOptionPane.showMessageDialog(view, errorMsg, "Illegal Move", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            String errorMsg = "Error parsing move token: " + moveToken;
            System.err.println(errorMsg);
            e.printStackTrace();
            view.setStatus(errorMsg);
            JOptionPane.showMessageDialog(view, errorMsg, "Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Placeholder for "Previous" button functionality.
     * This is complex and can be implemented later.
     */
    private void handlePgnPrevMove() {
        JOptionPane.showMessageDialog(view, "Previous move functionality not yet implemented.");
        // To implement this, you would reset the board and replay all moves up to (currentPgnMoveIndex - 1).
    }

    private void quitGame() {
        int confirmation = JOptionPane.showConfirmDialog(view, "Are you sure you want to quit?", "Confirm Quit", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_NO_OPTION) {
            System.exit(0);
        }
    }

    private class BoardMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            // Don't allow interaction during PGN replay
            if (pgnMoveTokens != null) return;

            Point point = e.getPoint();
            ChessBoardPanel boardPanel = view.getChessBoardPanel();
            Position clickedPos = boardPanel.getPositionFromPoint(point);
            if (clickedPos == null) return;

            Piece clickedPiece = board.getPiece(clickedPos.getRow(), clickedPos.getCol());

            // Check if the player clicked their own piece
            Colour currentTurnColor = isWhiteTurn ? Colour.WHITE : Colour.BLACK;
            if (clickedPiece != null && clickedPiece.getColor() == currentTurnColor) {
                selectedPosition = clickedPos;

                // Show visual feedback for dragging
                boardPanel.setDraggedPiece(clickedPiece, point);

                // Highlight the selected square
                boardPanel.selectSquare(selectedPosition);

                // 1. Ask the engine for all legal moves for the selected piece.
                List<Position> legalTargets = board.getLegalMovesForPiece(selectedPosition);

                // 2. Convert the List to a Set for the highlight method.
                Set<Position> highlightSet = new HashSet<>(legalTargets);

                // 3. Tell the view to draw the highlights.
                boardPanel.highlightLegalMoves(highlightSet);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // If no piece was selected during mousePressed, do nothing.
            if (selectedPosition == null) return;

            ChessBoardPanel boardPanel = view.getChessBoardPanel();
            Position releasePos = boardPanel.getPositionFromPoint(e.getPoint());

            // Store the start position before clearing state
            Position startPos = selectedPosition;

            // --- Always clear visual feedback first ---
            boardPanel.clearDraggedPiece();
            boardPanel.clearHighlights();
            boardPanel.clearSelection();
            selectedPosition = null;

            if (releasePos != null) {
                Optional<String> promotionChoice = Optional.empty();

                // Check if this move is a promotion
                Piece movingPiece = board.getPiece(startPos.getRow(), startPos.getCol());
                if (movingPiece instanceof Pawn) {
                    int promotionRank = (movingPiece.getColor() == Colour.WHITE) ? 0 : 7;
                    if (releasePos.getRow() == promotionRank) {
                        // Ask the user for their choice
                        promotionChoice = Optional.of(view.askPromotionChoice());
                    }
                }

                // --- Call the new, robust engine method ---
                boolean success = board.applyMove(startPos, releasePos, promotionChoice);

                if (success) {
                    isWhiteTurn = !isWhiteTurn; // Switch turns only on successful move
                }
            }

            // Always update the view to show the result (either the new board or the snapped-back piece)
            updateView();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedPosition != null) {
                view.getChessBoardPanel().setDraggedPiece(board.getPiece(selectedPosition.getRow(), selectedPosition.getCol()), e.getPoint());
            }
        }
    }

    private void updateView() {
        view.getChessBoardPanel().updateBoard(board);

        // Check for game over states
        Colour currentTurnColor = isWhiteTurn ? Colour.WHITE : Colour.BLACK;
        if (board.isInCheck(currentTurnColor) && !board.hasAnyLegalMoves(currentTurnColor)) {
            view.setStatus("Checkmate! " + (isWhiteTurn ? "Black" : "White") + " wins.");
        } else if (!board.isInCheck(currentTurnColor) && !board.hasAnyLegalMoves(currentTurnColor)) {
            view.setStatus("Stalemate! It's a draw.");
        } else {
            String status = (isWhiteTurn ? "White" : "Black") + "'s turn";
            if (board.isInCheck(currentTurnColor)) {
                status += " (Check)";
            }
            view.setStatus(status);
        }
    }

    // --- Helper methods for mouse logic ---

    // To be implemented in Step 3
    private void highlightLegalMovesFor(Position startPos) { }
    private Optional<Move> findMove(Position start, Position end) { return Optional.empty(); }
    private boolean isPromotionMove(Position start, Position end) { return false; }
}


