package main.model.pieces;

import main.model.*;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {

    public Rook(Color color) {
        super(color, PieceType.ROOK);
    }

    @Override
    public List<Position> getPseudoLegalMoves(Board board, Position currentPosition) {
        List<Position> moves = new ArrayList<>();
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}; // Right, Left, Down, Up

        for (int[] dir : directions) {
            addMovesInDirection(moves, board, currentPosition, dir[0], dir[1]);
        }
        return moves;
    }
}