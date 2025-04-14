package model.piece;

import model.*;
import java.util.LinkedList;
import java.util.List;

public class Queen extends Piece {

    public Queen(PlayerColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.QUEEN;
    }

    @Override
    public List<Position> getPseudoLegalMoves(BoardModel board, Position currentPos) {
        List<Position> moves = new LinkedList<>();
        // Combine Rook and Bishop moves
        int[] dRows = { -1, 1, 0, 0, -1, -1, 1, 1 }; // Linear + Diagonal
        int[] dCols = { 0, 0, -1, 1, -1, 1, -1, 1 };

        for (int i = 0; i < 8; i++) {
            // Using addLinearMoves, as diagonal is the same logic
            addLinearMoves(board, currentPos, moves, dRows[i], dCols[i]);
        }
        return moves;
    }
}