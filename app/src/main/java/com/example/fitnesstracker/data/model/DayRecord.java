package com.example.fitnesstracker.data.model;
import java.util.HashMap;

public class DayRecord {
    public DaySummary summary;
    public HashMap<String, RunRecord> runs;

    public DayRecord() {} // Required for Firebase
}