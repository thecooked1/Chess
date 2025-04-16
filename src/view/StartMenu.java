package view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;


public class StartMenu implements Runnable {

    private final GameLaunchCallback launchCallback;

    public StartMenu(GameLaunchCallback callback) {
        this.launchCallback = callback;
    }

    public void run() {
        final JFrame startWindow = new JFrame("Chess Setup");
        // Set window properties
        startWindow.setLocationByPlatform(true); // Let OS decide position initially
        startWindow.setResizable(false);
        // startWindow.setSize(260, 240); // Let pack() determine size

        Box components = Box.createVerticalBox();
        components.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        startWindow.add(components);

        // Game title
        final JPanel titlePanel = new JPanel();
        components.add(titlePanel);
        final JLabel titleLabel = new JLabel("Chess Game Setup");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titlePanel.add(titleLabel);
        components.add(Box.createVerticalStrut(10));

        // Player Name Inputs using GridBagLayout for better alignment
        JPanel namePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 5); // Padding

        // White player selections
        final JLabel whitePiece = new JLabel();
        try {
            URL whiteUrl = getClass().getClassLoader().getResource("resources/wp.png");
            if (whiteUrl != null) {
                Image whiteImg = ImageIO.read(whiteUrl);
                whitePiece.setIcon(new ImageIcon(whiteImg));
                // Set frame icon
                startWindow.setIconImage(whiteImg);
            } else System.err.println("Required game file wp.png missing");
        } catch (Exception e) {
            System.err.println("Required game file wp.png missing: " + e.getMessage());
        }
        namePanel.add(whitePiece, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField whiteInput = new JTextField("White", 12);
        namePanel.add(whiteInput, gbc);

        // Black player selections
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        final JLabel blackPiece = new JLabel();
        try {
            URL blackUrl = getClass().getClassLoader().getResource("resources/bp.png");
            if (blackUrl != null) {
                Image blackImg = ImageIO.read(blackUrl);
                blackPiece.setIcon(new ImageIcon(blackImg));
            } else System.err.println("Required game file bp.png missing");
        } catch (Exception e) {
            System.err.println("Required game file bp.png missing: " + e.getMessage());
        }
        namePanel.add(blackPiece, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final JTextField blackInput = new JTextField("Black", 12);
        namePanel.add(blackInput, gbc);

        components.add(namePanel);
        components.add(Box.createVerticalStrut(15));


        // Timer settings
        final String[] minSecInts = new String[60];
        for (int i = 0; i < 60; i++) {
            minSecInts[i] = String.format("%02d", i);
        }
        final String[] hourInts = {"00", "01", "02", "03", "04", "05"}; // More realistic hour options?


        final JComboBox<String> seconds = new JComboBox<>(minSecInts);
        final JComboBox<String> minutes = new JComboBox<>(minSecInts);
        final JComboBox<String> hours = new JComboBox<>(hourInts);

        // Set default time (e.g., 10 minutes)
        hours.setSelectedItem("00");
        minutes.setSelectedItem("10");
        seconds.setSelectedItem("00");

        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        timerPanel.add(new JLabel("Time (H:M:S):"));
        timerPanel.add(hours);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(minutes);
        timerPanel.add(new JLabel(":"));
        timerPanel.add(seconds);

        components.add(timerPanel);
        components.add(Box.createVerticalStrut(15));


        // Buttons
        Box buttons = Box.createHorizontalBox();
        final JButton quit = new JButton("Quit");
        quit.addActionListener(e -> startWindow.dispose());

        final JButton instr = new JButton("Instructions");
        instr.addActionListener(e -> JOptionPane.showMessageDialog(startWindow,
                "Enter player names.\n" +
                        "Set the time per player (HH:MM:SS).\n" +
                        "Setting time to 00:00:00 means an untimed game.\n" +
                        "Click \"Start\" to begin.",
                "How to Play",
                JOptionPane.INFORMATION_MESSAGE));

        final JButton start = new JButton("Start");
        start.addActionListener(e -> {
            String bn = blackInput.getText().trim();
            String wn = whiteInput.getText().trim();
            if(bn.isEmpty()) bn = "Black";
            if(wn.isEmpty()) wn = "White";

            try {
                int hh = Integer.parseInt((String) hours.getSelectedItem());
                int mm = Integer.parseInt((String) minutes.getSelectedItem());
                int ss = Integer.parseInt((String) seconds.getSelectedItem());

                // Use the callback to launch the game
                launchCallback.launch(wn, bn, hh, mm, ss);
                startWindow.dispose(); // Close the start menu
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(startWindow, "Invalid time setting.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttons.add(Box.createHorizontalGlue()); // Push buttons to the right (or center depending on layout)
        buttons.add(start);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(instr);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(quit);
        buttons.add(Box.createHorizontalGlue());


        components.add(buttons);

        startWindow.pack(); // Size window optimally
        startWindow.setLocationRelativeTo(null); // Center
        startWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startWindow.setVisible(true);
    }

    // Optional: Add a main method here for testing the StartMenu independently
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Example of using the callback
            GameLaunchCallback launcher = (wn, bn, h, m, s) -> {
                System.out.println("Starting game with:");
                System.out.println("White: " + wn);
                System.out.println("Black: " + bn);
                System.out.println("Time: " + String.format("%02d:%02d:%02d", h, m, s));
                // In real use, this would call Game.launchGame(...)
                // For testing, we just print.
            };
            new StartMenu(launcher).run();
        });
    }
}