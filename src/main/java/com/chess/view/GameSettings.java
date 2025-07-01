package com.chess.view;

public class GameSettings {
    // --- SIMPLIFIED: Only needs to know this player's name ---
    public final String playerName;
    public final int initialHours;
    public final int initialMinutes;
    public final int initialSeconds;

    public GameSettings(String playerName, int hours, int minutes, int seconds) {
        this.playerName = playerName;
        this.initialHours = hours;
        this.initialMinutes = minutes;
        this.initialSeconds = seconds;
    }

    public int getTotalSeconds() {
        return (initialHours * 3600) + (initialMinutes * 60) + initialSeconds;
    }
}