package main.view;

import main.model.pieces.Colour;
import main.model.pieces.Pawn;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

public class StartMenu implements Runnable {

    private final Consumer<GameSettings> startGameCallback;

    public StartMenu(Consumer<GameSettings> startGameCallback) {
        this.startGameCallback = startGameCallback;
    }

    @Override
    public void run() {
        final JFrame startWindow = new JFrame("Chess Setup");
        startWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startWindow.setResizable(false);

        // ... (The top part of the method is mostly fine) ...
        Box components = Box.createVerticalBox();
        startWindow.add(components);
        components.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final JPanel titlePanel = new JPanel();
        components.add(titlePanel);
        final JLabel titleLabel = new JLabel("Setup New Chess Game");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        components.add(Box.createVerticalStrut(10));

        JPanel playerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // --- White Player (Corrected) ---
        gbc.gridx = 0; gbc.gridy = 0;
        // Use the new, type-safe method
        JLabel whiteIconLabel = createIconLabel(Colour.WHITE);
        playerPanel.add(whiteIconLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField whiteInput = new JTextField("White", 10);
        playerPanel.add(whiteInput, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        // --- Black Player (Corrected) ---
        gbc.gridx = 0; gbc.gridy = 1;
        // Use the new, type-safe method
        JLabel blackIconLabel = createIconLabel(Colour.BLACK);
        playerPanel.add(blackIconLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField blackInput = new JTextField("Black", 10);
        playerPanel.add(blackInput, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        components.add(playerPanel);
        components.add(Box.createVerticalStrut(10));

        // --- The rest of the run() method is unchanged and correct ---
        // (Timer panel, buttons, action listeners, etc.)
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        timerPanel.setBorder(BorderFactory.createTitledBorder("Time Control (HH:MM:SS)"));
        final String[] minSecInts = new String[60];
        for (int i = 0; i < 60; i++) minSecInts[i] = String.format("%02d", i);
        final String[] hourInts = {"00", "01", "02", "03", "04", "05"};
        final JComboBox<String> hours = new JComboBox<>(hourInts);
        final JComboBox<String> minutes = new JComboBox<>(minSecInts);
        final JComboBox<String> seconds = new JComboBox<>(minSecInts);
        hours.setSelectedItem("00");
        minutes.setSelectedItem("10");
        seconds.setSelectedItem("00");
        timerPanel.add(hours);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(minutes);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(seconds);
        components.add(timerPanel);
        components.add(Box.createVerticalStrut(15));

        Box buttons = Box.createHorizontalBox();
        final JButton start = new JButton("Start Game");
        final JButton quit = new JButton("Quit");
        start.addActionListener(e -> {
            String bn = blackInput.getText().trim();
            String wn = whiteInput.getText().trim();
            if (bn.isEmpty()) bn = "Black";
            if (wn.isEmpty()) wn = "White";
            int hh = Integer.parseInt((String) Objects.requireNonNull(hours.getSelectedItem()));
            int mm = Integer.parseInt((String) Objects.requireNonNull(minutes.getSelectedItem()));
            int ss = Integer.parseInt((String) Objects.requireNonNull(seconds.getSelectedItem()));
            GameSettings settings = new GameSettings(wn, bn, hh, mm, ss);
            startGameCallback.accept(settings);
            startWindow.dispose();
        });
        quit.addActionListener(e -> System.exit(0));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(start);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(quit);
        buttons.add(Box.createHorizontalGlue());
        components.add(buttons);

        Image icon = PieceImageLoader.getApplicationIcon();
        if (icon != null) startWindow.setIconImage(icon);

        startWindow.pack();
        startWindow.setMinimumSize(startWindow.getPreferredSize());
        startWindow.setLocationRelativeTo(null);
        startWindow.setVisible(true);
    }

    // --- CORRECTED Helper to create icon label ---
    private JLabel createIconLabel(Colour color) {
        try {
            // Create a temporary Pawn of the given color to get its image
            Image img = PieceImageLoader.getImage(new Pawn(color));

            if (img != null) {
                // Return a label with a scaled-down version of the image
                return new JLabel(new ImageIcon(img.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            }
        } catch (Exception e) {
            System.err.println("Icon not loaded for color " + color + " - " + e.getMessage());
        }
        // Fallback to a simple text label if the image fails
        return new JLabel(color == Colour.WHITE ? "W:" : "B:");
    }
}