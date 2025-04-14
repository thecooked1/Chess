package model;

import model.piece.*; // Import specific pieces

import java.util.ArrayList;
import java.util.List;

public class BoardModel {
    private final Piece[][] board; // 8x8 grid, board[row][col]

    public BoardModel() {
        this.board = new Piece[8][8];
    }

    // Copy constructor for simulation
    public BoardModel(BoardModel other) {
        this.board = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                this.board[r][c] = other.board[r][c]; // Shallow copy of pieces is okay here
            }
        }
    }

    public void setupInitialPieces() {
        // Clear board first
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = null;
            }
        }

        // Black pieces (Row 0 and 1)
        placePiece(new Rook(PlayerColor.BLACK), new Position(0, 0));
        placePiece(new Knight(PlayerColor.BLACK), new Position(0, 1));
        placePiece(new Bishop(PlayerColor.BLACK), new Position(0, 2));
        placePiece(new Queen(PlayerColor.BLACK), new Position(0, 3));
        placePiece(new King(PlayerColor.BLACK), new Position(0, 4));
        placePiece(new Bishop(PlayerColor.BLACK), new Position(0, 5));
        placePiece(new Knight(PlayerColor.BLACK), new Position(0, 6));
        placePiece(new Rook(PlayerColor.BLACK), new Position(0, 7));
        for (int c = 0; c < 8; c++) {
            placePiece(new Pawn(PlayerColor.BLACK), new Position(1, c));
        }

        // White pieces (Row 6 and 7)
        for (int c = 0; c < 8; c++) {
            placePiece(new Pawn(PlayerColor.WHITE), new Position(6, c));
        }
        placePiece(new Rook(PlayerColor.WHITE), new Position(7, 0));
        placePiece(new Knight(PlayerColor.WHITE), new Position(7, 1));
        placePiece(new Bishop(PlayerColor.WHITE), new Position(7, 2));
        placePiece(new Queen(PlayerColor.WHITE), new Position(7, 3));
        placePiece(new King(PlayerColor.WHITE), new Position(7, 4));
        placePiece(new Bishop(PlayerColor.WHITE), new Position(7, 5));
        placePiece(new Knight(PlayerColor.WHITE), new Position(7, 6));
        placePiece(new Rook(PlayerColor.WHITE), new Position(7, 7));
    }

    public Piece getPieceAt(Position pos) {
        if (!Position.isValid(pos.getRow(), pos.getCol())) return null;
        return board[pos.getRow()][pos.getCol()];
    }

    public void setPieceAt(Position pos, Piece piece) {
        if (!Position.isValid(pos.getRow(), pos.getCol())) return;
        board[pos.getRow()][pos.getCol()] = piece;
    }

    public void placePiece(Piece piece, Position pos) {
        setPieceAt(pos, piece);
    }

    public Piece removePieceAt(Position pos) {
        Piece removed = getPieceAt(pos);
        setPieceAt(pos, null);
        return removed;
    }

    public boolean isEmpty(Position pos) {
        return getPieceAt(pos) == null;
    }

    public boolean isOccupiedByOpponent(Position pos, PlayerColor friendlyColor) {
        Piece piece = getPieceAt(pos);
        return piece != null && piece.getColor() != friendlyColor;
    }

    public List<Piece> getAllPieces() {
        List<Piece> pieces = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] != null) {
                    pieces.add(board[r][c]);
                }
            }
        }
        return pieces;
    }

    public List<Piece> getPieces(PlayerColor color) {
        List<Piece> pieces = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] != null && board[r][c].getColor() == color) {
                    pieces.add(board[r][c]);
                }
            }
        }
        return pieces;
    }

    public Position findKing(PlayerColor kingColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == kingColor) {
                    return new Position(r, c);
                }
            }
        }
        return null; // Should not happen in a valid game state
    }

    // Apply a move to this board state (used for simulation and actual moves)
    public Piece applyMove(Move move) {
        Piece movedPiece = removePieceAt(move.getStart());
        if (movedPiece == null) {
            // This should ideally not happen if move generation is correct
            System.err.println("Attempted to move a null piece from " + move.getStart());
            return null;
        }
        Piece capturedPiece = removePieceAt(move.getEnd()); // Remove whatever is at the destination
        placePiece(movedPiece, move.getEnd());             // Place the moved piece

        // Handle piece-specific state changes (like hasMoved)
        if (movedPiece instanceof King) ((King) movedPiece).setHasMoved(true);
        if (movedPiece instanceof Rook) ((Rook) movedPiece).setHasMoved(true);
        if (movedPiece instanceof Pawn) ((Pawn) movedPiece).setHasMoved(true);
        // Add logic for en passant capture, castling rook move if implemented

        move.setPieceCaptured(capturedPiece); // Record captured piece in the move object
        return capturedPiece;
    }

    // Get a direct reference to the internal board array (use with caution, primarily for View rendering)
    public Piece[][] getBoardArray() {
        return board;
    }
}