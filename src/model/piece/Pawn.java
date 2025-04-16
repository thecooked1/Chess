package model.piece;

import java.util.ArrayList;
import java.util.List;
import model.*;

public class Pawn extends Piece {
    private boolean hasMoved;
    private int promotionRank; // 7 for White, 0 for Black

    public Pawn(PieceColor color, Square initSq, String img_file) {
        super(color, PieceType.PAWN, initSq, img_file);
        this.hasMoved = false; // Will be updated by game logic/initial setup
        this.promotionRank = (color == PieceColor.WHITE) ? 7 : 0;
        // Determine initial hasMoved status based on starting rank
        if (color == PieceColor.WHITE && initSq.getRank() != 1) {
            this.hasMoved = true;
        } else if (color == PieceColor.BLACK && initSq.getRank() != 6) {
            this.hasMoved = true;
        }
    }

    /**
     * Marks this pawn as having moved. Should be called by game logic
     * after the pawn completes its first move.
     */
    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean hasMoved() {
        return this.hasMoved;
    }

    public int getPromotionRank() {
        return promotionRank;
    }

    @Override
    public List<Square> getPotentialMoves(Board board) {
        List<Square> moves = new ArrayList<>();
        Square[][] grid = board.getSquareGrid();
        int currentFile = getPosition().getFile();
        int currentRank = getPosition().getRank();
        int direction = (getColor() == PieceColor.WHITE) ? 1 : -1;

        // 1. Forward move(s)
        int oneStepRank = currentRank + direction;
        if (Board.isValidCoordinate(currentFile, oneStepRank)) {
            Square oneStepSquare = grid[currentFile][oneStepRank];
            if (!oneStepSquare.isOccupied()) {
                moves.add(oneStepSquare);
                // 2. Double step (only if one step is clear and pawn hasn't moved)
                if (!hasMoved) {
                    int twoStepRank = currentRank + 2 * direction;
                    if (Board.isValidCoordinate(currentFile, twoStepRank)) {
                        Square twoStepSquare = grid[currentFile][twoStepRank];
                        if (!twoStepSquare.isOccupied()) {
                            moves.add(twoStepSquare);
                        }
                    }
                }
            }
        }

        // 3. Captures
        int[] captureFiles = { currentFile - 1, currentFile + 1 };
        int captureRank = currentRank + direction;
        if (Board.isValidCoordinate(0, captureRank)) { // Only need to check rank validity once
            for (int captureFile : captureFiles) {
                if (Board.isValidCoordinate(captureFile, captureRank)) {
                    Square captureSquare = grid[captureFile][captureRank];
                    if (captureSquare.isOccupied() && captureSquare.getOccupyingPiece().getColor() != getColor()) {
                        moves.add(captureSquare);
                    }
                    // 4. En Passant (check if target square matches game state's en passant target)
                    // Note: The GameLogic needs to provide the en passant target square.
                    // This method only identifies potential captures *onto* the en passant rank.
                    // Actual validation happens in GameLogic.
                    Square enPassantTarget = board.getGameState() == null ? null : board.getGameState().getEnPassantTargetSquare();
                    if (enPassantTarget != null &&
                            captureSquare.equals(enPassantTarget) &&
                            !captureSquare.isOccupied() && // En passant target square must be empty
                            Math.abs(currentRank - enPassantTarget.getRank()) == 0 ) { // Must be on correct rank to perform EP
                        moves.add(captureSquare);
                    }
                }
            }
        }

        // Note: Promotion is not handled here. GameLogic detects when a pawn reaches
        // the promotion rank *after* a move is made.

        return moves;
    }
}