package main;

import main.model.Square;
import main.model.Board.Board;
import main.model.pieces.*;

public class FenUtility {

    public static String toFen(Board board) {
        // This method is fine, no changes needed here.
        StringBuilder fen = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int emptyCount = 0;
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPiece(new Square(r, c));
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    char symbol = piece.getSymbol();
                    fen.append(piece.getColor() == Colour.WHITE ? symbol : Character.toLowerCase(symbol));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (r < 7) {
                fen.append('/');
            }
        }
        
        fen.append(board.getTurn() == Colour.WHITE ? " w" : " b");
    
        // Add placeholders for castling, en passant, etc.
        fen.append(" - - 0 1");
    
        return fen.toString();

    }

    /**
     * Populates a given 2D Piece array based on a FEN string.
     * This method now correctly clears the grid first to prevent ghost pieces.
     *
     * @param fen The FEN string representing the board state.
     * @param grid The 2D Piece array to populate.
     */
    public static void FENtoBoard(String fen, Piece[][] grid) {
        // --- THE FIX: Clear the grid before populating it ---
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                grid[r][c] = null;
            }
        }
        // --- End of fix ---

        // Now, proceed with populating the fresh grid
        String[] ranks = fen.split(" ")[0].split("/");
        for (int r = 0; r < ranks.length; r++) {
            String rankStr = ranks[r];
            int c = 0;
            for (char ch : rankStr.toCharArray()) {
                if (c >= 8) break; // Safety break
                if (Character.isDigit(ch)) {
                    c += Character.getNumericValue(ch);
                } else {
                    Colour color = Character.isUpperCase(ch) ? Colour.WHITE : Colour.BLACK;
                    Piece piece = switch (Character.toLowerCase(ch)) {
                        case 'r' -> new Rook(color);
                        case 'n' -> new Knight(color);
                        case 'b' -> new Bishop(color);
                        case 'q' -> new Queen(color);
                        case 'k' -> new King(color);
                        case 'p' -> new Pawn(color);
                        default -> null;
                    };
                    grid[r][c] = piece;
                    c++;
                }
            }
        }
    }
}