// main/server/DatabaseManager.java
package main.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:chess_games.db";

    /**
     * Initializes the database. If the games table does not exist, it creates it.
     * This method should be called once when the server starts.
     */
    public static void initializeDatabase() {
        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS games ("
                + " id integer PRIMARY KEY AUTOINCREMENT,"
                + " white_player text NOT NULL,"
                + " black_player text NOT NULL,"
                + " result text NOT NULL,"
                + " pgn_data text NOT NULL,"
                + " game_date text NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement()) {
            // Create a new table
            stmt.execute(sql);
            System.out.println("Database initialized. Table 'games' is ready.");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    /**
     * Inserts a completed game record into the database.
     *
     * @param whitePlayer The name of the white player.
     * @param blackPlayer The name of the black player.
     * @param result      The game result ("1-0", "0-1", "1/2-1/2", "*").
     * @param pgn         The full PGN text of the game.
     * @param date        The date the game was played (e.g., "YYYY.MM.DD").
     */
    public static void saveGame(String whitePlayer, String blackPlayer, String result, String pgn, String date) {
        String sql = "INSERT INTO games(white_player, black_player, result, pgn_data, game_date) VALUES(?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, whitePlayer);
            pstmt.setString(2, blackPlayer);
            pstmt.setString(3, result);
            pstmt.setString(4, pgn);
            pstmt.setString(5, date);
            pstmt.executeUpdate();

            System.out.println("Successfully saved game between " + whitePlayer + " and " + blackPlayer + " to the database.");

        } catch (SQLException e) {
            System.err.println("Error saving game to database: " + e.getMessage());
        }
    }
}