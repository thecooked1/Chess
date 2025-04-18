package main.model.pieces;

import main.model.*;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    public Knight(Color color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public List<Position> getPseudoLegalMoves(Board board, Position currentPosition) {
        List<Position> moves = new ArrayList<>();
        int r = currentPosition.getRow();
        int c = currentPosition.getCol();

        int[] dr = {-2, -2, -1, -1, 1, 1, 2, 2}; // Row offsets
        int[] dc = {-1, 1, -2, 2, -2, 2, -1, 1}; // Column offsets

        for (int i = 0; i < 8; i++) {
            // Calculate potential target coordinates as integers first
            int targetRow = r + dr[i];
            int targetCol = c + dc[i];

            // Check if the calculated coordinates are within the board bounds
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
            // If the calculated targetRow/targetCol were out of bounds,
            // the code inside the 'if' is skipped, avoiding the Position exception.
        }
        return moves;
    }
}