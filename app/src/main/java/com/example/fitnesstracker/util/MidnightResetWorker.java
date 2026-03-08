package com.example.fitnesstracker.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fitnesstracker.data.model.DaySummary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MidnightResetWorker extends Worker {

    public MidnightResetWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 1. Grab the final steps of the day
        long finalSteps = StepPrefs.getSteps(context);
        String userId = FirebaseAuth.getInstance().getUid();

        // THE FIX: Get the exact Date string directly from memory! No more timezone guessing.
        String recordDate = context.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
                .getString("date", null);

        if (userId != null && finalSteps > 0 && recordDate != null) {

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("history")
                    .child(recordDate)
                    .child("summary");

            float finalDistance = finalSteps * 0.00075f;
            int finalCalories = (int) (finalSteps * 0.04);

            DaySummary finalStats = new DaySummary(
                    recordDate,
                    (int) finalSteps,
                    finalDistance,
                    finalCalories
            );

            // Push to cloud!
            ref.setValue(finalStats);
        }

        // 3. Wipe local memory for the new day
        StepPrefs.hardResetForNewDay(context);
        WalkRouteStore.clear(context);

        // THE FIX: Because we use OneTimeWorkRequest now, we must ask the scheduler to set the timer for tomorrow!
        MidnightResetScheduler.schedule(context);

        return Result.success();
    }
}