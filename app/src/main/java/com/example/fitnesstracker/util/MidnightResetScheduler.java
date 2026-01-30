package com.example.fitnesstracker.util;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MidnightResetScheduler {

    private static final String WORK_NAME = "midnight_step_reset";

    public static void schedule(Context context) {

        long initialDelay = millisUntilNextMidnight();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        MidnightResetWorker.class,
                        24, TimeUnit.HOURS
                )
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
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
        next.add(Calendar.DAY_OF_YEAR, 1);

        return next.getTimeInMillis() - now.getTimeInMillis();
    }
}
