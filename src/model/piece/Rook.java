package model.piece;

import model.*;
import java.util.LinkedList;
import java.util.List;

public class Rook extends Piece {
    private boolean hasMoved = false;

    public Rook(PlayerColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.ROOK;
    }

    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }

    @Override
    public List<Position> getPseudoLegalMoves(BoardModel board, Position currentPos) {
        List<Position> moves = new LinkedList<>();
        int[] dRows = { -1, 1, 0, 0 }; // Up, Down, Left, Right
        int[] dCols = { 0, 0, -1, 1 };

        for (int i = 0; i < 4; i++) {
            addLinearMoves(board, currentPos, moves, dRows[i], dCols[i]);
        }
        return moves;
    }
}
