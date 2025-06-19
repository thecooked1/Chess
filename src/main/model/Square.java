package main.model;

import java.util.Objects;

public record Square(int rank, int file) { // rank = y (0-7), file = x (0-7)

    public boolean isValid() {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    @Override
    public String toString() {
        char fileChar = (char) ('a' + file);
        int rankChar = 8 - rank; // Chess ranks are 1-8 from bottom up
        return "" + fileChar + rankChar;
    }

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
        return new Square(rank, file);
    }
}