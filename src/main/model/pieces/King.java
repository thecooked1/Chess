package main.model.pieces;
import main.model.Board.Board;

public class King extends Piece {

    private boolean hasMoved = false;

    public King(Colour colour) {
        super(colour);
        this.symbol = 'K';
        this.hasMoved = false;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean moved) {
        this.hasMoved = moved;
    }

    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Board board) {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);

        // King move
        if (rowDiff <= 1 && colDiff <= 1 && (rowDiff != 0 || colDiff != 0)) {
            Piece targetPiece = board.getPiece(toRow, toCol);
            if (targetPiece == null) {
                return true;
            } else {
                return targetPiece.getColor() != this.colour; // Can capture opponent's piece
            }
        }
        return false;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}