package com.chess.model.pieces;

import com.chess.common.Colour;
import com.chess.model.Board.Board;
import com.chess.common.Square;

public class Bishop extends Piece {

    public Bishop(Colour colour) {
        super(colour);
        this.symbol = 'B';
    }

    @Override
    public Piece copy() {
        Bishop newBishop = new Bishop(this.getColor());
        newBishop.setMoved(this.hasMoved());
        return newBishop;
    }

    @Override
    public boolean isValidMove(Square from, Square to, Board board) {
        // Bishops move diagonally
        if (Math.abs(from.rank() - to.rank()) != Math.abs(from.file() - to.file())) {
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