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

        // THE FIX: Pointing to the new "history" folder!
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
                            // Firebase magically parses the nested summary AND the list of runs!
                            DayRecord record = day.getValue(DayRecord.class);
                            if (record != null && record.summary != null) {
                                list.add(record);
                            }
                        }

                        // Show the newest days at the top
                        Collections.reverse(list);
                        listener.onData(list);
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}