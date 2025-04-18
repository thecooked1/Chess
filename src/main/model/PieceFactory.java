package main.model;

import main.model.pieces.*;

public class PieceFactory {
    public static Piece createPiece(PieceType type, Color color) {
        switch (type) {
            case PAWN: return new Pawn(color);
            case ROOK: return new Rook(color);
            case KNIGHT: return new Knight(color);
            case BISHOP: return new Bishop(color);
            case QUEEN: return new Queen(color);
            case KING: return new King(color);
            default: throw new IllegalArgumentException("Unknown piece type: " + type);
        }
    }
}