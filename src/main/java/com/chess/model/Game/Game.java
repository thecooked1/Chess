package com.chess.model.Game;

import com.chess.common.Colour;
import com.chess.model.Board.Board;
import com.chess.model.PGNParser.Interpreter;
import com.chess.model.PGNParser.Move;
import com.chess.model.PGNParser.Parser;
import java.util.List;


public class Game {
    private final Interpreter interpreter = new Interpreter();

    public boolean playGame(Parser.PGNGame game, int gameNumber) {
        Board board = new Board();
        List<String> moveTokens = game.getMoves();
        int moveNumber = 1;

        System.out.println("\n=== Playing Game #" + gameNumber + " ===");
        System.out.println("White: " + game.getHeaders().getOrDefault("White", "Unknown"));
        System.out.println("Black: " + game.getHeaders().getOrDefault("Black", "Unknown"));
        System.out.println("Result: " + game.getResult());
        System.out.println("---------------------------------");

        board.printBoard();

        try {
            for (String token : moveTokens) {
                // Determine whose turn it is BEFORE the move
                String player = board.getTurn().toString();
                String moveNumStr = (board.getTurn() == Colour.WHITE) ? moveNumber + "." : moveNumber + "...";
                System.out.println(moveNumStr + " " + player + " plays " + token);

                Move move;
                try {
                    move = interpreter.parseMove(token);
                } catch (IllegalArgumentException e) {
                    System.err.println("\nERROR in Game #" + gameNumber + " (" + player + " move):");
                    System.err.println("Failed to parse move token: '" + token + "'");
                    return false;
                }

                boolean moveApplied = board.applyMove(move);
                board.printBoard(); // Print board after each move

                if (!moveApplied) {
                    System.err.println("\nERROR: Board rejected the illegal move '" + token + "'");
                    return false;
                }

                // Increment move number after Black has moved.
                if (board.getTurn() == Colour.WHITE) {
                    moveNumber++;
                }
            }
        } catch (Exception e) {
            System.err.println("\nUNEXPECTED CRITICAL ERROR in Game #" + gameNumber);
            e.printStackTrace();
            return false;
        }

        System.out.println("\nGame #" + gameNumber + " finished successfully. Final Board:");
        board.printBoard();
        System.out.println("Expected Result: " + game.getResult() + '\n');
        return true;
    }

    public void playAllGames(Parser parser) {
        int gameIndex = 1;
        int successfulGames = 0;
        int failedGames = 0;

        if (parser.getGames().isEmpty()) {
            System.out.println("No games found in the PGN data.");
            return;
        }

        for (Parser.PGNGame game : parser.getGames()) {
            boolean gamePlayedSuccessfully = playGame(game, gameIndex);

            if (!gamePlayedSuccessfully) {
                System.err.println("--- Game #" + gameIndex + " aborted due to error. ---");
                failedGames++;
            } else {
                successfulGames++;
            }
            gameIndex++;
        }

        System.out.println("\n=========================================");
        System.out.println("=== Finished processing all games ===");
        System.out.println("Total Games Found: " + parser.getGames().size());
        System.out.println("Successfully Processed: " + successfulGames);
        System.out.println("Failed/Aborted: " + failedGames);
        System.out.println("=========================================");
    }
}