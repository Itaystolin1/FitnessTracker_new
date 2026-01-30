package com.example.fitnesstracker.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
        StepPrefs.hardResetForNewDay(getApplicationContext());
        WalkRouteStore.clear(getApplicationContext());
        return Result.success();
    }

}
