package model.piece;

import model.*;
import java.util.LinkedList;
import java.util.List;

public class King extends Piece {
    private boolean hasMoved = false;

    public King(PlayerColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.KING;
    }

    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }


    @Override
    public List<Position> getPseudoLegalMoves(BoardModel board, Position currentPos) {
        List<Position> moves = new LinkedList<>();
        int r = currentPos.getRow();
        int c = currentPos.getCol();

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue; // Skip the current square

                int targetRow = r + dr;
                int targetCol = c + dc;

                if (Position.isValid(targetRow, targetCol)) {
                    Position targetPos = new Position(targetRow, targetCol);
                    Piece targetPiece = board.getPieceAt(targetPos);
                    if (targetPiece == null || targetPiece.getColor() != this.color) {
                        moves.add(targetPos);
                    }
                }
            }
        }
        // Castling moves are generated/validated in RuleEngine, not here.
        return moves;
    }
}