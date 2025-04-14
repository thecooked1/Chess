package model;

import model.piece.*; // If needed for specific checks like promotion

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable; // Using legacy Observable for simplicity, consider PropertyChangeSupport

@SuppressWarnings("deprecation") // Suppress Observable deprecation warning
public class ChessGame extends Observable { // Observable notifies Observers (Controller)

    private BoardModel boardModel;
    private PlayerColor currentTurn;
    private GameStatus gameStatus;
    private Map<PlayerColor, ClockModel> clocks;
    private List<Piece> capturedWhitePieces;
    private List<Piece> capturedBlackPieces;
    private RuleEngine ruleEngine;

    // Add state for castling rights, en passant target square
    private boolean whiteCanCastleKingSide = true;
    private boolean whiteCanCastleQueenSide = true;
    private boolean blackCanCastleKingSide = true;
    private boolean blackCanCastleQueenSide = true;
    private Position enPassantTargetSquare = null; // Square behind pawn after double move

    public ChessGame() {
        this.boardModel = new BoardModel();
        this.clocks = new HashMap<>();
        this.capturedWhitePieces = new ArrayList<>();
        this.capturedBlackPieces = new ArrayList<>();
        this.ruleEngine = new RuleEngine();
        // Initialize clocks - default to untimed or use specific values
        this.clocks.put(PlayerColor.WHITE, new ClockModel(0, 0, 0));
        this.clocks.put(PlayerColor.BLACK, new ClockModel(0, 0, 0));
    }

    public void startGame(int hh, int mm, int ss) {
        boardModel.setupInitialPieces();
        currentTurn = PlayerColor.WHITE;
        gameStatus = GameStatus.ONGOING;
        capturedWhitePieces.clear();
        capturedBlackPieces.clear();
        clocks.put(PlayerColor.WHITE, new ClockModel(hh, mm, ss));
        clocks.put(PlayerColor.BLACK, new ClockModel(hh, mm, ss));
        resetCastlingRights();
        enPassantTargetSquare = null;

        setChanged(); // Mark state as changed
        notifyObservers(); // Notify observers (Controller)
    }

    private void resetCastlingRights() {
        whiteCanCastleKingSide = true;
        whiteCanCastleQueenSide = true;
        blackCanCastleKingSide = true;
        blackCanCastleQueenSide = true;
        // Could refine this by checking initial king/rook positions if setup changes
    }

    public BoardModel getBoardModel() {
        return boardModel;
    }

    public PlayerColor getCurrentTurn() {
        return currentTurn;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public String getClockTime(PlayerColor color) {
        return clocks.get(color).getTimeString();
    }

    public boolean isClockTimed() {
        // Check if either clock is timed
        return clocks.get(PlayerColor.WHITE).isTimed() || clocks.get(PlayerColor.BLACK).isTimed();
    }

    public List<Piece> getCapturedPieces(PlayerColor color) {
        return (color == PlayerColor.WHITE) ? capturedWhitePieces : capturedBlackPieces;
    }

    public List<Move> getLegalMovesForPieceAt(Position pos) {
        // Delegate to RuleEngine, passing the current game state
        return ruleEngine.generateLegalMovesForPiece(this, pos);
    }


    /**
     * Attempts to make a move. Returns true if successful, false otherwise.
     */
    public boolean makeMove(Position start, Position end) {
        if (gameStatus != GameStatus.ONGOING && gameStatus != GameStatus.CHECK) {
            System.out.println("Game is over. Cannot make move.");
            return false; // Game finished
        }

        Piece movingPiece = boardModel.getPieceAt(start);
        if (movingPiece == null || movingPiece.getColor() != currentTurn) {
            System.out.println("Invalid piece selected or not current player's turn.");
            return false; // No piece or wrong color
        }

        // Find the specific legal move object corresponding to start/end
        Move legalMove = null;
        List<Move> legalMoves = getLegalMovesForPieceAt(start);
        for(Move m : legalMoves) {
            if (m.getEnd().equals(end)) {
                legalMove = m;
                break;
            }
        }

        if (legalMove == null) {
            System.out.println("Illegal move attempted: " + start + " to " + end);
            return false; // Not a legal move destination
        }

        // --- Move Execution ---
        Piece captured = boardModel.applyMove(legalMove); // Apply move updates board and piece state (hasMoved)

        if (captured != null) {
            if (captured.getColor() == PlayerColor.WHITE) {
                capturedWhitePieces.add(captured);
            } else {
                capturedBlackPieces.add(captured);
            }
        }

        // Update Castling Rights (simple version)
        updateCastlingRights(movingPiece, start);

        // Update En Passant Target Square
        updateEnPassantTarget(movingPiece, start, end);

        // Handle Pawn Promotion (needs UI interaction via Controller)
        // if (movingPiece instanceof Pawn && (end.getRow() == 0 || end.getRow() == 7)) {
        //     // Need to pause here, ask controller/view for promotion choice
        //     // Piece promotedPiece = controller.getPromotionChoice(currentTurn);
        //     // boardModel.placePiece(promotedPiece, end);
        //     System.out.println("Pawn Promotion required!"); // Placeholder
        // }


        // Switch Turn
        currentTurn = currentTurn.opposite();

        // Update Game Status
        gameStatus = ruleEngine.getGameStatus(this); // Recalculate status after move

        // --- Notify Observers ---
        setChanged(); // Mark state as changed
        notifyObservers(legalMove); // Notify with the move made

        return true; // Move was successful
    }

    /** Decrements the clock for the specified player. */
    public void decrementClock(PlayerColor player) {
        if (gameStatus != GameStatus.ONGOING && gameStatus != GameStatus.CHECK) return;
        ClockModel clock = clocks.get(player);
        if (clock.isTimed()) {
            clock.decrementSecond();
            if (clock.isOutOfTime()) {
                // Determine winner based on timeout
                gameStatus = (player == PlayerColor.WHITE) ? GameStatus.BLACK_WINS_BY_TIMEOUT : GameStatus.WHITE_WINS_BY_TIMEOUT;
                setChanged();
                notifyObservers(); // Notify about status change due to time
            } else {
                // Notify only about clock update if time hasn't run out
                setChanged();
                notifyObservers(player); // Pass player whose clock ticked
            }
        }
    }

    // --- Helper methods for game state updates ---

    private void updateCastlingRights(Piece movedPiece, Position startPos) {
        if (movedPiece instanceof King) {
            if (movedPiece.getColor() == PlayerColor.WHITE) {
                whiteCanCastleKingSide = false;
                whiteCanCastleQueenSide = false;
            } else {
                blackCanCastleKingSide = false;
                blackCanCastleQueenSide = false;
            }
        } else if (movedPiece instanceof Rook) {
            // Check if the rook moved from its starting square
            if (movedPiece.getColor() == PlayerColor.WHITE) {
                if (startPos.equals(new Position(7, 0))) whiteCanCastleQueenSide = false;
                if (startPos.equals(new Position(7, 7))) whiteCanCastleKingSide = false;
            } else { // Black Rook
                if (startPos.equals(new Position(0, 0))) blackCanCastleQueenSide = false;
                if (startPos.equals(new Position(0, 7))) blackCanCastleKingSide = false;
            }
        }
        // Also need to disable if opponent captures a rook on its starting square
        // This needs refinement if captured piece is needed
    }

    private void updateEnPassantTarget(Piece movedPiece, Position start, Position end) {
        // Reset target first
        enPassantTargetSquare = null;
        // If a pawn moved two squares, set the target square behind it
        if (movedPiece instanceof Pawn && Math.abs(start.getRow() - end.getRow()) == 2) {
            int targetRow = (start.getRow() + end.getRow()) / 2; // Row between start and end
            enPassantTargetSquare = new Position(targetRow, start.getCol());
        }
    }

    // --- Getters for special state needed by RuleEngine ---
    public boolean canCastleKingSide(PlayerColor color) {
        return (color == PlayerColor.WHITE) ? whiteCanCastleKingSide : blackCanCastleKingSide;
    }
    public boolean canCastleQueenSide(PlayerColor color) {
        return (color == PlayerColor.WHITE) ? whiteCanCastleQueenSide : blackCanCastleQueenSide;
    }
    public Position getEnPassantTargetSquare() {
        return enPassantTargetSquare;
    }
}