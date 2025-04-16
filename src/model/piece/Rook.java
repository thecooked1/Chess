package model.piece;

import java.util.List;
import model.*;

public class Rook extends Piece {
    private boolean hasMoved;

    public Rook(PieceColor color, Square initSq, String img_file) {
        super(color, PieceType.ROOK, initSq, img_file);
        this.hasMoved = false; // Assume starts unmoved, GameLogic updates
        // Determine initial hasMoved status based on starting position
        if (color == PieceColor.WHITE && initSq.getRank() == 0 && (initSq.getFile() == 0 || initSq.getFile() == 7)) {
            this.hasMoved = false;
        } else if (color == PieceColor.BLACK && initSq.getRank() == 7 && (initSq.getFile() == 0 || initSq.getFile() == 7)) {
            this.hasMoved = false;
        } else {
            this.hasMoved = true; // If not in a standard starting pos, assume moved
        }
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean hasMoved() {
        return this.hasMoved;
    }


    @Override
    public List<Square> getPotentialMoves(Board board) {
        // Rooks move linearly (ranks and files)
        return getPotentialLinearMoves(board);
    }
}