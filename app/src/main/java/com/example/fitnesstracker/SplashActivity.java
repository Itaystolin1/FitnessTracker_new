package com.example.fitnesstracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, 2000);
    }

    private void checkAuthAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences prefs = getSharedPreferences("AppSessionPrefs", MODE_PRIVATE);

        long lastActiveTime = prefs.getLong("last_active_time", 0);
        long currentTime = System.currentTimeMillis();

        // --- TIMEOUT CONFIGURATION ---
        // Change the 24 to a 6 if you really want a strict 6-hour timeout!
        long timeoutInMillis = 24 * 60 * 60 * 1000L;

        if (currentUser != null) {
            // Check if they exceeded the time gap
            if (lastActiveTime > 0 && (currentTime - lastActiveTime) > timeoutInMillis) {
                // Timeout! Log them out.
                FirebaseAuth.getInstance().signOut();
                prefs.edit().putLong("last_active_time", 0).apply();
                startActivity(new Intent(SplashActivity.this, IntroActivity.class));
            } else {
                // Still valid! Proceed to the app.
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
        } else {
            // Never logged in
            startActivity(new Intent(SplashActivity.this, IntroActivity.class));
        }

        finish();
    }
}