package com.example.fitnesstracker.util;

import android.os.Handler;
import android.os.Looper;

public class RunInactivityController {

    public interface Listener {
        void onInactivity();
    }

    private static final long TIMEOUT_MS = 15_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private long lastMovement = System.currentTimeMillis();

    private final Runnable check = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - lastMovement >= TIMEOUT_MS) {
                listener.onInactivity();
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    };

    public RunInactivityController(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        lastMovement = System.currentTimeMillis();
        handler.post(check);
    }

    public void stop() {
        handler.removeCallbacks(check);
    }

    public void markMovement() {
        lastMovement = System.currentTimeMillis();
    }
}
