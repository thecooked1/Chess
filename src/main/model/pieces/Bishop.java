package main.model.pieces;

import main.model.*;

import java.util.ArrayList;
import java.util.List;

public class Bishop extends Piece {

    public Bishop(Color color) {
        super(color, PieceType.BISHOP);
    }

    @Override
    public List<Position> getPseudoLegalMoves(Board board, Position currentPosition) {
        List<Position> moves = new ArrayList<>();
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}; // Down-Right, Down-Left, Up-Right, Up-Left

        for (int[] dir : directions) {
            addMovesInDirection(moves, board, currentPosition, dir[0], dir[1]);
        }
        return moves;
    }
}