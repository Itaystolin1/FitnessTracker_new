package com.example.fitnesstracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepPrefs {
    private static final String KEY_DAILY_PATH = "daily_path";
    private static final String KEY_GOAL_STEPS = "goal_steps";
    private static final String KEY_GOAL_RUN = "goal_run_km";
    private static final String KEY_TODAY_RUN_DIST = "today_run_dist";
    private static final String PREF = "step_prefs";
    private static final String KEY_DATE = "date";
    private static final String KEY_BASELINE = "baseline";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_HEIGHT = "height_cm";
    private static final String KEY_WEIGHT = "weight_kg";
    private static final String KEY_GENDER = "gender"; // male / female

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
    }

    public static void ensureToday(Context c) {
        String saved = sp(c).getString(KEY_DATE, null);
        String today = today();

        if (!today.equals(saved)) {
            sp(c).edit()
                    .putString(KEY_DATE, today)
                    .putLong(KEY_BASELINE, -1)
                    .putLong(KEY_STEPS, 0)
                    .apply();
        }
    }
    // ===== DAILY BREADCRUMB PATH =====
    public static String getDailyPath(Context c) {
        return sp(c).getString(KEY_DAILY_PATH, "");
    }

    public static void addPathPoint(Context c, double lat, double lng) {
        String current = getDailyPath(c);
        String newPoint = lat + "," + lng;
        if (current.isEmpty()) {
            sp(c).edit().putString(KEY_DAILY_PATH, newPoint).apply();
        } else {
            // We use a "|" to separate the coordinates!
            sp(c).edit().putString(KEY_DAILY_PATH, current + "|" + newPoint).apply();
        }
    }
    // ===== DUAL GOALS =====
    public static int getStepGoal(Context c) {
        return sp(c).getInt(KEY_GOAL_STEPS, 10000); // Default 10,000
    }
    public static void setStepGoal(Context c, int goal) {
        sp(c).edit().putInt(KEY_GOAL_STEPS, goal).apply();
    }

    public static float getRunGoal(Context c) {
        return sp(c).getFloat(KEY_GOAL_RUN, 5.0f); // Default 5.0 km
    }
    public static void setRunGoal(Context c, float goal) {
        sp(c).edit().putFloat(KEY_GOAL_RUN, goal).apply();
    }

    // ===== TODAY'S RUN TOTAL =====
    public static float getTodayRunDistance(Context c) {
        return sp(c).getFloat(KEY_TODAY_RUN_DIST, 0f);
    }
    public static void addRunDistance(Context c, float distance) {
        float current = getTodayRunDistance(c);
        sp(c).edit().putFloat(KEY_TODAY_RUN_DIST, current + distance).apply();
    }
    public static long getBaseline(Context c) {
        return sp(c).getLong(KEY_BASELINE, -1);
    }

    public static void setBaseline(Context c, long v) {
        sp(c).edit().putLong(KEY_BASELINE, v).apply();
    }

    public static long getSteps(Context c) {
        return sp(c).getLong(KEY_STEPS, 0);
    }

    public static void setSteps(Context c, long v) {
        sp(c).edit().putLong(KEY_STEPS, v).apply();
    }

    // ===== PROFILE =====

    public static void saveProfile(Context c, float heightCm, float weightKg, String gender) {
        sp(c).edit()
                .putFloat(KEY_HEIGHT, heightCm)
                .putFloat(KEY_WEIGHT, weightKg)
                .putString(KEY_GENDER, gender)
                .apply();
    }

    public static float getHeightCm(Context c) {
        return sp(c).getFloat(KEY_HEIGHT, 170f);
    }

    public static float getWeightKg(Context c) {
        return sp(c).getFloat(KEY_WEIGHT, 70f);
    }

    public static String getGender(Context c) {
        return sp(c).getString(KEY_GENDER, "male");
    }

    public static void hardResetForNewDay(Context c) {
        sp(c).edit()
                .putString(KEY_DATE, today())
                .putLong(KEY_BASELINE, -1)
                .putLong(KEY_STEPS, 0)
                .apply();
        sp(c).edit().putString(KEY_DAILY_PATH, "").apply();
    }
}
