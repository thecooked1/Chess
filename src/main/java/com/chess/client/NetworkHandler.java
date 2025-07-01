// main/client/NetworkHandler.java
package com.chess.client;

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
    private Consumer<String> onMessageReceived;

    public NetworkHandler(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void setMessageConsumer(Consumer<String> consumer) {
        this.onMessageReceived = consumer;
    }


    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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