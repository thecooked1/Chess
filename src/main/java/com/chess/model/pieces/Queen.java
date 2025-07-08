package com.chess.model.pieces;

import com.chess.common.Colour;
import com.chess.model.Board.Board;
import com.chess.common.Square;

public class Queen extends Piece {

    public Queen(Colour colour) {
        super(colour);
        this.symbol = 'Q';
    }
    @Override
    public Piece copy() {
        Queen newQueen = new Queen(this.getColor());
        newQueen.setMoved(this.hasMoved());
        return newQueen;
    }

    @Override
    public boolean isValidMove(Square from, Square to, Board board) {
        // Check for straight-line movement (like a Rook)
        boolean isRookMove = from.rank() == to.rank() || from.file() == to.file();

        // Check for diagonal movement (like a Bishop)
        boolean isBishopMove = Math.abs(from.rank() - to.rank()) == Math.abs(from.file() - to.file());

        // If the move is neither straight nor diagonal, it's invalid for a Queen.
        if (!isRookMove && !isBishopMove) {
            return false;
        }

        // check if the path is clear.
        return isPathClear(from, to, board);
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}