package com.chess.client;

import javax.swing.SwingUtilities;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NetworkHandler networkHandler = new NetworkHandler("127.0.0.1", 6789);
            new Thread(networkHandler).start();
            new AuthFrame(networkHandler);
        });
    }
}