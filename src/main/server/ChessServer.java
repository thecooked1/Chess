package main.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChessServer {
    public static void main(String[] args) {
        int port = 6789; // Port to listen on
        System.out.println("Chess Server is starting on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Waiting for two players to connect...");
                
                // Wait for the first player (White)
                Socket player1Socket = serverSocket.accept();
                System.out.println("Player 1 (White) connected: " + player1Socket.getInetAddress());

                // Wait for the second player (Black)
                Socket player2Socket = serverSocket.accept();
                System.out.println("Player 2 (Black) connected: " + player2Socket.getInetAddress());

                System.out.println("Both players connected. Starting new game handler...");

                // Create a new thread to handle this game, so the server can accept more games
                GameHandler gameHandler = new GameHandler(player1Socket, player2Socket);
                new Thread(gameHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}