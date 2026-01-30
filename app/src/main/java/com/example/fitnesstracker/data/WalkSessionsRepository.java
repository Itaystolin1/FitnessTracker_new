package com.example.fitnesstracker.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public final class WalkSessionsRepository {
    private WalkSessionsRepository() {}

    private static String uid() {
        return FirebaseAuth.getInstance().getUid();
    }

    private static DatabaseReference base() {
        return FirebaseDatabase.getInstance().getReference("users");
    }

    public static String startSession(String date, long startTimeMs) {
        String u = uid();
        if (u == null) return null;

        DatabaseReference ref = base()
                .child(u)
                .child("walk_sessions")
                .child(date)
                .push();

        String id = ref.getKey();
        if (id == null) return null;

        Map<String, Object> m = new HashMap<>();
        m.put("startTime", startTimeMs);
        m.put("endTime", 0L);
        m.put("encodedPath", "");
        m.put("points", 0);
        m.put("lastUpdated", ServerValue.TIMESTAMP);

        ref.setValue(m);
        return id;
    }

    public static void updateSession(String date, String sessionId,
                                     String encodedPath, int points, long nowMs) {
        String u = uid();
        if (u == null) return;

        DatabaseReference ref = base()
                .child(u)
                .child("walk_sessions")
                .child(date)
                .child(sessionId);

        Map<String, Object> m = new HashMap<>();
        m.put("encodedPath", encodedPath);
        m.put("points", points);
        m.put("lastUpdated", nowMs);

        ref.updateChildren(m);
    }

    public static void endSession(String date, String sessionId,
                                  String encodedPath, int points, long endTimeMs) {
        String u = uid();
        if (u == null) return;

        DatabaseReference ref = base()
                .child(u)
                .child("walk_sessions")
                .child(date)
                .child(sessionId);

        Map<String, Object> m = new HashMap<>();
        m.put("encodedPath", encodedPath);
        m.put("points", points);
        m.put("endTime", endTimeMs);
        m.put("lastUpdated", endTimeMs);

        ref.updateChildren(m);
    }

    public interface SessionsListener {
        void onData(DataSnapshot snapshot);
    }

    public static void listenDaySessions(String date, SessionsListener listener) {
        String u = uid();
        if (u == null) return;

        base().child(u)
                .child("walk_sessions")
                .child(date)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        listener.onData(snapshot);
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}
