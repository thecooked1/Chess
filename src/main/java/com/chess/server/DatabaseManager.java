// In main.server.DatabaseManager.java
package com.chess.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:chess_app.db"; // Renamed for clarity

    /**
     * Initializes both the 'games' and 'users' tables in the database.
     * This method should be called once when the server starts.
     */
    public static void initializeDatabase() {
        String createGamesTableSql = "CREATE TABLE IF NOT EXISTS games ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " white_player TEXT NOT NULL,"
                + " black_player TEXT NOT NULL,"
                + " result TEXT NOT NULL,"
                + " pgn_data TEXT NOT NULL,"
                + " game_date TEXT NOT NULL"
                + ");";

        // --- NEW ---
        // SQL statement for creating the new users table
        String createUsersTableSql = "CREATE TABLE IF NOT EXISTS users ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT NOT NULL UNIQUE,"
                + " password_hash TEXT NOT NULL,"
                + " wins INTEGER DEFAULT 0,"
                + " losses INTEGER DEFAULT 0,"
                + " draws INTEGER DEFAULT 0"
                + ");";

        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement()) {
            // Create the tables if they don't exist
            stmt.execute(createGamesTableSql);
            stmt.execute(createUsersTableSql); // Execute the new statement
            System.out.println("SERVER: Database initialized. Tables 'games' and 'users' are ready.");
        } catch (SQLException e) {
            System.err.println("SERVER: Database initialization error: " + e.getMessage());
        }
    }

    /**
     * Inserts a completed game record into the database. (Your existing method)
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

            System.out.println("SERVER: Saved game between " + whitePlayer + " and " + blackPlayer + " to the database.");
        } catch (SQLException e) {
            System.err.println("SERVER: Error saving game to database: " + e.getMessage());
        }
    }

    // --- NEW ---
    /**
     * Provides a connection to the database.
     * This is needed by the new UserManager class.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
}