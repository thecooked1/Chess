package com.chess;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class PGNGenerator {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PGNGenerator() {}

    /**
     * Generates a complete PGN string for a finished game.
     *
     * @param whitePlayerName The name of the white player.
     * @param blackPlayerName The name of the black player.
     * @param result          The result of the game ("1-0", "0-1", "1/2-1/2", or "*").
     * @param moveHistory     A list of all moves made in Standard Algebraic Notation (SAN).
     * @return A fully formatted PGN string.
     */
    public static String generate(String whitePlayerName, String blackPlayerName, String result, List<String> moveHistory) {
        StringBuilder pgn = new StringBuilder();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // 1. Add PGN headers (the "Seven Tag Roster")
        pgn.append(String.format("[Event \"Online Chess Match\"]%n"));
        pgn.append(String.format("[Site \"Java Chess Server\"]%n"));
        pgn.append(String.format("[Date \"%s\"]%n", today.format(formatter)));
        pgn.append(String.format("[Round \"1\"]%n"));
        pgn.append(String.format("[White \"%s\"]%n", whitePlayerName));
        pgn.append(String.format("[Black \"%s\"]%n", blackPlayerName));
        pgn.append(String.format("[Result \"%s\"]%n%n", result));

        // 2. Add the move text
        int moveNumber = 1;
        for (int i = 0; i < moveHistory.size(); i++) {
            if (i % 2 == 0) {
                pgn.append(moveNumber).append(". ");
                moveNumber++;
            }
            pgn.append(moveHistory.get(i)).append(" ");
        }

        // 3. Add the final result at the end
        pgn.append(result);

        return pgn.toString();
    }
}

