package com.chess.model.pieces;

import com.chess.common.Colour;
import com.chess.model.Board.Board;
import com.chess.common.Square;

public class Knight extends Piece {

    public Knight(Colour colour) {
        super(colour);
        this.symbol = 'N';
    }

    @Override
    public Piece copy() {
        Knight newKnight = new Knight(this.getColor());
        newKnight.setMoved(this.hasMoved());
        return newKnight;
    }

    @Override
    public boolean isValidMove(Square from, Square to, Board board) {
        int rankDiff = Math.abs(from.rank() - to.rank());
        int fileDiff = Math.abs(from.file() - to.file());

        // A knight's move is an L-shape: two squares in one cardinal direction,
        // then one square in a perpendicular direction.
        boolean isLMove = (rankDiff == 2 && fileDiff == 1) || (rankDiff == 1 && fileDiff == 2);

        return isLMove;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}