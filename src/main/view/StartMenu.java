package main.view;

import main.controller.GameController; // Needs reference to start game
import main.model.PieceType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer; // To pass game settings back

public class StartMenu implements Runnable {

    private final Consumer<GameSettings> startGameCallback;

    public StartMenu(Consumer<GameSettings> startGameCallback) {
        this.startGameCallback = startGameCallback;
    }

    @Override
    public void run() {
        final JFrame startWindow = new JFrame("Chess Setup");
        startWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startWindow.setLocation(300, 100);
        startWindow.setResizable(false);

        Box components = Box.createVerticalBox();
        startWindow.add(components);

        // Padding around components
        components.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


        // Game title
        final JPanel titlePanel = new JPanel();
        components.add(titlePanel);
        final JLabel titleLabel = new JLabel("Setup New Chess Game");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel);
        components.add(Box.createVerticalStrut(10));

        // Player inputs using GridBagLayout for better alignment
        JPanel playerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding
        gbc.anchor = GridBagConstraints.WEST;

        // White Player
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel whiteIconLabel = createIconLabel("wpawn.png"); // Use PieceImageLoader? Better for consistency
        playerPanel.add(whiteIconLabel != null ? whiteIconLabel : new JLabel("W:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField whiteInput = new JTextField("White", 10);
        playerPanel.add(whiteInput, gbc);
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        gbc.weightx = 0.0; // Reset weight

        // Black Player
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel blackIconLabel = createIconLabel("bpawn.png");
        playerPanel.add(blackIconLabel != null ? blackIconLabel : new JLabel("B:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField blackInput = new JTextField("Black", 10);
        playerPanel.add(blackInput, gbc);
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        gbc.weightx = 0.0; // Reset weight

        components.add(playerPanel);
        components.add(Box.createVerticalStrut(10));

        // Timer settings
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        timerPanel.setBorder(BorderFactory.createTitledBorder("Time Control (HH:MM:SS)"));

        final String[] minSecInts = new String[60];
        for (int i = 0; i < 60; i++) {
            minSecInts[i] = String.format("%02d", i);
        }
        final String[] hourInts = {"00", "01", "02", "03", "04", "05"}; // Allow more hours?

        final JComboBox<String> hours = new JComboBox<>(hourInts);
        final JComboBox<String> minutes = new JComboBox<>(minSecInts);
        final JComboBox<String> seconds = new JComboBox<>(minSecInts);

        hours.setSelectedItem("00");
        minutes.setSelectedItem("10"); // Default to 10 mins
        seconds.setSelectedItem("00");

        timerPanel.add(hours);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(minutes);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(seconds);

        components.add(timerPanel);
        components.add(Box.createVerticalStrut(15));


        // Buttons
        Box buttons = Box.createHorizontalBox();
        final JButton start = new JButton("Start Game");
        final JButton quit = new JButton("Quit");
        // Add instructions button if desired

        start.addActionListener(e -> {
            String bn = blackInput.getText().trim();
            String wn = whiteInput.getText().trim();
            if (bn.isEmpty()) bn = "Black";
            if (wn.isEmpty()) wn = "White";

            int hh = Integer.parseInt((String) Objects.requireNonNull(hours.getSelectedItem()));
            int mm = Integer.parseInt((String) Objects.requireNonNull(minutes.getSelectedItem()));
            int ss = Integer.parseInt((String) Objects.requireNonNull(seconds.getSelectedItem()));

            GameSettings settings = new GameSettings(wn, bn, hh, mm, ss);
            startGameCallback.accept(settings); // Pass settings to controller callback
            startWindow.dispose(); // Close the start menu
        });

        quit.addActionListener(e -> System.exit(0)); // Exit application


        buttons.add(Box.createHorizontalGlue()); // Push buttons to the right/center
        buttons.add(start);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(quit);
        buttons.add(Box.createHorizontalGlue());

        components.add(buttons);

        // Set application icon
        Image icon = PieceImageLoader.getApplicationIcon();
        if (icon != null) {
            startWindow.setIconImage(icon);
        }

        startWindow.pack();
        startWindow.setMinimumSize(startWindow.getPreferredSize());
        startWindow.setLocationRelativeTo(null); // Center
        startWindow.setVisible(true);
    }

    // Helper to create icon label (could be improved)
    private JLabel createIconLabel(String resourceName) {
        try {
            Image img = PieceImageLoader.getImage( // Need to map filename back to Type/Color or load directly
                    PieceType.PAWN, // Simplistic guess
                    resourceName.contains("wpawn") ? Color.WHITE : Color.BLACK
            );
            // Directly load if PieceImageLoader isn't adapted for generic names
            // InputStream is = getClass().getResourceAsStream("/" + resourceName);
            // Image img = ImageIO.read(is);

            if (img != null) {
                return new JLabel(new ImageIcon(img.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            }
        } catch (Exception e) {
            System.err.println("Icon not loaded: " + resourceName + " - " + e.getMessage());
        }
        return null; // Fallback
    }
}