package main.model;

import java.util.Objects;

public class Position {
    private final int row; // 0-7
    private final int col; // 0-7

    public Position(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) {
            throw new IllegalArgumentException("Position out of bounds: (" + row + ", " + col + ")");
        }
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        // Convert to algebraic notation (e.g., a1, h8) for easier debugging
        char file = (char) ('a' + col);
        char rank = (char) ('8' - row);
        return "" + file + rank;
    }

    // Helper to convert algebraic notation to Position (useful for testing/debugging)
    public static Position fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }
        char fileChar = algebraic.charAt(0);
        char rankChar = algebraic.charAt(1);
        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }
        int col = fileChar - 'a';
        int row = '8' - rankChar;
        return new Position(row, col);
    }
}