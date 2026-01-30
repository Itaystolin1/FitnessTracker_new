package com.example.fitnesstracker;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.example.fitnesstracker.util.MidnightResetScheduler;


public class MainActivity extends AppCompatActivity {

    private static final String PREF = "session_pref";
    private static final String KEY_BG_TIME = "bg_time_ms";
    private static final long AUTO_LOGOUT_MS = 30L * 60L * 1000L; // 30 minutes

    private SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences(PREF, MODE_PRIVATE);

        // ✅ REQUIRED — schedules daily reset
        MidnightResetScheduler.schedule(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        sp.edit().putLong(KEY_BG_TIME, System.currentTimeMillis()).apply();
    }

    @Override
    protected void onStart() {
        super.onStart();

        long lastBg = sp.getLong(KEY_BG_TIME, -1);
        if (lastBg > 0) {
            long delta = System.currentTimeMillis() - lastBg;
            if (delta >= AUTO_LOGOUT_MS && FirebaseAuth.getInstance().getCurrentUser() != null) {
                FirebaseAuth.getInstance().signOut();
                forceGoToSplash();
            }
        }
    }

    private void forceGoToSplash() {
        NavHostFragment navHost =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHost == null) return;
        NavController nav = navHost.getNavController();
        nav.popBackStack(R.id.splashFragment, false);
    }
}
