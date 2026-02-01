package com.example.fitnesstracker.util;

import android.os.SystemClock;
import java.util.Locale;

public class RunTimer {
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private boolean isRunning = false;

    public void start() {
        if (!isRunning) {
            startTime = SystemClock.uptimeMillis();
            isRunning = true;
        }
    }

    public void stop() {
        if (isRunning) {
            timeSwapBuff += timeInMilliseconds;
            isRunning = false;
        }
    }

    /**
     * Returns the total elapsed time in seconds.
     */
    public long getElapsedSeconds() {
        updateTime();
        return updatedTime / 1000;
    }

    /**
     * Returns the formatted time string (e.g., "00:05:30" or "05:30").
     */
    public String getFormattedTime() {
        updateTime();
        int secs = (int) (updatedTime / 1000);
        int mins = secs / 60;
        int hours = mins / 60;
        secs = secs % 60;
        mins = mins % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs);
        } else {
            return String.format(Locale.US, "%02d:%02d", mins, secs);
        }
    }

    private void updateTime() {
        if (isRunning) {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
        } else {
            updatedTime = timeSwapBuff;
        }
    }
}