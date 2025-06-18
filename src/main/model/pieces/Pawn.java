package main.model.pieces;
import main.model.Board.Board;

public class Pawn extends Piece {

    public Pawn(Colour colour) {
        super(colour);
        this.symbol = 'P';
    }

    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Board board) {
        int direction = (colour == Colour.WHITE) ? -1 : 1;
        int startRankRow = (colour == Colour.WHITE) ? 6 : 1;
        Piece targetPiece = board.getPiece(toRow, toCol);
        // Single move forward
        if (fromCol == toCol && toRow == fromRow + direction && targetPiece == null) {
            return true;
        }

        // Double move forward
        if (fromCol == toCol && fromRow == startRankRow && toRow == fromRow + 2 * direction
                && targetPiece == null
                && board.getPiece(fromRow + direction, fromCol) == null) {
            return true;
        }

        // Capture
        if (Math.abs(toCol - fromCol) == 1 && toRow == fromRow + direction && targetPiece != null
                && targetPiece.getColor() != this.colour) {
            return true;
        }

        //  En Passant Capture
        int[] enPassantTarget = board.getEnPassantTarget(); // Get potential target {row, col}
        if (enPassantTarget != null
                && Math.abs(toCol - fromCol) == 1 // Moving diagonally
                && toRow == fromRow + direction    // Moving one step forward direction-wise
                && targetPiece == null            // Target square *must* be empty for en passant
                && toRow == enPassantTarget[0]    // Target row must match en passant row
                && toCol == enPassantTarget[1]) { // Target col must match en passant col
            return true;
        }
        return false;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}