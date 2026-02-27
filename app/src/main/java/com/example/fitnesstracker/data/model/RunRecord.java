package com.example.fitnesstracker.data.model;

public class RunRecord {
    public float distance;
    public int calories;
    public String pace;
    public String time;

    public RunRecord() {}

    public RunRecord(float distance, int calories, String pace, String time) {
        this.distance = distance;
        this.calories = calories;
        this.pace = pace;
        this.time = time;
    }
}