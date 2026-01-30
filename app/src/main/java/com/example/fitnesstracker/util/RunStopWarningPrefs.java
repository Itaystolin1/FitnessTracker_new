package com.example.fitnesstracker.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class RunStopWarningPrefs {

    private static final String PREF = "run_warning_prefs";
    private static final String KEY_SKIP = "skip_run_stop_warning";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean shouldShow(Context c) {
        return !sp(c).getBoolean(KEY_SKIP, false);
    }

    public static void disable(Context c) {
        sp(c).edit().putBoolean(KEY_SKIP, true).apply();
    }
}
