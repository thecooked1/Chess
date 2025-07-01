package com.chess.model.pieces;

import com.chess.common.Colour;
import com.chess.model.Board.Board;
import com.chess.common.Square;

public class Pawn extends Piece {

    public Pawn(Colour colour) {
        super(colour);
        this.symbol = 'P';
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
            // Check that the square being jumped over is also empty
            Square jumpedSquare = new Square(from.rank() + direction, from.file());
            if (board.getPiece(jumpedSquare) == null) {
                return true;
            }
        }

        // --- Case 3: Standard diagonal capture ---
        if (Math.abs(fileDiff) == 1 && rankDiff == direction && targetPiece != null) {
            return targetPiece.getColor() != this.colour;
        }

        // --- Case 4: En Passant capture ---
        Square enPassantTarget = board.getEnPassantTargetSquare();
        if (enPassantTarget != null && to.equals(enPassantTarget)) {
            // Verify it's a diagonal move to the en passant square
            if (Math.abs(fileDiff) == 1 && rankDiff == direction) {
                // The target square must be empty for an en passant capture
                return targetPiece == null;
            }
        }

        // the move is invalid.
        return false;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}