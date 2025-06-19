package main.view;


public class GameSettings {
    public final String whitePlayerName;
    public final String blackPlayerName;
    public final int initialHours;
    public final int initialMinutes;
    public final int initialSeconds;

    public GameSettings(String whitePlayerName, String blackPlayerName, int hours, int minutes, int seconds) {
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
        this.initialHours = hours;
        this.initialMinutes = minutes;
        this.initialSeconds = seconds;
    }
}
