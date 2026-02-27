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

    // THE FIX: Added a reset method to clear the memory for new runs!
    public void reset() {
        startTime = 0L;
        timeInMilliseconds = 0L;
        timeSwapBuff = 0L;
        updatedTime = 0L;
        isRunning = false;
    }

    public long getElapsedSeconds() {
        updateTime();
        return updatedTime / 1000;
    }

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