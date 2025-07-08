// Create in a new package, e.g., com.chess.server.util
package com.chess.server.util;

import com.chess.model.PGNParser.Parser; // Using YOUR Parser class
import com.chess.server.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class GameImporter {

    /**
     * Imports all games from a given PGN file into the database.
     * @param filePath The path to the .pgn file on the server's filesystem.
     * @return The number of games successfully imported.
     */
    public int importFromFile(Path filePath) {
        int importedCount = 0;

        // 1. Create an instance of your parser.
        Parser pgnParser = new Parser();

        try {
            // 2. Use your parser's loadPGN(File) method.
            File pgnFile = filePath.toFile();
            pgnParser.loadPGN(pgnFile);

            // 3. Get the list of parsed games using your getGames() method.
            List<Parser.PGNGame> parsedGames = pgnParser.getGames();

            System.out.println("Found " + parsedGames.size() + " games in " + filePath.getFileName());

            // 4. Iterate through each parsed game and save it.
            for (Parser.PGNGame game : parsedGames) {
                // Get the headers map from your PGNGame object.
                Map<String, String> headers = game.getHeaders();

                // Extract required information, with defaults if a header is missing.
                String whitePlayer = headers.getOrDefault("White", "Unknown Player");
                String blackPlayer = headers.getOrDefault("Black", "Unknown Player");
                String result = headers.getOrDefault("Result", game.getResult()); // Use game result as fallback
                String date = headers.getOrDefault("Date", "????.??.??");

                // Reconstruct the full PGN text for storage.
                String fullPgnText = buildPgnString(game);

                // 5. Use your existing DatabaseManager to save the game.
                DatabaseManager.saveGame(whitePlayer, blackPlayer, result, fullPgnText, date);

                importedCount++;
            }

        } catch (IOException e) {
            System.err.println("Error reading or importing PGN file: " + e.getMessage());
            return 0;
        }

        System.out.println("Successfully imported " + importedCount + " games.");
        return importedCount;
    }

    /**
     * Helper method to reconstruct the PGN string from a parsed game object.
     * @param game The PGNGame object from your parser.
     * @return A formatted PGN string.
     */
    private String buildPgnString(Parser.PGNGame game) {
        StringBuilder sb = new StringBuilder();

        // Append headers
        game.getHeaders().forEach((key, value) -> {
            sb.append(String.format("[%s \"%s\"]\n", key, value));
        });
        sb.append("\n");

        // Append moves with move numbers
        List<String> moves = game.getMoves();
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) { // White's move
                sb.append((i / 2) + 1).append(". ");
            }
            sb.append(moves.get(i)).append(" ");
        }

        // Append the final result
        sb.append(game.getResult());

        return sb.toString();
    }
}