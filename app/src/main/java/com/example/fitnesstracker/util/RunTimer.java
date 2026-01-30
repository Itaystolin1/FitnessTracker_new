package com.example.fitnesstracker.util;

public final class RunTimer {

    private long startMs = -1;
    private boolean running = false;

    public void start() {
        startMs = System.currentTimeMillis();
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public long elapsedMs() {
        if (!running || startMs < 0) return 0L;
        return System.currentTimeMillis() - startMs;
    }

    public void reset() {
        startMs = -1;
        running = false;
    }
}
