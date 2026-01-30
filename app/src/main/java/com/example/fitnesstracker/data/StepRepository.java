package com.example.fitnesstracker.data;

import android.content.Context;

import com.example.fitnesstracker.util.DistanceEstimator;
import com.example.fitnesstracker.util.StepPrefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepRepository {

    public interface StatsListener {
        void onStats(long steps, float km, float calories);
    }

    private static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static DatabaseReference dailyRef() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("daily").child(todayKey());
    }

    public static void listenStats(Context c, StatsListener l) {
        dailyRef().child("steps").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) {
                long steps = s.getValue(Long.class) == null ? 0 : s.getValue(Long.class);

                float h = StepPrefs.getHeightCm(c);
                float w = StepPrefs.getWeightKg(c);
                String g = StepPrefs.getGender(c);

                float km = DistanceEstimator.distanceKm(steps, h, g);
                float cal = DistanceEstimator.calories(steps, h, w, g);

                l.onStats(steps, km, cal);
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    public static void setBaselineIfMissing(long baseline) {
        dailyRef().child("baseline").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) {
                Long b = s.getValue(Long.class);
                if (b == null || b < 0) dailyRef().child("baseline").setValue(baseline);
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    public static void writeSteps(long steps, long lastSensor) {
        DatabaseReference ref = dailyRef();
        ref.child("steps").setValue(steps);
        ref.child("lastSensor").setValue(lastSensor);
        ref.child("updatedAt").setValue(System.currentTimeMillis());
    }

    public static void readBaseline(Callback<Long> cb) {
        dailyRef().child("baseline").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) {
                Long b = s.getValue(Long.class);
                cb.onResult(b == null ? -1L : b);
            }
            @Override public void onCancelled(DatabaseError error) {
                cb.onResult(-1L);
            }
        });
    }

    public interface Callback<T> { void onResult(T v); }
}
