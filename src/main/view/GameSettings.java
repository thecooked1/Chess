package main.view;


public class GameSettings {
    public final String whiteName;
    public final String blackName;
    public final int hours;
    public final int minutes;
    public final int seconds;

    GameSettings(String wn, String bn, int h, int m, int s) {
        whiteName = wn;
        blackName = bn;
        hours = h;
        minutes = m;
        seconds = s;
    }
}
