package main.model.pieces;

import main.model.*;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {

    public King(Color color) {
        super(color, PieceType.KING);
    }

    @Override
    public List<Position> getPseudoLegalMoves(Board board, Position currentPosition) {
        List<Position> moves = new ArrayList<>();
        int r = currentPosition.getRow();
        int c = currentPosition.getCol();

        // Iterate through all 8 adjacent squares and the current square
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                // Skip the square the king is currently on
                if (dr == 0 && dc == 0) continue;

                // Calculate potential target coordinates as integers first
                int targetRow = r + dr;
                int targetCol = c + dc;

                // --- Check if the calculated coordinates are within the board bounds ---
                if (targetRow >= 0 && targetRow < 8 && targetCol >= 0 && targetCol < 8) {
                    // --- Only create Position object if coordinates are valid ---
                    Position targetPos = new Position(targetRow, targetCol);

                    // Check the square occupation status
                    Piece targetPiece = board.getPieceAt(targetPos);
                    if (targetPiece == null || targetPiece.getColor() != this.color) {
                        // If square is empty or occupied by opponent, it's a pseudo-legal move
                        moves.add(targetPos);
                    }
                }
                // If coordinates were out of bounds, the inner 'if' block is skipped.
            }
        }
        // TODO: Add Castling logic here (requires GameState knowledge - handled separately in GameState/GameLogic)
        // Note: Castling moves are added later during full legal move generation in GameState,
        // as they require checking game state conditions (rights, intermediate squares attacked).
        // This method correctly only calculates the basic adjacent moves.
        return moves;
    }
}