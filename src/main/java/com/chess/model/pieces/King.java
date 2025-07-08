package com.chess.model.pieces;

import com.chess.common.Colour;
import com.chess.model.Board.Board;
import com.chess.common.Square;

public class King extends Piece {

    public King(Colour colour) {
        super(colour);
        this.symbol = 'K';
    }

    @Override
    public Piece copy() {
        King newKing = new King(this.getColor());
        newKing.setMoved(this.hasMoved());
        return newKing;
    }

    @Override
    public boolean isValidMove(Square from, Square to, Board board) {
        int rankDiff = Math.abs(from.rank() - to.rank());
        int fileDiff = Math.abs(from.file() - to.file());

        // Standard 1-square move
        if (rankDiff <= 1 && fileDiff <= 1) {
            // Ensure it's not the same square
            return !from.equals(to);
        }

        // Castling move attempt (King moves two squares horizontally)
        if (rankDiff == 0 && fileDiff == 2) {
            return true;
        }

        return false;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}