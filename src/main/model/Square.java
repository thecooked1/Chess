package main.model;

import java.util.Objects;

// Using record for immutability and conciseness
public record Square(int file, int rank) { // file = x (0-7), rank = y (0-7)

    // Convenience method for checking board bounds
    public boolean isValid() {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Square square = (Square) o;
        return file == square.file && rank == square.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, rank);
    }

    @Override
    public String toString() {
        // Convert to algebraic notation (e.g., a1, h8) for easier debugging
        char fileChar = (char) ('a' + file);
        int rankChar = 8 - rank; // Chess ranks are 1-8 from bottom up
        return "" + fileChar + rankChar;
    }

    // Static factory method for algebraic notation input
    public static Square fromAlgebraic(String alg) {
        if (alg == null || alg.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + alg);
        }
        char fileChar = Character.toLowerCase(alg.charAt(0));
        char rankChar = alg.charAt(1);

        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') {
            throw new IllegalArgumentException("Invalid algebraic notation: " + alg);
        }

        int file = fileChar - 'a';
        int rank = 8 - (rankChar - '0'); // Convert '1'-'8' to rank 7-0
        return new Square(file, rank);
    }
}