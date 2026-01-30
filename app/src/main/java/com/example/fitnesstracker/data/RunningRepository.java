package com.example.fitnesstracker.data;

import com.example.fitnesstracker.data.model.RunSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class RunningRepository {

    private static RunSession currentRun;

    private static String uid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static void startRun() {
        currentRun = new RunSession();
        currentRun.startMs = System.currentTimeMillis();
    }

    public static void updateRun(float km, float calories) {
        if (currentRun == null) return;
        currentRun.distanceKm = km;
        currentRun.calories = calories;
    }

    public static void endRun() {
        if (currentRun == null) return;

        currentRun.endMs = System.currentTimeMillis();

        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid())
                .child("runs")
                .push()
                .setValue(currentRun);

        currentRun = null;
    }

    public static boolean isRunning() {
        return currentRun != null;
    }
}
