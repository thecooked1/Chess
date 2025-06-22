// main/client/NetworkHandler.java
package main.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class NetworkHandler implements Runnable {

    private final String serverAddress;
    private final int serverPort;
    private PrintWriter out;
    private final Consumer<String> onMessageReceived;

    // --- THIS IS THE CRITICAL PART THAT IS LIKELY MISSING ---
    private final List<String> initialMessages;

    /**
     * The main constructor that accepts initial messages to send upon connection.
     */
    public NetworkHandler(String serverAddress, int serverPort, Consumer<String> onMessageReceived, List<String> initialMessages) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.onMessageReceived = onMessageReceived;
        this.initialMessages = initialMessages != null ? initialMessages : Collections.emptyList();
    }

    /**
     * A secondary constructor for backwards compatibility or simpler use cases.
     * This is the constructor the compiler is looking for.
     */
    public NetworkHandler(String serverAddress, int serverPort, Consumer<String> onMessageReceived) {
        // It calls the main constructor with an empty list of initial messages.
        this(serverAddress, serverPort, onMessageReceived, null);
    }


    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // After connecting, send any initial setup messages that were provided.
            for (String message : initialMessages) {
                sendMessage(message);
            }

            // Listen for messages from the server in a loop
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                onMessageReceived.accept(serverMessage);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            onMessageReceived.accept("ERROR Connection lost.");
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}