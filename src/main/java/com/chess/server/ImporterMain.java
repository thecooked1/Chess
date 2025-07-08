// In com.chess.server (or wherever your ChessServer.java is)
package com.chess.server;

import com.chess.server.util.GameImporter; // Use the path to your new GameImporter
import java.nio.file.Paths;

public class ImporterMain {

    public static void main(String[] args) {
        // Step 1: Ensure the database and its tables are created.
        DatabaseManager.initializeDatabase();

        // Step 2: Create an instance of our new importer service.
        GameImporter importer = new GameImporter();

        // --- INSTRUCTIONS ---
        // 1. Place a .pgn file in the ROOT FOLDER of your IntelliJ project.
        //    (This is the same level as your pom.xml file and 'src' folder).
        // 2. Change the file name below to match the file you want to import.

        String fileName = "games.pgn"; // <-- CHANGE THIS

        System.out.println("Starting PGN import of: " + fileName);

        // Step 3: Run the import process.
        importer.importFromFile(Paths.get(fileName));

        System.out.println("Import process finished.");
    }
}