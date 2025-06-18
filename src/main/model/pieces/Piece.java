package main.model.pieces;
import main.model.Board.Board;


public abstract class Piece {
    protected Colour colour;
    protected char symbol;

    public Piece(Colour colour) {
        this.colour = colour;
    }

    public Colour getColor() {
        return colour;
    }

    public abstract boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Board board);
    public abstract char getSymbol();

    @Override
    public String toString() {
        return colour.toString().charAt(0) + "" + getSymbol();
    }

    // Helper method
    protected boolean isPathClear(int r1, int c1, int r2, int c2, Board board) {
        int dr = Integer.compare(r2, r1);
        int dc = Integer.compare(c2, c1);

        int r = r1 + dr;
        int c = c1 + dc;

        while (r != r2 || c != c2) {
            if (!board.isValidPosition(r, c)) return false;
            if (board.getPiece(r, c) != null) {
                return false;
            }
            r += dr;
            c += dc;
        }
        return true;
    }
}