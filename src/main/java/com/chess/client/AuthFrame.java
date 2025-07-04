// Create in package: main.client
package com.chess.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class AuthFrame extends JFrame {
    private final NetworkHandler networkHandler;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> timeControlComboBox;

    public AuthFrame(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        // Set the callback for handling server messages related to auth
        this.networkHandler.setMessageConsumer(this::handleServerMessage);

        setTitle("Chess - Login or Register");
        setSize(400, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Components
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);

        String[] timeOptions = {
                "10 minutes (Standard)",
                "5 minutes (Blitz)",
                "3 minutes (Bullet)",
                "15 minutes (Rapid)",
                "1 minute (Hyperbullet)"
        };
        timeControlComboBox = new JComboBox<>(timeOptions);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        // Layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; add(usernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Time Control:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; add(timeControlComboBox, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; add(buttonPanel, gbc);

        // Listeners
        loginButton.addActionListener(this::onLogin);
        registerButton.addActionListener(this::onRegister);

        setVisible(true);
    }

    private void onLogin(ActionEvent e) {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        networkHandler.sendMessage("LOGIN " + user + " " + pass);
    }

    private void onRegister(ActionEvent e) {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        networkHandler.sendMessage("REGISTER " + user + " " + pass);
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(" ", 2);
            String command = parts[0];
            String payload = parts.length > 1 ? parts[1].replace("_", " ") : "";

            switch (command) {
                case "LOGIN_SUCCESS":
                    // Login was successful! Close this auth window and start the game.
                    this.dispose(); // Close the login frame
                    String username = parts[1];
                    String selectedTime = (String) timeControlComboBox.getSelectedItem();
                    int timeInSeconds = parseTime(selectedTime);
                    // Launch the main GameController
                    new GameController(username, timeInSeconds, this.networkHandler);
                    break;
                case "LOGIN_FAIL":
                    JOptionPane.showMessageDialog(this, "Login Failed: " + payload, "Login Error", JOptionPane.ERROR_MESSAGE);
                    break;
                case "REGISTER_SUCCESS":
                    JOptionPane.showMessageDialog(this, "Registration Successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "REGISTER_FAIL":
                    JOptionPane.showMessageDialog(this, "Registration Failed: " + payload, "Registration Error", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        });
    }

    private int parseTime(String timeString) {
        if (timeString == null) return 600; // Default 10 minutes
        // Extracts the number from the string (e.g., "10 minutes..." -> 10)
        String minutesStr = timeString.split(" ")[0];
        try {
            int minutes = Integer.parseInt(minutesStr);
            return minutes * 60;
        } catch (NumberFormatException e) {
            return 600; // Default on parsing error
        }
    }

}