package com.example.fitnesstracker;

import android.content.Intent;
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

        // Delay for 2 seconds to show the logo, then check auth
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, 2000);
    }

    private void checkAuthAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // User is logged in -> Go to Main Dashboard
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            // User is NOT logged in -> Go to new Intro/Onboarding Screen
            Intent intent = new Intent(SplashActivity.this, IntroActivity.class);
            startActivity(intent);
        }

        // Close SplashActivity so user can't go back to it
        finish();
    }
}