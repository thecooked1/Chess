package model.piece;

import model.*;
import java.util.LinkedList;
import java.util.List;

public class Knight extends Piece {

    public Knight(PlayerColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.KNIGHT;
    }

    @Override
    public List<Position> getPseudoLegalMoves(BoardModel board, Position currentPos) {
        List<Position> moves = new LinkedList<>();
        int r = currentPos.getRow();
        int c = currentPos.getCol();

        int[] dr = { -2, -2, -1, -1, 1, 1, 2, 2 }; // Possible row changes
        int[] dc = { -1, 1, -2, 2, -2, 2, -1, 1 }; // Possible col changes

        for (int i = 0; i < 8; i++) {
            int targetRow = r + dr[i];
            int targetCol = c + dc[i];

            if (Position.isValid(targetRow, targetCol)) {
                Position targetPos = new Position(targetRow, targetCol);
                Piece targetPiece = board.getPieceAt(targetPos);
                if (targetPiece == null || targetPiece.getColor() != this.color) {
                    moves.add(targetPos);
                }
            }
        }
        return moves;
    }
}
