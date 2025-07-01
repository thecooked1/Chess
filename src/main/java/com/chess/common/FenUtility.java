package com.chess.common;

import com.chess.model.Board.Board;
import com.chess.model.pieces.Piece;

/**
 * A utility class for converting a Board object to and from
 * Forsyth-Edwards Notation (FEN).
 */
public final class FenUtility {

    private FenUtility() {} // Private constructor for utility class

    public static String toFen(Board board) {
        StringBuilder fen = new StringBuilder();

        // 1. Piece Placement
        for (int rank = 0; rank < 8; rank++) {
            int emptySquares = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = board.getPiece(new Square(rank, file));
                if (piece == null) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    char symbol = piece.getSymbol();
                    fen.append(piece.getColor() == Colour.BLACK ? Character.toLowerCase(symbol) : symbol);
                }
            }
            if (emptySquares > 0) {
                fen.append(emptySquares);
            }
            if (rank < 7) {
                fen.append('/');
            }
        }

        // 2. Active Color
        fen.append(' ');
        fen.append(board.getTurn() == Colour.WHITE ? 'w' : 'b');

        // 3. Castling Availability (This is a simplified version)
        // A full implementation would check board.getCastlingRights()
        fen.append(" KQkq"); // Placeholder, assuming all rights for now

        // 4. En Passant Target Square
        // --- THIS IS THE CRITICAL FIX ---
        fen.append(' ');
        Square epSquare = board.getEnPassantTargetSquare();
        if (epSquare != null) {
            fen.append(epSquare.toAlgebraic());
        } else {
            fen.append('-');
        }

        // 5. Halfmove Clock & 6. Fullmove Number (not implemented, using placeholders)
        fen.append(" 0 1");

        return fen.toString();
    }
}