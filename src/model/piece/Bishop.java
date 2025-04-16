package model.piece;

import java.util.List;
import model.*;

public class Bishop extends Piece {

    public Bishop(PieceColor color, Square initSq, String img_file) {
        super(color, PieceType.BISHOP, initSq, img_file);
    }

    @Override
    public List<Square> getPotentialMoves(Board board) {
        // Bishops move diagonally
        return getPotentialDiagonalMoves(board);
    }
}