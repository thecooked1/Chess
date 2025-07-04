// Create in package: main.server
package com.chess.server;

import com.chess.server.db.UserManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Matchmaker matchmaker;
    private final UserManager userManager;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean isAuthenticated = false;
    private int preferredTime = 600; // Default
    private volatile boolean isInGame = false;
    private final CountDownLatch gameFinishedLatch = new CountDownLatch(1);

    public ClientHandler(Socket socket, Matchmaker matchmaker) {
        this.socket = socket;
        this.matchmaker = matchmaker;
        this.userManager = new UserManager();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (!isAuthenticated && !socket.isClosed()) {
                String authMessage = in.readLine();
                if (authMessage == null) break;
                handleAuthCommand(authMessage);
            }

            if (isAuthenticated) {
                handleSetupPhase();
                matchmaker.addPlayerToQueue(this);

                // --- NEW WAITING MECHANISM ---
                // Wait here until the game is over.
                // The latch will be released by the GameHandler.
                gameFinishedLatch.await();
            }
        } catch (IOException e) {
            System.err.println("SERVER: Client " + (username != null ? username : "unauth") + " error: " + e.getMessage());
        } catch (InterruptedException e) {
            // This thread was interrupted, which is a way to shut it down.
            Thread.currentThread().interrupt();
            System.err.println("SERVER: ClientHandler for " + username + " was interrupted.");
        } finally {
            matchmaker.removePlayerFromQueue(this);
            try { socket.close(); } catch (IOException e) { /* Ignore */ }
            System.out.println("SERVER: ClientHandler for " + (username != null ? username : "unauth") + " is shutting down.");
        }
    }

    private void handleAuthCommand(String message) {
        String[] parts = message.split(" ", 3);
        String command = parts[0];
        if (parts.length < 3) {
            sendMessage("ERROR BAD_REQUEST");
            return;
        }
        String user = parts[1];
        String pass = parts[2];

        if (command.equals("REGISTER")) {
            String result = userManager.registerUser(user, pass);
            if (result.equals("SUCCESS")) sendMessage("REGISTER_SUCCESS");
            else sendMessage("REGISTER_FAIL " + result.replace(" ", "_"));
        } else if (command.equals("LOGIN")) {
            boolean success = userManager.loginUser(user, pass);
            if (success) {
                this.username = user;
                this.isAuthenticated = true;
                sendMessage("LOGIN_SUCCESS " + user);
            } else {
                sendMessage("LOGIN_FAIL Invalid_username_or_password");
            }
        }
    }

    private void handleSetupPhase() throws IOException {
        boolean timeSet = false;
        boolean ready = false;

        // This loop now waits for both time and ready signals.
        while (!ready) {
            String setupMessage = in.readLine();
            if (setupMessage == null) throw new IOException("Client disconnected during setup.");

            String[] parts = setupMessage.split(" ", 2);
            String command = parts[0];

            if (command.equals("SET_TIME")) {
                try {
                    this.preferredTime = Integer.parseInt(parts[1]);
                    timeSet = true;
                } catch (Exception e) {
                    sendMessage("ERROR Invalid_time_format");
                }
            } else if (command.equals("PLAYER_READY")) {
                if (!timeSet) {
                    // Optionally enforce that time must be set before ready.
                    // For now, we can just proceed with the default time.
                    System.out.println("SERVER: " + username + " sent PLAYER_READY without SET_TIME. Using default.");
                }
                sendMessage("INFO Acknowledged_ready");
                ready = true; // Set flag to exit the loop.

            } else if (command.equals("PLAY_BOT")) {
                sendMessage("INFO Creating_game_with_bot");
                // Directly start a game against a bot instead of going to the matchmaker.
                new Thread(new GameHandler(this)).start();
                ready = true; // Mark setup as complete.
                break; // Break from setup, the thread will now wait on the latch.

            } else {
                // This is where "SET_NAME" would have been caught.
                sendMessage("ERROR Unexpected_command_in_setup_phase:_" + command);
            }
        }
        // After the loop finishes (because ready is true), the run() method will
        // proceed to add the player to the matchmaker.
    }

    public void signalGameFinished() {
        gameFinishedLatch.countDown();
    }

    public void sendMessage(String message) { out.println(message); }
    public String getUsername() { return username; }
    public int getPreferredTime() { return preferredTime; }
    public PrintWriter getWriter() { return out; }
    public BufferedReader getReader() { return in; }
}