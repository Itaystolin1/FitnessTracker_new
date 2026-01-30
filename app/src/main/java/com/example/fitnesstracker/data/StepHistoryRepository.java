package com.example.fitnesstracker.data;

import com.example.fitnesstracker.data.model.DailyStats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class StepHistoryRepository {

    public interface HistoryListener {
        void onData(List<DailyStats> days);
    }

    public static void listenHistory(HistoryListener listener) {
        String uid = FirebaseAuth.getInstance().getUid();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("daily")
                .orderByKey()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<DailyStats> list = new ArrayList<>();

                        for (DataSnapshot day : snapshot.getChildren()) {
                            DailyStats stats = day.getValue(DailyStats.class);
                            if (stats != null) {
                                stats.date = day.getKey();
                                list.add(stats);
                            }
                        }
                        listener.onData(list);
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}
