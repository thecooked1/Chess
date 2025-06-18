package main.model.pieces;
import main.model.Board.Board;

public class Rook extends Piece {

    private boolean hasMoved = false;

    public Rook(Colour colour) {
        super(colour);
        this.symbol = 'R';
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
        // move
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }

        // Check if the path is clear using the helper method
        if (!isPathClear(fromRow, fromCol, toRow, toCol, board)) {
            return false;
        }

        Piece targetPiece = board.getPiece(toRow, toCol);
        if (targetPiece == null) {
            return true;
        } else {
            return targetPiece.getColor() != this.colour;
        }
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}