package model;

import javax.swing.*;
import view.*;

public class Game implements Runnable {
    public void run() {
        SwingUtilities.invokeLater(new StartMenu());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Game());
    }
}
