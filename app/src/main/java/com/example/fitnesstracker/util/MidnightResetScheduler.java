package com.example.fitnesstracker.util;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MidnightResetScheduler {

    private static final String WORK_NAME = "midnight_step_reset";

    public static void schedule(Context context) {

        long initialDelay = millisUntilNextMidnight();

        // THE FIX: Use a precise OneTimeWorkRequest instead of Periodic!
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(MidnightResetWorker.class)
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .build();

        // REPLACE guarantees that if you open the app at 10 PM, it deletes the old confused timer
        // and sets a flawless new 2-hour countdown.
        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        request
                );
    }

    private static long millisUntilNextMidnight() {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();

        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Add one day to get exactly 00:00:00 tomorrow
        next.add(Calendar.DAY_OF_YEAR, 1);

        return next.getTimeInMillis() - now.getTimeInMillis();
    }
}