package model.piece;

import java.util.ArrayList;
import java.util.List;
import model.*;

public class Knight extends Piece {

    public Knight(PieceColor color, Square initSq, String img_file) {
        super(color, PieceType.KNIGHT, initSq, img_file);
    }

    @Override
    public List<Square> getPotentialMoves(Board board) {
        List<Square> moves = new ArrayList<>();
        Square[][] grid = board.getSquareGrid();
        int currentFile = getPosition().getFile();
        int currentRank = getPosition().getRank();

        // Possible L-shaped moves (delta file, delta rank)
        int[] fileOffsets = { 1, 1, 2, 2, -1, -1, -2, -2 };
        int[] rankOffsets = { 2, -2, 1, -1, 2, -2, 1, -1 };

        for (int i = 0; i < 8; i++) {
            int targetFile = currentFile + fileOffsets[i];
            int targetRank = currentRank + rankOffsets[i];

            if (Board.isValidCoordinate(targetFile, targetRank)) {
                Square targetSquare = grid[targetFile][targetRank];
                if (!targetSquare.isOccupied() || targetSquare.getOccupyingPiece().getColor() != getColor()) {
                    // Can move to empty square or capture opponent's piece
                    moves.add(targetSquare);
                }
            }
        }
        return moves;
    }
}