package main.common;

public record Square(int rank, int file) { // rank = y (0-7), file = x (0-7)

    public boolean isValid() {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    /**
     * Converts the square's coordinates to algebraic notation (e.g., "e4").
     * @return The algebraic string.
     */
    @Override
    public String toString() {
        return toAlgebraic();
    }

    /**
     * Explicit method to get algebraic notation.
     * @return The algebraic string (e.g., "e4").
     */
    public String toAlgebraic() {
        char fileChar = (char) ('a' + file);
        int rankNum = 8 - rank; // Chess ranks are 1-8 from bottom up
        return "" + fileChar + rankNum;
    }

    /**
     * Gets the file (column) as its character representation.
     * @return 'a' through 'h'.
     */
    public char fileAsChar() {
        return (char) ('a' + this.file);
    }

    /**
     * Gets the rank (row) as its character representation.
     * @return '1' through '8'.
     */
    public char rankAsChar() {
        // rank 0 is '8', rank 7 is '1'
        return (char) ('0' + (8 - this.rank));
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