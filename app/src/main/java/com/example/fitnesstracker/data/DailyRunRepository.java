package com.example.fitnesstracker.data;

import com.example.fitnesstracker.data.model.RunSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DailyRunRepository {

    private static DatabaseReference ref() {
        String uid = FirebaseAuth.getInstance().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());

        return FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("daily")
                .child(date)
                .child("runs");
    }

    public static String startRun(RunSession run) {
        DatabaseReference r = ref().push();
        r.setValue(run);
        return r.getKey();
    }

    public static void updateRun(String runId, RunSession run) {
        if (runId == null) return;
        ref().child(runId).setValue(run);
    }

    public static void endRun(String runId, RunSession run) {
        updateRun(runId, run);
    }
}
