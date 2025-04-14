package model.piece;

import model.*;
import java.util.LinkedList;
import java.util.List;

public class Pawn extends Piece {
    private boolean hasMoved = false;
    // Add flag for en passant vulnerability if implementing

    public Pawn(PlayerColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.PAWN;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    @Override
    public List<Position> getPseudoLegalMoves(BoardModel board, Position currentPos) {
        List<Position> moves = new LinkedList<>();
        int r = currentPos.getRow();
        int c = currentPos.getCol();
        int direction = (color == PlayerColor.WHITE) ? -1 : 1; // White moves up (-row), Black moves down (+row)
        int startRow = (color == PlayerColor.WHITE) ? 6 : 1;

        // 1. Forward move
        Position oneStep = new Position(r + direction, c);
        if (Position.isValid(oneStep.getRow(), oneStep.getCol()) && board.isEmpty(oneStep)) {
            moves.add(oneStep);
            // 2. Double step forward (only if first move was possible)
            if (r == startRow) { // Check if pawn is on starting row
                Position twoStep = new Position(r + 2 * direction, c);
                if (Position.isValid(twoStep.getRow(), twoStep.getCol()) && board.isEmpty(twoStep)) {
                    moves.add(twoStep);
                }
            }
        }

        // 3. Captures
        int[] captureCols = {c - 1, c + 1};
        for (int captureCol : captureCols) {
            if (Position.isValid(r + direction, captureCol)) {
                Position capturePos = new Position(r + direction, captureCol);
                Piece target = board.getPieceAt(capturePos);
                if (target != null && target.getColor() != this.color) {
                    moves.add(capturePos);
                }
                // Add En Passant check here if implementing
            }
        }

        // Promotion is handled when the move is *made*, not generated here.
        return moves;
    }
}