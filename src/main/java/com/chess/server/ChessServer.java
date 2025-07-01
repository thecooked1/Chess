// In main.server.ChessServer.java
package com.chess.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChessServer {
    public static void main(String[] args) {
        int port = 6789;
        DatabaseManager.initializeDatabase();
        System.out.println("Chess Server is starting on port " + port);

        Matchmaker matchmaker = new Matchmaker();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening for connections...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("SERVER: New client connected: " + socket.getInetAddress());
                new Thread(new ClientHandler(socket, matchmaker)).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }
}