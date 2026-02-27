package com.example.fitnesstracker.data.model;

public class DaySummary {
    public String date;
    public int walkSteps;
    public float walkDistance;
    public int walkCalories;

    public DaySummary() {}

    public DaySummary(String date, int walkSteps, float walkDistance, int walkCalories) {
        this.date = date;
        this.walkSteps = walkSteps;
        this.walkDistance = walkDistance;
        this.walkCalories = walkCalories;
    }
}