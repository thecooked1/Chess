package main.model.pieces;
import main.model.Board.Board;

public class Queen extends Piece {

    public Queen(Colour colour) {
        super(colour);
        this.symbol = 'Q';
    }

    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Board board) {
        // Check Rook-like movement
        boolean rookMove = false;
        if (fromRow == toRow || fromCol == toCol) {
            if (fromRow != toRow || fromCol != toCol) {
                if(isPathClear(fromRow, fromCol, toRow, toCol, board)) {
                    Piece target = board.getPiece(toRow, toCol);
                    rookMove = (target == null || target.getColor() != this.colour);
                }
            }
        }

        // Check Bishop-like movement
        boolean bishopMove = false;
        if (Math.abs(toRow - fromRow) == Math.abs(toCol - fromCol)) {
            if (fromRow != toRow || fromCol != toCol) {
                if (isPathClear(fromRow, fromCol, toRow, toCol, board)) {
                    Piece target = board.getPiece(toRow, toCol);
                    bishopMove = (target == null || target.getColor() != this.colour);
                }
            }
        }

        return rookMove || bishopMove;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}