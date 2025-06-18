package main.model.Game;
import main.model.Board.Board;
import main.model.PGNParser.Interpreter;
import main.model.PGNParser.Move;
import main.model.PGNParser.Parser;
import java.util.List;
import java.util.Map;

public class Game {
    private final Interpreter interpreter = new Interpreter();

    public boolean playGame(Parser.PGNGame game, int gameNumber) {
        Board board = new Board(); // Reset board
        boolean whiteToMove = true;
        List<String> moveTokens = game.getMoves();
        int moveNumber = 1;
        int halfMoveClock = 0;

        System.out.println("=== Playing Game #" + gameNumber + " ===");
        System.out.println("White: " + game.getHeaders().getOrDefault("White", "Unknown"));
        System.out.println("Black: " + game.getHeaders().getOrDefault("Black", "Unknown"));
        System.out.println("Event: " + game.getHeaders().getOrDefault("Event", "Unknown"));

        board.printBoard();

        try {
            for (String token : moveTokens) {
                Move move = null;
                // Parsing phase
                try {
                    move = interpreter.parseMove(token);
                } catch (IllegalArgumentException e) {
                    System.err.println("\nERROR in Game #" + gameNumber + " (Move " + moveNumber + " " + (whiteToMove ? "White" : "Black") + "):");
                    System.err.println("Failed to parse move token: '" + token + "'");
                    System.err.println("Reason: " + e.getMessage());
                    return false;
                }

                // Game Replay Phase
                String player = whiteToMove ? "White" : "Black";
                String moveNumStr = whiteToMove ? moveNumber + "." : moveNumber + "...";
                System.out.println(moveNumStr + " " + player + ": " + token);
                boolean moveApplied = board.applyMove(move, whiteToMove);
                // board.printBoard();

                if (!moveApplied) {
                    System.err.println("\nERROR in Game #" + gameNumber + " (Move " + moveNumber + " " + player + "):");
                    System.err.println("Illegal move detected for token: '" + token + "'");
                    // board.printBoard();
                    return false;
                }


                whiteToMove = !whiteToMove;
                if (whiteToMove) {
                    moveNumber++;
                }
                halfMoveClock++;
            }

        } catch (Exception e) {

            System.err.println("\nUNEXPECTED ERROR in Game #" + gameNumber + " around move: " + moveNumber + " (" + (whiteToMove ? "White" : "Black") + ")");
            System.err.println("Move token being processed: '" + (moveTokens.size() > halfMoveClock ? moveTokens.get(halfMoveClock) : "N/A") + "'");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        board.printBoard();

        System.out.println("Game #" + gameNumber + " finished successfully. Result: " + game.getResult() + '\n');
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
                System.err.println("--- Game #" + gameIndex + " aborted due to error. Skipping to next game. ---");
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