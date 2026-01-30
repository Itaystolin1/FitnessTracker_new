package com.example.fitnesstracker.data.model;

import java.util.List;
import java.util.Map;

public class DailyStats {

    public String date;
    public long steps;
    public float distanceKm;
    public float calories;

    // ✅ NEW — only for WALK days
    // Each string = encoded polyline of one walk session
    public List<String> walkPolylines;
    public Map<String, RunSession> runs;

    // Firebase required
    public DailyStats() {}

    public DailyStats(
            String date,
            long steps,
            float distanceKm,
            float calories,
            List<String> walkPolylines
    ) {
        this.date = date;
        this.steps = steps;
        this.distanceKm = distanceKm;
        this.calories = calories;
        this.walkPolylines = walkPolylines;
    }
}
