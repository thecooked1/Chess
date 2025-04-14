package controller;

import model.*;
import view.*;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("deprecation") // For Observer
public class GameController implements Observer, ActionListener {

    private ChessGame model;
    private IGameView view; // Use interface for decoupling if needed elsewhere
    private Timer gameTimer;

    private Position selectedPiecePosition = null; // Track selected piece

    public GameController(ChessGame model) {
        this.model = model;
        this.model.addObserver(this); // Register controller as observer of model

        // Timer setup (interval 1000ms = 1 second)
        gameTimer = new Timer(1000, this);
        gameTimer.setInitialDelay(0);
    }

    // Called by Main or Start Menu to link the View
    public void setView(IGameView view) {
        this.view = view;
    }

    // --- Game Lifecycle ---

    public void startApplication() {
        // Launch the Start Menu
        SwingUtilities.invokeLater(() -> {
            StartMenuFrame startMenu = new StartMenuFrame(this); // Pass controller ref
            startMenu.run(); // Build and show UI
        });
    }

    // Called from StartMenuFrame
    public void initializeNewGame(String whiteName, String blackName, int hh, int mm, int ss) {
        // Ensure view exists (it should be created now or before)
        if (view == null) {
            view = new GameFrame(this, whiteName, blackName); // Create the Game Frame
        }
        // Configure the model
        model.startGame(hh, mm, ss);

        // Make the GameFrame visible
        ((GameFrame)view).setVisible(true);

        // Start the timer if the game is timed
        if (model.isClockTimed()) {
            gameTimer.start();
        } else {
            gameTimer.stop(); // Ensure stopped for untimed games
        }
        // Initial status message
        updateStatusMessage();
        view.updateClock(PlayerColor.WHITE, model.getClockTime(PlayerColor.WHITE));
        view.updateClock(PlayerColor.BLACK, model.getClockTime(PlayerColor.BLACK));
        view.initializeBoard(model.getBoardModel().getBoardArray()); // Initial board draw
    }

    // --- Input Handling (from View) ---

    public void handleSquareClick(Position clickedPos) {
        if (model.getGameStatus() != GameStatus.ONGOING && model.getGameStatus() != GameStatus.CHECK) {
            view.clearHighlights(); // Clear highlights if game is over
            return;
        }

        Piece clickedPiece = model.getBoardModel().getPieceAt(clickedPos);

        if (selectedPiecePosition == null) {
            // 1. First click: Select a piece
            if (clickedPiece != null && clickedPiece.getColor() == model.getCurrentTurn()) {
                selectedPiecePosition = clickedPos;
                List<Move> legalMoves = model.getLegalMovesForPieceAt(selectedPiecePosition);
                List<Position> legalTargets = legalMoves.stream().map(Move::getEnd).collect(java.util.stream.Collectors.toList());

                view.clearHighlights(); // Clear previous
                view.highlightSquare(selectedPiecePosition, null); // Highlight selection (color handled by view)
                view.highlightLegalMoves(legalTargets); // Highlight possible moves
            } else {
                // Clicked empty square or opponent's piece initially
                view.clearHighlights();
                selectedPiecePosition = null;
            }
        } else {
            // 2. Second click: Attempt to move or deselect
            if (clickedPos.equals(selectedPiecePosition)) {
                // Clicked same square again: Deselect
                selectedPiecePosition = null;
                view.clearHighlights();
            } else {
                // Attempt the move
                boolean moveMade = model.makeMove(selectedPiecePosition, clickedPos);
                if (moveMade) {
                    // Successful move - model will notify observer (this controller)
                    // which will update the view. Clear selection state here.
                    selectedPiecePosition = null;
                    view.clearHighlights();
                    // Timer continues, status updates via observer
                } else {
                    // Illegal move attempt or clicked non-target square.
                    // Option 1: Deselect current piece
                    // selectedPiecePosition = null;
                    // view.clearHighlights();

                    // Option 2: Check if clicked another friendly piece to switch selection
                    if (clickedPiece != null && clickedPiece.getColor() == model.getCurrentTurn()) {
                        selectedPiecePosition = clickedPos; // Switch selection
                        List<Move> legalMoves = model.getLegalMovesForPieceAt(selectedPiecePosition);
                        List<Position> legalTargets = legalMoves.stream().map(Move::getEnd).collect(java.util.stream.Collectors.toList());
                        view.clearHighlights();
                        view.highlightSquare(selectedPiecePosition, null);
                        view.highlightLegalMoves(legalTargets);
                    } else {
                        // Clicked empty or opponent's piece - treat as deselect
                        selectedPiecePosition = null;
                        view.clearHighlights();
                    }
                }
            }
        }
    }


    public void requestNewGame() {
        int choice = JOptionPane.showConfirmDialog(
                (GameFrame)view, // Requires view to be a JFrame or Component
                "Start a new game? Current game will be lost.",
                "New Game Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            gameTimer.stop();
            view.closeGameWindow(); // Close current game window
            // Relaunch start menu
            startApplication();
        }
    }

    public void requestQuitGame() {
        int choice = JOptionPane.showConfirmDialog(
                (GameFrame)view,
                "Are you sure you want to quit?",
                "Quit Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            gameTimer.stop();
            view.closeGameWindow(); // Close game window
            // Optionally: System.exit(0); // Exit application entirely
        }
    }


    // --- Model Update Handling (Observer Pattern) ---

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof ChessGame) {
            // Ensure updates run on the EDT for Swing safety
            SwingUtilities.invokeLater(() -> {
                if (view == null) return; // View might not be ready yet

                // Update board display
                view.updateBoard(model.getBoardModel().getBoardArray());

                // Update clocks (check if arg is PlayerColor for specific tick)
                if (arg instanceof PlayerColor) {
                    PlayerColor tickedPlayer = (PlayerColor) arg;
                    view.updateClock(tickedPlayer, model.getClockTime(tickedPlayer));
                } else { // General update, refresh both clocks
                    view.updateClock(PlayerColor.WHITE, model.getClockTime(PlayerColor.WHITE));
                    view.updateClock(PlayerColor.BLACK, model.getClockTime(PlayerColor.BLACK));
                }


                // Update status message
                updateStatusMessage();

                // Check for game over
                GameStatus status = model.getGameStatus();
                if (status != GameStatus.ONGOING && status != GameStatus.CHECK) {
                    gameTimer.stop();
                    view.clearHighlights(); // Clear selection/moves on game end
                    PlayerColor winner = null; // Determine winner based on status
                    if (status == GameStatus.CHECKMATE_WHITE_WINS || status == GameStatus.WHITE_WINS_BY_TIMEOUT) {
                        winner = PlayerColor.WHITE;
                    } else if (status == GameStatus.CHECKMATE_BLACK_WINS || status == GameStatus.BLACK_WINS_BY_TIMEOUT) {
                        winner = PlayerColor.BLACK;
                    }
                    view.showGameOver(status, winner);
                }
            });
        }
    }

    // --- Timer Action Handling ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == gameTimer) {
            // Timer ticked, tell the model to decrement the current player's clock
            model.decrementClock(model.getCurrentTurn());
            // The model.decrementClock() will notify observers if time runs out or just ticks
        }
    }

    // --- Helper Methods ---
    private void updateStatusMessage() {
        GameStatus status = model.getGameStatus();
        String message = "";
        switch(status) {
            case ONGOING:
                message = model.getCurrentTurn() + "'s Turn";
                break;
            case CHECK: // If CHECK status is implemented in RuleEngine/ChessGame
                message = model.getCurrentTurn() + "'s Turn (Check!)";
                break;
            case CHECKMATE_WHITE_WINS: message = "Checkmate! White wins."; break;
            case CHECKMATE_BLACK_WINS: message = "Checkmate! Black wins."; break;
            case STALEMATE: message = "Stalemate! Draw."; break;
            case WHITE_WINS_BY_TIMEOUT: message = "White wins on time."; break;
            case BLACK_WINS_BY_TIMEOUT: message = "Black wins on time."; break;
        }
        if (view != null) {
            view.setStatusMessage(message);
        }
    }
}