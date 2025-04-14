package model.piece;

import model.*;
import java.util.LinkedList;
import java.util.List;

public class Bishop extends Piece {

    public Bishop(PlayerColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.BISHOP;
    }

    @Override
    public List<Position> getPseudoLegalMoves(BoardModel board, Position currentPos) {
        List<Position> moves = new LinkedList<>();
        int[] dRows = { -1, -1, 1, 1 }; // NW, NE, SW, SE
        int[] dCols = { -1, 1, -1, 1 };

        for (int i = 0; i < 4; i++) {
            addDiagonalMoves(board, currentPos, moves, dRows[i], dCols[i]);
        }
        return moves;
    }
}