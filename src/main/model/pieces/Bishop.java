package main.model.pieces;
import main.model.Board.Board;

public class Bishop extends Piece {

    public Bishop(Colour colour) {
        super(colour);
        this.symbol = 'B';
    }

    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Board board) {
        // Must move diagonally
        if (Math.abs(toRow - fromRow) != Math.abs(toCol - fromCol)) {
            return false;
        }
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }

        // Check if the path is clear
        if (!isPathClear(fromRow, fromCol, toRow, toCol, board)) {
            return false;
        }

        // Check the target square
        Piece targetPiece = board.getPiece(toRow, toCol);
        if (targetPiece == null) {
            return true;
        } else {
            return targetPiece.getColor() != this.colour; // Can capture opponent's piece
        }
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}