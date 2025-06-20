package main.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkHandler implements Runnable {

    private final String serverAddress;
    private final int serverPort;
    private PrintWriter out;
    private BufferedReader in;
    private final Consumer<String> onMessageReceived; // Callback to notify the controller

    public NetworkHandler(String serverAddress, int serverPort, Consumer<String> onMessageReceived) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.onMessageReceived = onMessageReceived;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Pass the "connected" message to the controller
            onMessageReceived.accept("CONNECTED");

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                // When a message arrives, pass it to the controller via the callback
                onMessageReceived.accept(serverMessage);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            onMessageReceived.accept("ERROR Connection lost.");
        }
    }

    /**
     * Sends a message to the server.
     * @param message The message to send.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}