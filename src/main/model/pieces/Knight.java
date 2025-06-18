package main.model.pieces;
import main.model.Board.Board;

public class Knight extends Piece {

    public Knight(Colour colour) {
        super(colour);
        this.symbol = 'N';
    }

    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Board board) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        // L-shape move
        if ((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)) {
            //check the target square.
            Piece targetPiece = board.getPiece(toRow, toCol);
            if (targetPiece == null) {
                return true;
            } else {
                return targetPiece.getColor() != this.colour;
            }
        }
        return false;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}