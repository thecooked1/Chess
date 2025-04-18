package main.model;

import main.model.pieces.*; // Import all piece types

public class Board {
    private final Piece[][] squares; // 8x8 array, squares[row][col]

    public Board() {
        squares = new Piece[8][8];
    }

    // Copy constructor for immutability in game logic checks
    public Board(Board other) {
        this.squares = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                // Piece objects are immutable enough for shallow copy here,
                // but we need new instances if setHasMoved is used often during checks.
                // Let's assume basic piece info is immutable for now.
                if (other.squares[r][c] != null) {
                    // Create a new instance to avoid shared 'hasMoved' state issues during simulation
                    this.squares[r][c] = PieceFactory.createPiece(other.squares[r][c].getType(), other.squares[r][c].getColor());
                    this.squares[r][c].setHasMoved(other.squares[r][c].hasMoved());
                } else {
                    this.squares[r][c] = null;
                }
            }
        }
    }


    public void initializeStandardSetup() {
        // Clear board first
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c] = null;
            }
        }

        // Black pieces (Row 0 and 1)
        squares[0][0] = PieceFactory.createPiece(PieceType.ROOK, Color.BLACK);
        squares[0][1] = PieceFactory.createPiece(PieceType.KNIGHT, Color.BLACK);
        squares[0][2] = PieceFactory.createPiece(PieceType.BISHOP, Color.BLACK);
        squares[0][3] = PieceFactory.createPiece(PieceType.QUEEN, Color.BLACK);
        squares[0][4] = PieceFactory.createPiece(PieceType.KING, Color.BLACK);
        squares[0][5] = PieceFactory.createPiece(PieceType.BISHOP, Color.BLACK);
        squares[0][6] = PieceFactory.createPiece(PieceType.KNIGHT, Color.BLACK);
        squares[0][7] = PieceFactory.createPiece(PieceType.ROOK, Color.BLACK);
        for (int c = 0; c < 8; c++) {
            squares[1][c] = PieceFactory.createPiece(PieceType.PAWN, Color.BLACK);
        }

        // White pieces (Row 6 and 7)
        for (int c = 0; c < 8; c++) {
            squares[6][c] = PieceFactory.createPiece(PieceType.PAWN, Color.WHITE);
        }
        squares[7][0] = PieceFactory.createPiece(PieceType.ROOK, Color.WHITE);
        squares[7][1] = PieceFactory.createPiece(PieceType.KNIGHT, Color.WHITE);
        squares[7][2] = PieceFactory.createPiece(PieceType.BISHOP, Color.WHITE);
        squares[7][3] = PieceFactory.createPiece(PieceType.QUEEN, Color.WHITE);
        squares[7][4] = PieceFactory.createPiece(PieceType.KING, Color.WHITE);
        squares[7][5] = PieceFactory.createPiece(PieceType.BISHOP, Color.WHITE);
        squares[7][6] = PieceFactory.createPiece(PieceType.KNIGHT, Color.WHITE);
        squares[7][7] = PieceFactory.createPiece(PieceType.ROOK, Color.WHITE);
    }

    public Piece getPieceAt(Position pos) {
        if (!isValidPosition(pos)) return null;
        return squares[pos.getRow()][pos.getCol()];
    }

    public Piece getPieceAt(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return null;
        return squares[row][col];
    }

    public void setPieceAt(Position pos, Piece piece) {
        if (!isValidPosition(pos)) return;
        squares[pos.getRow()][pos.getCol()] = piece;
        if (piece != null) {
            // Ensure piece knows it moved if this isn't its initial placement
            // Handled more robustly in GameLogic during move execution
        }
    }

    public void movePiece(Position from, Position to) {
        if (!isValidPosition(from) || !isValidPosition(to)) return;
        Piece piece = getPieceAt(from);
        if (piece != null) {
            piece.setHasMoved(true); // Mark piece as moved
            setPieceAt(to, piece);
            setPieceAt(from, null);
        }
    }

    public boolean isValidPosition(Position pos) {
        return pos != null && pos.getRow() >= 0 && pos.getRow() < 8 && pos.getCol() >= 0 && pos.getCol() < 8;
    }

    // Find the king of a specific color
    public Position findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    return new Position(r, c);
                }
            }
        }
        return null; // Should not happen in a valid game state
    }
}