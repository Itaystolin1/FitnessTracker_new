package com.example.fitnesstracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepPrefs {

    private static final String PREF = "step_prefs";

    private static final String KEY_DATE = "date";
    private static final String KEY_BASELINE = "baseline";
    private static final String KEY_STEPS = "steps";

    // Profile
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
    }
}
