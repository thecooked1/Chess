package model;

public class ClockModel {
    private int hh;
    private int mm;
    private int ss;
    private final int initialHh;
    private final int initialMm;
    private final int initialSs;

    public ClockModel(int hh, int mm, int ss) {
        this.initialHh = hh;
        this.initialMm = mm;
        this.initialSs = ss;
        setTime(hh, mm, ss);
    }

    public void setTime(int hh, int mm, int ss) {
        this.hh = hh;
        this.mm = mm;
        this.ss = ss;
    }

    public void reset() {
        setTime(initialHh, initialMm, initialSs);
    }

    public boolean isOutOfTime() {
        return (hh <= 0 && mm <= 0 && ss <= 0); // Use <= for safety
    }

    public boolean isTimed() {
        return !(initialHh == 0 && initialMm == 0 && initialSs == 0);
    }

    public void decrementSecond() {
        if (isOutOfTime()) return; // Don't decrement if already out

        ss--;
        if (ss < 0) {
            ss = 59;
            mm--;
            if (mm < 0) {
                mm = 59;
                hh--;
                if (hh < 0) { // Should not happen with isOutOfTime check, but defensively...
                    hh = 0;
                    mm = 0;
                    ss = 0;
                }
            }
        }
    }

    public String getTimeString() {
        if (!isTimed()) {
            return "Untimed";
        }
        // Ensure non-negative values for display
        int displayH = Math.max(0, hh);
        int displayM = Math.max(0, mm);
        int displayS = Math.max(0, ss);

        return String.format("%02d:%02d:%02d", displayH, displayM, displayS);
    }
}