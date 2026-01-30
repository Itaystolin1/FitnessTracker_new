package com.example.fitnesstracker.util;

public class DistanceEstimator {

    private DistanceEstimator() {}

    public static float stepLengthMeters(float heightCm, String gender) {
        float heightM = heightCm / 100f;

        if ("female".equalsIgnoreCase(gender)) {
            return heightM * 0.413f;
        }
        return heightM * 0.415f; // male default
    }

    public static float distanceKm(long steps, float heightCm, String gender) {
        float stepLen = stepLengthMeters(heightCm, gender);
        return (steps * stepLen) / 1000f;
    }

    public static float calories(
            long steps,
            float heightCm,
            float weightKg,
            String gender
    ) {
        float stepLen = stepLengthMeters(heightCm, gender);
        return steps * stepLen * weightKg * 0.0005f;
    }
}
