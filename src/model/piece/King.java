package model.piece;

import java.util.ArrayList;
import java.util.List;
import model.*;

public class King extends Piece {
    private boolean hasMoved;

    public King(PieceColor color, Square initSq, String img_file) {
        super(color, PieceType.KING, initSq, img_file);
        this.hasMoved = false; // Assume starts unmoved, GameLogic updates
        // Determine initial hasMoved status based on starting position
        if (color == PieceColor.WHITE && initSq.getRank() == 0 && initSq.getFile() == 4) {
            this.hasMoved = false;
        } else if (color == PieceColor.BLACK && initSq.getRank() == 7 && initSq.getFile() == 4) {
            this.hasMoved = false;
        } else {
            this.hasMoved = true; // If not in standard starting pos, assume moved
        }
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean hasMoved() {
        return this.hasMoved;
    }

    @Override
    public List<Square> getPotentialMoves(Board board) {
        List<Square> moves = new ArrayList<>();
        Square[][] grid = board.getSquareGrid();
        int currentFile = getPosition().getFile();
        int currentRank = getPosition().getRank();

        // Check all 8 adjacent squares
        for (int df = -1; df <= 1; df++) {
            for (int dr = -1; dr <= 1; dr++) {
                if (df == 0 && dr == 0) continue; // Skip the current square

                int targetFile = currentFile + df;
                int targetRank = currentRank + dr;

                if (Board.isValidCoordinate(targetFile, targetRank)) {
                    Square targetSquare = grid[targetFile][targetRank];
                    if (!targetSquare.isOccupied() || targetSquare.getOccupyingPiece().getColor() != getColor()) {
                        // Basic move validity (doesn't check if move puts king in check - GameLogic does that)
                        moves.add(targetSquare);
                    }
                }
            }
        }

        // --- Potential Castling Moves ---
        // Note: GameLogic performs the full validation (king hasn't moved, rook hasn't moved,
        // squares are empty, king doesn't pass through check). This method only adds the
        // *target squares* if the basic path looks potentially clear locally.
        if (!hasMoved && board.getGameState() != null) { // Need game state for rights
            GameState gameState = board.getGameState();
            // Kingside
            if (gameState.canCastleKingside(getColor())) {
                if (!grid[currentFile + 1][currentRank].isOccupied() &&
                        !grid[currentFile + 2][currentRank].isOccupied()) {
                    // Basic check: squares between king and rook are empty
                    moves.add(grid[currentFile + 2][currentRank]);
                }
            }
            // Queenside
            if (gameState.canCastleQueenside(getColor())) {
                if (!grid[currentFile - 1][currentRank].isOccupied() &&
                        !grid[currentFile - 2][currentRank].isOccupied() &&
                        !grid[currentFile - 3][currentRank].isOccupied()) { // Also check square next to rook
                    // Basic check: squares between king and rook are empty
                    moves.add(grid[currentFile - 2][currentRank]);
                }
            }
        }


        return moves;
    }
}