package model;

/**
 * Represents a simple countdown clock for a player.
 */
public class Clock {
    int hh;
    int mm;
    int ss;
    private boolean running;

    public Clock(int hh, int mm, int ss) {
        this.hh = hh;
        this.mm = mm;
        this.ss = ss;
        this.running = false; // Clock usually starts paused
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isOutOfTime() {
        return (hh <= 0 && mm <= 0 && ss <= 0);
    }

    /**
     * Decrements the clock by one second if it's running.
     * Does nothing if the clock is stopped or already out of time.
     */
    public void decrementSecond() {
        if (!running || isOutOfTime()) {
            return;
        }

        if (ss > 0) {
            ss--;
        } else { // ss == 0
            if (mm > 0) {
                mm--;
                ss = 59;
            } else { // mm == 0
                if (hh > 0) {
                    hh--;
                    mm = 59;
                    ss = 59;
                } else {
                    // Time is up, state is now 00:00:00
                    // No need to go negative
                }
            }
        }
    }

    /**
     * Gets the current time as a formatted string HH:MM:SS.
     * @return The formatted time string.
     */
    public String getTime() {
        // Ensure values don't display negative if somehow decremented past zero
        int displayH = Math.max(0, hh);
        int displayM = Math.max(0, mm);
        int displayS = Math.max(0, ss);
        return String.format("%02d:%02d:%02d", displayH, displayM, displayS);
    }

    /**
     * Gets the total time remaining in seconds.
     * @return total seconds remaining.
     */
    public long getTotalSeconds() {
        return Math.max(0L, (long)hh * 3600 + (long)mm * 60 + ss);
    }

    /**
     * Sets the clock to a specific time. Stops the clock.
     * @param newHh Hours
     * @param newMm Minutes
     * @param newSs Seconds
     */
    public void setTime(int newHh, int newMm, int newSs) {
        this.hh = Math.max(0, newHh);
        this.mm = Math.max(0, newMm);
        this.ss = Math.max(0, newSs);
        this.running = false;
    }
}