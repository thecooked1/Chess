// Create in a new package: main.server.db
package com.chess.server.db;

import com.chess.server.DatabaseManager;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserManager {

    public String registerUser(String username, String password) {
        String sqlCheck = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck)) {
            pstmtCheck.setString(1, username);
            if (pstmtCheck.executeQuery().next()) {
                return "USERNAME_TAKEN";
            }
        } catch (SQLException e) {
            System.err.println("DB ERROR on user check: " + e.getMessage());
            return "ERROR";
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sqlInsert = "INSERT INTO users(username, password_hash) VALUES(?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
            pstmtInsert.setString(1, username);
            pstmtInsert.setString(2, hashedPassword);
            pstmtInsert.executeUpdate();
            return "SUCCESS";
        } catch (SQLException e) {
            System.err.println("DB ERROR on user insert: " + e.getMessage());
            return "ERROR";
        }
    }

    public boolean loginUser(String username, String password) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                return BCrypt.checkpw(password, storedHash);
            }
            return false; // User not found
        } catch (SQLException e) {
            System.err.println("DB ERROR on login: " + e.getMessage());
            return false;
        }
    }
}