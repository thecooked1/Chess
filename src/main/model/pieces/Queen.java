package main.model.pieces;

import main.model.*;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {

    public Queen(Color color) {
        super(color, PieceType.QUEEN);
    }

    @Override
    public List<Position> getPseudoLegalMoves(Board board, Position currentPosition) {
        List<Position> moves = new ArrayList<>();
        // Combine Rook and Bishop directions
        int[][] directions = {
                {0, 1}, {0, -1}, {1, 0}, {-1, 0}, // Rook moves
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // Bishop moves
        };

        for (int[] dir : directions) {
            addMovesInDirection(moves, board, currentPosition, dir[0], dir[1]);
        }
        return moves;
    }
}