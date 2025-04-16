package model.piece;

import java.util.List;
import model.*;

public class Queen extends Piece {

    public Queen(PieceColor color, Square initSq, String img_file) {
        super(color, PieceType.QUEEN, initSq, img_file);
    }

    @Override
    public List<Square> getPotentialMoves(Board board) {
        // Queen moves linearly and diagonally
        List<Square> moves = getPotentialLinearMoves(board);
        moves.addAll(getPotentialDiagonalMoves(board));
        return moves;
    }
}