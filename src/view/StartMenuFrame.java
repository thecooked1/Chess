package view;

import controller.GameController; // Need controller to start game

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StartMenuFrame extends JFrame implements Runnable {

    private JTextField whiteInput, blackInput;
    private JComboBox<String> hoursCombo, minutesCombo, secondsCombo;
    private GameController controller; // To initiate game start

    // Constructor accepting the controller
    public StartMenuFrame(GameController controller) {
        super("Chess Setup");
        this.controller = controller;
        // run(); // Call run method to build UI
    }

    // Default constructor if run directly or needs adaptation
    public StartMenuFrame() {
        super("Chess Setup");
        // If used without a pre-existing controller, how does it get one?
        // This design might need refinement. Maybe Main creates Controller first.
        // For now, let's assume the controller is passed in.
    }


    @Override
    public void run() { // Builds and shows the frame
        // Set window properties
        setLocation(300, 100);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Exit app if this closes

        // Use Box Layout for vertical arrangement
        Box mainBox = Box.createVerticalBox();
        mainBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(mainBox);

        // Title
        JLabel titleLabel = new JLabel("Chess Game Setup");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainBox.add(titleLabel);
        mainBox.add(Box.createVerticalStrut(20));


        // Player Inputs (Using Grid Layout for alignment within horizontal boxes)
        JPanel whitePanel = createPlayerPanel("White:", "wpawn.png"); // Pass icon filename
        whiteInput = new JTextField("White", 15);
        whitePanel.add(whiteInput);

        JPanel blackPanel = createPlayerPanel("Black:", "bpawn.png");
        blackInput = new JTextField("Black", 15);
        blackPanel.add(blackInput);

        mainBox.add(whitePanel);
        mainBox.add(Box.createVerticalStrut(5));
        mainBox.add(blackPanel);
        mainBox.add(Box.createVerticalStrut(20));

        // Timer Settings
        mainBox.add(new JLabel("Timer (HH:MM:SS):"));
        mainBox.add(Box.createVerticalStrut(5));

        Box timerBox = Box.createHorizontalBox();
        String[] minSecInts = new String[60];
        for (int i = 0; i < 60; i++) {
            minSecInts[i] = String.format("%02d", i);
        }
        String[] hourInts = {"00", "01", "02", "03", "04", "05"}; // Example hours

        hoursCombo = new JComboBox<>(hourInts);
        minutesCombo = new JComboBox<>(minSecInts);
        secondsCombo = new JComboBox<>(minSecInts);

        // Set preferred sizes to prevent large combos
        Dimension comboSize = new Dimension(60, hoursCombo.getPreferredSize().height);
        hoursCombo.setPreferredSize(comboSize);
        minutesCombo.setPreferredSize(comboSize);
        secondsCombo.setPreferredSize(comboSize);

        timerBox.add(hoursCombo);
        timerBox.add(Box.createHorizontalStrut(5));
        timerBox.add(new JLabel(":"));
        timerBox.add(Box.createHorizontalStrut(5));
        timerBox.add(minutesCombo);
        timerBox.add(Box.createHorizontalStrut(5));
        timerBox.add(new JLabel(":"));
        timerBox.add(Box.createHorizontalStrut(5));
        timerBox.add(secondsCombo);
        timerBox.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the timer box
        mainBox.add(timerBox);
        mainBox.add(Box.createVerticalStrut(20));


        // Buttons
        Box buttonBox = Box.createHorizontalBox();
        JButton startButton = new JButton("Start Game");
        JButton quitButton = new JButton("Quit");

        startButton.addActionListener(e -> startGame());
        quitButton.addActionListener(e -> System.exit(0));

        buttonBox.add(Box.createHorizontalGlue()); // Push buttons to center/right
        buttonBox.add(startButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(quitButton);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainBox.add(buttonBox);

        // Set Frame Icon
        try {
            Image icon = ImageIO.read(getClass().getResource("/assets/wking.png"));
            setIconImage(icon);
        } catch (Exception e) {
            System.err.println("Warning: Could not load start menu icon.");
        }

        pack(); // Adjust size to components
        setLocationRelativeTo(null); // Center
        setVisible(true);
    }

    // Helper to create player input row
    private JPanel createPlayerPanel(String labelText, String iconFileName) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(labelText);
        try {
            Image img = ImageIO.read(getClass().getResource("/assets/" + iconFileName));
            // Scale icon if needed
            label.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            System.err.println("Warning: Could not load player icon " + iconFileName);
        }
        panel.add(label);
        return panel;
    }


    private void startGame() {
        if (controller == null) {
            JOptionPane.showMessageDialog(this, "Error: Game Controller not initialized.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String whiteName = whiteInput.getText().trim();
        String blackName = blackInput.getText().trim();
        if (whiteName.isEmpty()) whiteName = "White";
        if (blackName.isEmpty()) blackName = "Black";

        try {
            int hh = Integer.parseInt((String) hoursCombo.getSelectedItem());
            int mm = Integer.parseInt((String) minutesCombo.getSelectedItem());
            int ss = Integer.parseInt((String) secondsCombo.getSelectedItem());

            // Tell the controller to start the game with these settings
            controller.initializeNewGame(whiteName, blackName, hh, mm, ss);

            this.setVisible(false); // Hide start menu
            this.dispose(); // Release resources

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid time format.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Setter for the controller if needed after construction
    public void setController(GameController controller) {
        this.controller = controller;
    }
}