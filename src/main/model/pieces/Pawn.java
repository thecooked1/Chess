package main.model.pieces;

import main.model.Board;
import main.model.Color;
import main.model.Position;
import main.model.Piece;
import main.model.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Position> getPseudoLegalMoves(Board board, Position currentPosition) {
        List<Position> moves = new ArrayList<>();
        int currentRow = currentPosition.getRow();
        int currentCol = currentPosition.getCol();
        int direction = (color == Color.WHITE) ? -1 : 1; // White moves up (-row), Black moves down (+row)
        int startRow = (color == Color.WHITE) ? 6 : 1;

        // 1. Move Forward One Square
        int oneStepRow = currentRow + direction;
        int oneStepCol = currentCol;
        if (oneStepRow >= 0 && oneStepRow < 8) { // Check row bound
            Position oneStep = new Position(oneStepRow, oneStepCol);
            // No need to check isValidPosition again, as we already checked row/col
            if (board.getPieceAt(oneStep) == null) {
                moves.add(oneStep);

                // 2. Move Forward Two Squares (only from starting position and if one step was clear)
                if (currentRow == startRow) {
                    int twoStepsRow = currentRow + 2 * direction;
                    int twoStepsCol = currentCol;
                    // Need to check row bound for two steps as well
                    if (twoStepsRow >= 0 && twoStepsRow < 8) {
                        Position twoSteps = new Position(twoStepsRow, twoStepsCol);
                        // No need for isValidPosition check here either
                        if (board.getPieceAt(twoSteps) == null) {
                            moves.add(twoSteps);
                        }
                    }
                }
            }
        }


        // 3. Capture Diagonally
        int captureRow = currentRow + direction;
        // Check if the target row for capture is valid first
        if (captureRow >= 0 && captureRow < 8) {
            int[] captureCols = {currentCol - 1, currentCol + 1};
            for (int captureCol : captureCols) {
                // --- Check if the calculated column is within bounds ---
                if (captureCol >= 0 && captureCol < 8) {
                    // --- Only create Position object if coordinates are valid ---
                    Position capturePos = new Position(captureRow, captureCol);
                    // No need for board.isValidPosition(capturePos) check anymore

                    Piece targetPiece = board.getPieceAt(capturePos);
                    if (targetPiece != null && targetPiece.getColor() != this.color) {
                        moves.add(capturePos);
                    }
                    // Note: En Passant check needs separate logic using GameState.enPassantTarget
                }
                // If captureCol was out of bounds, skip creating Position
            }
        }

        // Note: Promotion is handled by checking the *result* of these moves in GameState/GameLogic,
        // not by generating specific promotion moves here.

        return moves;
    }
}