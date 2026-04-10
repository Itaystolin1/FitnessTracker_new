package com.example.fitnesstracker.data;

import com.example.fitnesstracker.data.model.DayRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StepHistoryRepository {

    public interface HistoryListener {
        void onData(List<DayRecord> days);
    }

    public static void listenHistory(HistoryListener listener) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("history")
                .orderByKey()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<DayRecord> list = new ArrayList<>();

                        for (DataSnapshot day : snapshot.getChildren()) {
                            DayRecord record = day.getValue(DayRecord.class);
                            if (record != null) {

                                // FIX: If they did a run but 0 background steps, create a dummy summary so the Date and 0s show up!
                                if (record.summary == null && record.runs != null && !record.runs.isEmpty()) {
                                    record.summary = new com.example.fitnesstracker.data.model.DaySummary(day.getKey(), 0, 0f, 0);
                                }

                                // FIX: Filter out completely empty days
                                boolean hasSteps = record.summary != null && record.summary.walkSteps > 0;
                                boolean hasRuns = record.runs != null && !record.runs.isEmpty();

                                if (hasSteps || hasRuns) {
                                    list.add(record);
                                }
                            }
                        }

                        Collections.reverse(list);
                        listener.onData(list);
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}