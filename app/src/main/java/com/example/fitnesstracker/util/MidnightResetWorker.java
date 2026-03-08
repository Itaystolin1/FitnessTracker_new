package com.example.fitnesstracker.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fitnesstracker.data.model.DaySummary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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

        // 1. Grab the final steps of the day BEFORE we delete them
        long finalSteps = StepPrefs.getSteps(context);

        // 2. Save to Firebase History
        String userId = FirebaseAuth.getInstance().getUid();

        if (userId != null && finalSteps > 0) {
            // Because this runs at 00:00, we must subtract 1 day to save it to the correct day!
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String recordDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

            // THE FIX: Point to the new "history" folder and the "summary" node!
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("history")
                    .child(recordDate)
                    .child("summary");

            float finalDistance = finalSteps * 0.00075f;
            int finalCalories = (int) (finalSteps * 0.04);

            // THE FIX: Use the new DaySummary model
            DaySummary finalStats = new DaySummary(
                    recordDate,
                    (int) finalSteps,
                    finalDistance,
                    finalCalories
            );

            // Push to cloud!
            ref.setValue(finalStats);
        }

        // 3. Now that it is safely in the cloud, wipe the local memory for the new day
        StepPrefs.hardResetForNewDay(context);
        WalkRouteStore.clear(context);

        return Result.success();
    }
}