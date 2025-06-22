package main.model.pieces;

import main.common.Colour;
import main.model.Board.Board;
import main.common.Square;

public class Rook extends Piece {

    public Rook(Colour colour) {
        super(colour);
        this.symbol = 'R';
    }

    @Override
    public boolean isValidMove(Square from, Square to, Board board) {
        // Rooks move in straight lines
        if (from.rank() != to.rank() && from.file() != to.file()) {
            return false;
        }

        // Check if the path between the squares is clear of other pieces.
        if (!isPathClear(from, to, board)) {
            return false;
        }

        return true;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}