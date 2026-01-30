package com.example.fitnesstracker.data.model;

public class RunSession {
    public long startMs;
    public long endMs;
    public float distanceKm;
    public float calories;

    public RunSession() {}

    public RunSession(long startMs) {
        this.startMs = startMs;
    }
}
