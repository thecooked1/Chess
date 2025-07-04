package com.chess.server;

import java.util.LinkedList;
import java.util.Queue;

public class Matchmaker {
    private final Queue<ClientHandler> waitingPool = new LinkedList<>();

    public synchronized void addPlayerToQueue(ClientHandler player) {
        System.out.println("SERVER: " + player.getUsername() + " entered the matchmaking pool.");

        ClientHandler opponent = waitingPool.poll();
        if (opponent != null) {
            System.out.println("SERVER: Match found for " + player.getUsername() + " and " + opponent.getUsername());
            GameHandler gameHandler = new GameHandler(player, opponent);
            new Thread(gameHandler).start();
        } else {
            player.sendMessage("INFO Waiting_for_an_opponent...");
            waitingPool.add(player);
            System.out.println("SERVER: " + player.getUsername() + " is waiting for a match.");
        }
    }

    public synchronized void removePlayerFromQueue(ClientHandler player) {
        if (waitingPool.remove(player)) {
            System.out.println("SERVER: " + player.getUsername() + " removed from pool (disconnected).");
        }
    }
}