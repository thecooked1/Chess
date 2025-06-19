package main.view;

import main.model.pieces.Colour;
import main.model.pieces.Pawn;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;


public class StartMenu {

    private final JFrame startWindow;

    public StartMenu(Consumer<GameSettings> startGameCallback) {
        startWindow = new JFrame("New Chess Game Setup");
        startWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startWindow.setResizable(false);
        startWindow.setLayout(new BorderLayout(10, 10));

        // --- Title ---
        JLabel titleLabel = new JLabel("Setup New Chess Game", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        startWindow.add(titleLabel, BorderLayout.NORTH);

        // --- Input Fields ---
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Player Names
        gbc.gridy = 0; gbc.gridx = 0; fieldsPanel.add(new JLabel("White Player:"), gbc);
        gbc.gridy = 1; fieldsPanel.add(new JLabel("Black Player:"), gbc);
        gbc.gridy = 2; fieldsPanel.add(new JLabel("Time Control:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JTextField whiteInput = new JTextField("Player 1", 15);
        fieldsPanel.add(whiteInput, gbc);

        gbc.gridy = 1;
        JTextField blackInput = new JTextField("Player 2", 15);
        fieldsPanel.add(blackInput, gbc);

        // Time Control
        gbc.gridy = 2;
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        String[] minSecInts = new String[60];
        for (int i = 0; i < 60; i++) minSecInts[i] = String.format("%02d", i);
        JComboBox<String> hours = new JComboBox<>(new String[]{"00", "01", "02"});
        JComboBox<String> minutes = new JComboBox<>(minSecInts);
        JComboBox<String> seconds = new JComboBox<>(minSecInts);
        minutes.setSelectedItem("0"); // Default to 0
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
            // Package the user's choices into our settings object
            String wn = whiteInput.getText().trim().isEmpty() ? "Player 1" : whiteInput.getText().trim();
            String bn = blackInput.getText().trim().isEmpty() ? "Player 2" : blackInput.getText().trim();
            int hh = Integer.parseInt((String) Objects.requireNonNull(hours.getSelectedItem()));
            int mm = Integer.parseInt((String) Objects.requireNonNull(minutes.getSelectedItem()));
            int ss = Integer.parseInt((String) Objects.requireNonNull(seconds.getSelectedItem()));
            GameSettings settings = new GameSettings(wn, bn, hh, mm, ss);

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