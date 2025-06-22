package main.model.pieces;
import main.common.Colour;
import main.model.Board.Board;
import main.common.Square;


public abstract class Piece {
    protected Colour colour;
    protected char symbol;
    protected boolean hasMoved = false;

    public Piece(Colour colour) {
        this.colour = colour;
    }

    public Colour getColor() {
        return colour;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean hasMoved) {
         this.hasMoved = hasMoved;
    }



    public abstract boolean isValidMove(Square from, Square to, Board board);
    public abstract char getSymbol();

    @Override
    public String toString() {
        return colour.toString().charAt(0) + "" + getSymbol();
    }

    // Helper method
    protected boolean isPathClear(Square from, Square to, Board board) {
        int dr = Integer.compare(to.rank(), from.rank());
        int dc = Integer.compare(to.file(), from.file());
        int r = from.rank() + dr;
        int c = from.file() + dc;
        while (r != to.rank() || c != to.file()) {
            if (board.getPiece(new Square(r, c)) != null) {
                return false;
            }
            r += dr;
            c += dc;
        }
        return true;
    }
}