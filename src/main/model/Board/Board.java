package main.model.Board;
import main.model.Position;
import main.model.pieces.*;
import main.model.PGNParser.Move;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Board {
    private final Piece[][] grid;
    private int[] enPassantTarget; // Stores {row, col} of the square *behind* the pawn that just moved two squares

    public Board() {
        grid = new Piece[8][8];
        setupInitialPosition();
        enPassantTarget = null;
    }

    public int[] getEnPassantTarget() {
        return enPassantTarget;
    }

    public boolean applyMove(Move move, boolean isWhiteTurn) {
        Colour currentColour = isWhiteTurn ? Colour.WHITE : Colour.BLACK;
        Colour enemyColour = isWhiteTurn ? Colour.BLACK : Colour.WHITE;

        // Handle castling
        if (move.isKingsideCastle() || move.isQueensideCastle()) {
            return handleCastling(move, currentColour);
        }

        int targetRow = 8 - move.getTargetRank();
        int targetCol = move.getTargetFile() - 'a';
        String pieceTypeSAN = move.getPiece();
        String dis = move.getDisambiguation();

        int foundStartRow = -1, foundStartCol = -1;
        Piece movingPiece = null;
        Piece capturedPieceOriginal = null; // Keep track of origin target square
        boolean isEnPassantCapture = false;

        // Find the piece to move
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null && piece.getColor() == currentColour &&
                        Character.toString(piece.getSymbol()).equalsIgnoreCase(pieceTypeSAN)) {

                    // Apply disambiguation rules
                    if (!dis.isEmpty()) {
                        if (dis.length() == 1) {
                            char d = dis.charAt(0);
                            if (Character.isDigit(d)) { // Disambiguation by rank
                                if ((8 - Character.getNumericValue(d)) != row) continue;
                            } else { // Disambiguation by file
                                if ((d - 'a') != col) continue;
                            }
                        } else if (dis.length() == 2) { // Disambiguation by full coordinate
                            int dCol = dis.charAt(0) - 'a';
                            int dRow = 8 - Character.getNumericValue(dis.charAt(1));
                            if (dRow != row || dCol != col) continue;
                        }
                    }

                    // Check if this piece could make the move
                    if (piece.isValidMove(row, col, targetRow, targetCol, this)) {

                        capturedPieceOriginal = grid[targetRow][targetCol]; // Store potential capture
                        grid[targetRow][targetCol] = piece;
                        grid[row][col] = null;

                        // Handle Potential En Passant capture
                        Piece capturedPawn = null;
                        int capturedPawnRow = -1;
                        if (piece instanceof Pawn && targetCol != col && capturedPieceOriginal == null &&
                                enPassantTarget != null && enPassantTarget[0] == targetRow && enPassantTarget[1] == targetCol)
                        {
                            // This is the acutal en passant capture.
                            capturedPawnRow = currentColour == Colour.WHITE ? targetRow + 1 : targetRow - 1;
                            capturedPawn = grid[capturedPawnRow][targetCol];
                            grid[capturedPawnRow][targetCol] = null; // Remove the captured pawn
                            isEnPassantCapture = true;
                        }

                        // Check if the move leaves the king in check
                        boolean legal = !isInCheck(currentColour);

                        // If not legal, undo the move and continue searching for another piece
                        if (!legal) {
                            grid[row][col] = piece; // Restore moving piece
                            grid[targetRow][targetCol] = capturedPieceOriginal; // Restore whatever was on target square
                            if (isEnPassantCapture) {
                                grid[capturedPawnRow][targetCol] = capturedPawn; // Restore en passant captured pawn
                            }
                            isEnPassantCapture = false;
                            continue;
                        }

                        foundStartRow = row;
                        foundStartCol = col;
                        movingPiece = piece;
                        break;
                    }
                }
            }
            if (movingPiece != null) break;
        }

        if (movingPiece == null) {
            System.err.println("Illegal move: No piece found that can legally perform " + move.getRaw() + " for " + currentColour);
            return false;
        }

        // Update Castling Rights
        if (movingPiece instanceof King) ((King) movingPiece).setMoved(true);
        if (movingPiece instanceof Rook) ((Rook) movingPiece).setMoved(true);

        // Handle Promotion
        if (movingPiece instanceof Pawn && (targetRow == 0 || targetRow == 7) && move.getPromotion() != null) {
            String promo = move.getPromotion().toUpperCase();
            Piece promotedPiece;
            switch (promo) {
                case "Q": promotedPiece = new Queen(currentColour); break;
                case "R": promotedPiece = new Rook(currentColour); break;
                case "B": promotedPiece = new Bishop(currentColour); break;
                case "N": promotedPiece = new Knight(currentColour); break;
                default: throw new IllegalArgumentException("Invalid promotion piece: " + promo);
            }
            grid[targetRow][targetCol] = promotedPiece;
        }

        if (movingPiece instanceof Pawn && Math.abs(targetRow - foundStartRow) == 2) {
            enPassantTarget = new int[]{(foundStartRow + targetRow) / 2, foundStartCol};
        } else {
            enPassantTarget = null;
        }

        // Check for Check/Checkmate
        if (isInCheck(enemyColour)) {
            move.setCheck(true);
            if (!hasAnyLegalMoves(enemyColour)) {
                move.setCheckmate(true);
            }
        } else {
            // Check for Stalemate
            if (!hasAnyLegalMoves(enemyColour)) {
                System.out.println("Stalemate detected!");
                // move.setStalemate(true);
            }
        }
        return true;
    }

    private boolean handleCastling(Move move, Colour color) {
        int row = (color == Colour.WHITE) ? 7 : 0;

        // Find King and Rook
        int kingCol = 4;
        Piece kingPiece = grid[row][kingCol];
        if (!(kingPiece instanceof King) || kingPiece.getColor() != color || ((King) kingPiece).hasMoved()) {
            System.err.println("Castling failed: King issue.");
            return false;
        }
        King king = (King) kingPiece;

        int rookCol = move.isKingsideCastle() ? 7 : 0;
        int rookTargetCol = move.isKingsideCastle() ? 5 : 3;
        int kingTargetCol = move.isKingsideCastle() ? 6 : 2;

        Piece rookPiece = grid[row][rookCol];
        if (!(rookPiece instanceof Rook) || rookPiece.getColor() != color || ((Rook) rookPiece).hasMoved()) {
            System.err.println("Castling failed: Rook issue.");
            return false;
        }
        Rook rook = (Rook) rookPiece;

        // Check if king is currently in check
        if (isInCheck(color)) {
            System.err.println("Castling failed: King in check.");
            return false;
        }

        // Check if path is clear between King and Rook
        int startCol = Math.min(kingCol, rookCol) + 1;
        int endCol = Math.max(kingCol, rookCol);
        for (int c = startCol; c < endCol; c++) {
            if (grid[row][c] != null) {
                System.err.println("Castling failed: Path blocked at " + row + "," + c);
                return false;
            }
        }

        // Check if king passes through check
        int step = move.isKingsideCastle() ? 1 : -1;
        for (int c = kingCol + step; c != kingTargetCol + step; c += step) {
            // Temporarily move king to check square safety
            Piece temp = grid[row][c];
            grid[row][c] = king;
            grid[row][kingCol] = null;
            boolean passesThroughCheck = isInCheck(color);
            // Undo temporary move
            grid[row][kingCol] = king;
            grid[row][c] = temp;

            if (passesThroughCheck) {
                System.err.println("Castling failed: King passes through check at " + row + "," + c);
                return false;
            }
        }

        // Castling
        grid[row][kingTargetCol] = king;
        grid[row][kingCol] = null;
        grid[row][rookTargetCol] = rook;
        grid[row][rookCol] = null;

        king.setMoved(true);
        rook.setMoved(true);

        // Clear en passant target after castling
        enPassantTarget = null;

        // Check opponent after castling
        Colour enemyColour = (color == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
        if (isInCheck(enemyColour)) {
            move.setCheck(true);
            if (!hasAnyLegalMoves(enemyColour)) {
                move.setCheckmate(true);
            }
        }
        return true;
    }

    public boolean isInCheck(Colour color) {
        int kingRow = -1, kingCol = -1;
        // Find the king
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p instanceof King && p.getColor() == color) {
                    kingRow = r;
                    kingCol = c;
                    break;
                }
            }
            if (kingRow != -1) break;
        }

        if (kingRow == -1) {
            throw new IllegalStateException("King of color " + color + " not found on the board!");
        }

        // Check if any enemy piece can attack the king's square
        Colour enemyColor = (color == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece attacker = grid[r][c];
                if (attacker != null && attacker.getColor() == enemyColor) {
                    if (attacker.isValidMove(r, c, kingRow, kingCol, this)) {
                        return true; // King is under attack
                    }
                }
            }
        }
        return false; // King is not in check
    }

    public boolean hasAnyLegalMoves(Colour color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null && piece.getColor() == color) {
                    for (int targetRow = 0; targetRow < 8; targetRow++) {
                        for (int targetCol = 0; targetCol < 8; targetCol++) {
                            if (piece.isValidMove(row, col, targetRow, targetCol, this)) {
                                // Simulate the move
                                Piece backupTarget = grid[targetRow][targetCol];
                                Piece backupEnPassantCapturedPawn = null;
                                int enPassantPawnRow = -1;
                                boolean wasEnPassant = false;
                                int[] backupEnPassantTarget = enPassantTarget == null ? null : enPassantTarget.clone(); // Need to backup EP target too

                                grid[targetRow][targetCol] = piece;
                                grid[row][col] = null;

                                if (piece instanceof Pawn && targetCol != col && backupTarget == null &&
                                        enPassantTarget != null && enPassantTarget[0] == targetRow && enPassantTarget[1] == targetCol)
                                {
                                    enPassantPawnRow = color == Colour.WHITE ? targetRow + 1 : targetRow - 1;
                                    backupEnPassantCapturedPawn = grid[enPassantPawnRow][targetCol];
                                    grid[enPassantPawnRow][targetCol] = null;
                                    wasEnPassant = true;
                                }
                                boolean stillInCheck = isInCheck(color);

                                // Undo the move
                                grid[row][col] = piece;
                                grid[targetRow][targetCol] = backupTarget;
                                if(wasEnPassant) {
                                    grid[enPassantPawnRow][targetCol] = backupEnPassantCapturedPawn;
                                }
                                // Restore en passant target state for next iteration
                                this.enPassantTarget = backupEnPassantTarget;

                                if (!stillInCheck) {
                                    return true; // Found at least one legal move
                                }
                            }
                        }
                    }
                }
            }
        }
        return false; // No legal moves found for this color
    }

    public void setupInitialPosition() {
        for(int r=0; r<8; r++) {
            for (int c = 0; c < 8; c++) {
                grid[r][c] = null;
            }
        }
        enPassantTarget = null; // Reset en passant

        // Setup White pieces
        grid[7][0] = new Rook(Colour.WHITE);
        grid[7][1] = new Knight(Colour.WHITE);
        grid[7][2] = new Bishop(Colour.WHITE);
        grid[7][3] = new Queen(Colour.WHITE);
        grid[7][4] = new King(Colour.WHITE);
        grid[7][5] = new Bishop(Colour.WHITE);
        grid[7][6] = new Knight(Colour.WHITE);
        grid[7][7] = new Rook(Colour.WHITE);
        for (int i = 0; i < 8; i++) grid[6][i] = new Pawn(Colour.WHITE); // Rank 2 = Row 6

        // Setup Black pieces
        grid[0][0] = new Rook(Colour.BLACK);
        grid[0][1] = new Knight(Colour.BLACK);
        grid[0][2] = new Bishop(Colour.BLACK);
        grid[0][3] = new Queen(Colour.BLACK);
        grid[0][4] = new King(Colour.BLACK);
        grid[0][5] = new Bishop(Colour.BLACK);
        grid[0][6] = new Knight(Colour.BLACK);
        grid[0][7] = new Rook(Colour.BLACK);
        for (int i = 0; i < 8; i++) grid[1][i] = new Pawn(Colour.BLACK); // Rank 7 = Row 1
    }

    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public Piece getPiece(int row, int col) {
        if (!isValidPosition(row, col)) return null; // Or throw exception
        return grid[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        if (isValidPosition(row, col)) {
            grid[row][col] = piece;
        }
    }

    //Helper for debugging
    public void printBoard() {
        System.out.println("  a b c d e f g h");
        System.out.println(" +-----------------+");
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + "|");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                System.out.print((p == null ? "." : p.getSymbol()) + " ");
            }
            System.out.println("|" + (8 - r));
        }
        System.out.println(" +-----------------+");
        System.out.println("  a b c d e f g h");
        if (enPassantTarget != null) {
            char file = (char)('a' + enPassantTarget[1]);
            int rank = 8 - enPassantTarget[0];
            /*System.out.println("En Passant Target: " + file + rank + " (Board coords: " + enPassantTarget[0]+","+enPassantTarget[1]+")");
        } else {
            System.out.println("En Passant Target: None");*/
        }
    }
    /**
     * Generates a list of all legal destination positions for a piece at a given start position.
     * This is what the GUI controller needs to highlight squares.
     */
    public List<Position> getLegalMovesForPiece(Position startPos) {
        List<Position> legalMoves = new ArrayList<>();
        Piece piece = getPiece(startPos.getRow(), startPos.getCol());
        if (piece == null) {
            return legalMoves; // No piece, no moves
        }

        Colour playerColor = piece.getColor();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position targetPos = new Position(r, c);
                // Check if the move is pseudo-legal
                if (piece.isValidMove(startPos.getRow(), startPos.getCol(), targetPos.getRow(), targetPos.getCol(), this)) {
                    // Simulate the move to see if it leaves the king in check
                    Piece backupTarget = grid[targetPos.getRow()][targetPos.getCol()];
                    grid[targetPos.getRow()][targetPos.getCol()] = piece;
                    grid[startPos.getRow()][startPos.getCol()] = null;

                    boolean leavesKingInCheck = isInCheck(playerColor);

                    // Undo the move
                    grid[startPos.getRow()][startPos.getCol()] = piece;
                    grid[targetPos.getRow()][targetPos.getCol()] = backupTarget;

                    if (!leavesKingInCheck) {
                        legalMoves.add(targetPos);
                    }
                }
            }
        }
        // TODO: Add logic for castling and en passant to this list
        return legalMoves;
    }

    /**
     * Applies a move given a start and end position. This is for the GUI controller.
     * Returns true if the move was successful.
     */
    public boolean applyMove(Position start, Position end, Optional<String> promotionPiece) {
        Piece movingPiece = getPiece(start.getRow(), start.getCol());
        if (movingPiece == null) return false;

        Colour currentColour = movingPiece.getColor();

        // Check if this move is in the list of legal moves. This is the ultimate validation.
        boolean isLegal = getLegalMovesForPiece(start).contains(end);
        if (!isLegal) {
            // TODO: Also need to check for castling moves here, as they are special
            System.err.println("GUI move from " + start + " to " + end + " is not legal.");
            return false;
        }

        // --- Execute the move ---
        // Handle en passant capture (which is a special case)
        if (movingPiece instanceof Pawn && end.getCol() != start.getCol() && getPiece(end.getRow(), end.getCol()) == null) {
            int capturedPawnRow = currentColour == Colour.WHITE ? end.getRow() + 1 : end.getRow() - 1;
            grid[capturedPawnRow][end.getCol()] = null;
        }

        // Move the piece
        grid[end.getRow()][end.getCol()] = movingPiece;
        grid[start.getRow()][start.getCol()] = null;

        // Handle promotion
        if (movingPiece instanceof Pawn && (end.getRow() == 0 || end.getRow() == 7)) {
            String promo = promotionPiece.orElse("Q").toUpperCase(); // Default to Queen
            Piece promoted;
            switch (promo) {
                case "R": promoted = new Rook(currentColour); break;
                case "B": promoted = new Bishop(currentColour); break;
                case "N": promoted = new Knight(currentColour); break;
                default:  promoted = new Queen(currentColour); break;
            }
            grid[end.getRow()][end.getCol()] = promoted;
        }

        // Update piece's internal state (for castling rights)
        if (movingPiece instanceof King || movingPiece instanceof Rook) {
            movingPiece.setMoved(true);
        }

        // Update en passant target for the *next* turn
        if (movingPiece instanceof Pawn && Math.abs(start.getRow() - end.getRow()) == 2) {
            enPassantTarget = new int[]{(start.getRow() + end.getRow()) / 2, start.getCol()};
        } else {
            enPassantTarget = null;
        }

        return true;
    }

}