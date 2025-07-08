package com.chess.model.pieces;

import com.chess.common.Colour;
import com.chess.model.Board.Board;
import com.chess.common.Square;

public class Pawn extends Piece {

    public Pawn(Colour colour) {
        super(colour);
        this.symbol = (colour == Colour.WHITE) ? 'P' : 'p';
    }

    @Override
    public boolean isValidMove(Square from, Square to, Board board) {
        // Determine pawn's direction of movement based on its color.
        int direction = (this.colour == Colour.WHITE) ? -1 : 1;
        int startRank = (this.colour == Colour.WHITE) ? 6 : 1;

        Piece targetPiece = board.getPiece(to);
        int rankDiff = to.rank() - from.rank();
        int fileDiff = to.file() - from.file();

        // --- Case 1: Standard 1-square forward move ---
        if (fileDiff == 0 && rankDiff == direction && targetPiece == null) {
            return true;
        }

        // --- Case 2: Double-square forward move from starting rank ---
        if (from.rank() == startRank && fileDiff == 0 && rankDiff == 2 * direction && targetPiece == null) {
            return board.getPiece(new Square(from.rank() + direction, from.file())) == null;
        }

        // --- Case 3: Standard diagonal capture ---
        if (Math.abs(fileDiff) == 1 && rankDiff == direction && targetPiece != null && targetPiece.getColor() != this.colour) {
            return true;
        }

        // --- Case 4: En Passant capture ---
        if (Math.abs(fileDiff) == 1 && rankDiff == direction && to.equals(board.getEnPassantTargetSquare())) {
            return true;
        }

        return false;
    }

    @Override
    public Piece copy() {
        Pawn newPawn = new Pawn(this.getColor());
        newPawn.setMoved(this.hasMoved());
        return newPawn;
    }

    @Override
    public char getSymbol() {
        return this.symbol;
    }
}