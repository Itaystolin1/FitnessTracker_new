package com.example.fitnesstracker.data.model;

import java.util.HashMap;
import java.util.Map;

public class DailyStats {
    // Changed to public to allow direct access from legacy Repositories
    public String date;
    public int totalSteps;
    public float totalDistance;
    public int totalCalories;
    public Map<String, RunSession> runSessions = new HashMap<>();

    public DailyStats() {
        // Default constructor required for Firebase
    }

    public DailyStats(String date, int totalSteps, float totalDistance, int totalCalories) {
        this.date = date;
        this.totalSteps = totalSteps;
        this.totalDistance = totalDistance;
        this.totalCalories = totalCalories;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public Map<String, RunSession> getRunSessions() {
        return runSessions;
    }

    public void setRunSessions(Map<String, RunSession> runSessions) {
        this.runSessions = runSessions;
    }
}