package com.example.fitnesstracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores WALK routes per day as multiple segments.
 * Each segment = one walking session (not connected).
 */
public final class WalkRouteStore {

    private static final String PREF = "walk_routes";
    private static final String KEY_SEGMENTS = "segments";

    private WalkRouteStore() {}

    // segments -> List< List< double[]{lat,lng} > >
    public static List<List<double[]>> load(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_SEGMENTS, null);

        List<List<double[]>> out = new ArrayList<>();
        if (raw == null) return out;

        try {
            JSONArray segs = new JSONArray(raw);
            for (int i = 0; i < segs.length(); i++) {
                JSONArray segArr = segs.getJSONArray(i);
                List<double[]> seg = new ArrayList<>();
                for (int j = 0; j < segArr.length(); j++) {
                    JSONArray p = segArr.getJSONArray(j);
                    seg.add(new double[]{p.getDouble(0), p.getDouble(1)});
                }
                out.add(seg);
            }
        } catch (JSONException e) {
            out.clear(); // corrupted → reset
        }
        return out;
    }

    public static void save(Context c, List<List<double[]>> segments) {
        JSONArray segs = new JSONArray();
        try {
            for (List<double[]> seg : segments) {
                JSONArray arr = new JSONArray();
                for (double[] p : seg) {
                    JSONArray pt = new JSONArray();
                    pt.put(p[0]);
                    pt.put(p[1]);
                    arr.put(pt);
                }
                segs.put(arr);
            }
        } catch (Exception ignored) {}

        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SEGMENTS, segs.toString())
                .apply();
    }

    /** Start a new walk segment (so paths do NOT connect) */
    public static void startNewSegment(Context c) {
        List<List<double[]>> segs = load(c);
        segs.add(new ArrayList<>());
        save(c, segs);
    }

    /** Append GPS point to current segment */
    public static void append(Context c, double lat, double lng) {
        List<List<double[]>> segs = load(c);
        if (segs.isEmpty()) segs.add(new ArrayList<>());
        segs.get(segs.size() - 1).add(new double[]{lat, lng});
        save(c, segs);
    }

    /** Clear everything (called at midnight) */
    public static void clear(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
