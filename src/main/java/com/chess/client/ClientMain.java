// Create in package: main.client
package com.chess.client;

import javax.swing.SwingUtilities;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create the network handler first, it will be passed around.
            NetworkHandler networkHandler = new NetworkHandler("127.0.0.1", 6789);
            new Thread(networkHandler).start(); // Start listening for messages

            // Show the authentication frame.
            // Pass the network handler to it so it can send login/register messages.
            new AuthFrame(networkHandler);
        });
    }
}