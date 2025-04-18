package main.model;

public class Clock {
    private int hh;
    private int mm;
    private int ss;
    private boolean running;

    public Clock(int hh, int mm, int ss) {
        this.hh = hh;
        this.mm = mm;
        this.ss = ss;
        this.running = false; // Start paused
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public boolean outOfTime() {
        return (hh <= 0 && mm <= 0 && ss <= 0);
    }

    // Decrements time by one second if running
    public boolean decrement() {
        if (!running || outOfTime()) {
            return outOfTime(); // Return true if already out of time
        }

        if (ss > 0) {
            ss--;
        } else { // ss == 0
            if (mm > 0) {
                ss = 59;
                mm--;
            } else { // mm == 0
                if (hh > 0) {
                    ss = 59;
                    mm = 59;
                    hh--;
                } else {
                    // Time is up now
                    running = false; // Stop the clock
                    return true;
                }
            }
        }
        return false; // Time not out yet
    }

    public String getTime() {
        String fHrs = String.format("%02d", Math.max(0, this.hh));
        String fMins = String.format("%02d", Math.max(0, this.mm));
        String fSecs = String.format("%02d", Math.max(0, this.ss));
        return fHrs + ":" + fMins + ":" + fSecs;
    }

    // Reset clock to a specific time
    public void setTime(int hh, int mm, int ss) {
        this.hh = hh;
        this.mm = mm;
        this.ss = ss;
        this.running = false; // Ensure it's stopped after reset
    }
}