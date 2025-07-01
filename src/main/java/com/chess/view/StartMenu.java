// main/view/StartMenu.java
package com.chess.view;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

public class StartMenu {

    private final JFrame startWindow;

    public StartMenu(Consumer<GameSettings> startGameCallback) {
        startWindow = new JFrame("Chess Game Setup");
        startWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startWindow.setResizable(false);
        startWindow.setLayout(new BorderLayout(10, 10));

        // --- Title ---
        JLabel titleLabel = new JLabel("Setup New Game", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        startWindow.add(titleLabel, BorderLayout.NORTH);

        // --- Input Fields ---
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Simplified UI: Only asks for one player's name
        gbc.gridy = 0; gbc.gridx = 0; fieldsPanel.add(new JLabel("Your Name:"), gbc);
        gbc.gridy = 1; fieldsPanel.add(new JLabel("Time Control:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JTextField nameInput = new JTextField("Player", 15);
        fieldsPanel.add(nameInput, gbc);

        // Time Control
        gbc.gridy = 1;
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        String[] minSecValues = new String[60];
        for (int i = 0; i < 60; i++) minSecValues[i] = String.format("%02d", i);
        JComboBox<String> hours = new JComboBox<>(new String[]{"00", "01", "02"});
        JComboBox<String> minutes = new JComboBox<>(minSecValues);
        JComboBox<String> seconds = new JComboBox<>(minSecValues);

        minutes.setSelectedItem("10"); // Default to 10 minutes
        seconds.setSelectedItem("00");

        timerPanel.add(hours);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(minutes);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(seconds);
        fieldsPanel.add(timerPanel, gbc);

        startWindow.add(fieldsPanel, BorderLayout.CENTER);

        // --- Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton startButton = new JButton("Start Game");
        JButton quitButton = new JButton("Quit");
        buttonPanel.add(startButton);
        buttonPanel.add(quitButton);
        startWindow.add(buttonPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        quitButton.addActionListener(e -> System.exit(0));

        startButton.addActionListener(e -> {
            // Get values from the UI components
            String playerName = nameInput.getText().trim().isEmpty() ? "Player" : nameInput.getText().trim();
            int hh = Integer.parseInt((String) Objects.requireNonNull(hours.getSelectedItem()));
            int mm = Integer.parseInt((String) Objects.requireNonNull(minutes.getSelectedItem()));
            int ss = Integer.parseInt((String) Objects.requireNonNull(seconds.getSelectedItem()));

            // Use the new, simpler GameSettings constructor
            GameSettings settings = new GameSettings(playerName, hh, mm, ss);

            // Close the start menu
            startWindow.dispose();

            // Trigger the callback function, passing it the new settings
            startGameCallback.accept(settings);
        });

        startWindow.pack();
        startWindow.setLocationRelativeTo(null);
    }

    /** Makes the start menu window visible. */
    public void show() {
        startWindow.setVisible(true);
    }
}