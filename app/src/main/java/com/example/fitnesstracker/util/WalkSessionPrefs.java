package com.example.fitnesstracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class WalkSessionPrefs {
    private static final String PREF = "walk_session_prefs";
    private static final String KEY_DATE = "date";
    private static final String KEY_SESSION_ID = "session_id";

    private WalkSessionPrefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static void setCurrent(Context c, String date, String sessionId) {
        sp(c).edit().putString(KEY_DATE, date).putString(KEY_SESSION_ID, sessionId).apply();
    }

    public static String getDate(Context c) {
        return sp(c).getString(KEY_DATE, null);
    }

    public static String getSessionId(Context c) {
        return sp(c).getString(KEY_SESSION_ID, null);
    }

    public static void clear(Context c) {
        sp(c).edit().clear().apply();
    }
}
