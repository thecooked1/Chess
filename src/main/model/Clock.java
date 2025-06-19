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

    public boolean isRunning() { return running; }
    public void start() { this.running = true; }
    public void stop() { this.running = false; }
    public boolean outOfTime() { return (hh <= 0 && mm <= 0 && ss <= 0); }

    public boolean decrement() {
        if (!running || outOfTime()) {
            return outOfTime();
        }
        if (ss > 0) {
            ss--;
        } else {
            if (mm > 0) {
                ss = 59;
                mm--;
            } else {
                if (hh > 0) {
                    ss = 59;
                    mm = 59;
                    hh--;
                } else {
                    running = false;
                    return true;
                }
            }
        }
        return false;
    }

    public String getTime() {
        return String.format("%02d:%02d:%02d", Math.max(0, hh), Math.max(0, mm), Math.max(0, ss));
    }

    public void setTime(int hh, int mm, int ss) {
        this.hh = hh;
        this.mm = mm;
        this.ss = ss;
        this.running = false;
    }
}