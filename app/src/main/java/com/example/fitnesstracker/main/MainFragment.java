package com.example.fitnesstracker.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.fitnesstracker.IntroActivity;
import com.example.fitnesstracker.MainActivity;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.service.StepTrackingService;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainFragment extends Fragment {

    private TextView tvStepCount, tvDistance, tvCalories, tvDate;
    private TextView tvGoalProgress;
    private android.widget.ProgressBar pbGoalProgress;
    private Button btnStartRun;
    private ImageButton btnLogout;
    private SwitchMaterial switchWalkTracking;

    private static final String PREF_TRACKER = "TrackerPrefs";
    private static final String KEY_IS_TRACKING = "TRACKING_ENABLED";

    private final BroadcastReceiver statsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra(StepTrackingService.EXTRA_MODE);
            if (mode != null && !mode.equals(MovementMode.WALK.name())) return;

            if (intent.hasExtra(StepTrackingService.EXTRA_STEPS)) {
                int steps = intent.getIntExtra(StepTrackingService.EXTRA_STEPS, 0);
                tvStepCount.setText(String.valueOf(steps));

                if (pbGoalProgress != null && tvGoalProgress != null) {
                    pbGoalProgress.setProgress(steps);
                    int percentage = (int) (((double) steps / 10000) * 100);
                    if (percentage > 100) percentage = 100;
                    tvGoalProgress.setText("Progress: " + percentage + "%");
                }
            }
            if (intent.hasExtra(StepTrackingService.EXTRA_DISTANCE)) {
                float dist = intent.getFloatExtra(StepTrackingService.EXTRA_DISTANCE, 0f);
                tvDistance.setText(String.format(Locale.US, "%.2f km", dist));
            }
            if (intent.hasExtra(StepTrackingService.EXTRA_CALORIES)) {
                int cals = intent.getIntExtra(StepTrackingService.EXTRA_CALORIES, 0);
                tvCalories.setText(String.format(Locale.US, "%d kcal", cals));
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvDate = view.findViewById(R.id.tvDate);
        tvStepCount = view.findViewById(R.id.tvStepCount);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvCalories = view.findViewById(R.id.tvCalories);

        tvGoalProgress = view.findViewById(R.id.tvGoalProgress);
        pbGoalProgress = view.findViewById(R.id.pbGoalProgress);

        btnStartRun = view.findViewById(R.id.btnStartRun);
        btnLogout = view.findViewById(R.id.btnLogout);
        switchWalkTracking = view.findViewById(R.id.switchWalkTracking);

        // SAFELY APPLY COLORS IN JAVA TO PREVENT APP CRASH!
        pbGoalProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#39FF14")));
        switchWalkTracking.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#39FF14")));
        switchWalkTracking.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#8039FF14")));

        String today = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(today.toUpperCase());

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_TRACKER, Context.MODE_PRIVATE);
        boolean isTrackingEnabled = prefs.getBoolean(KEY_IS_TRACKING, false);

        switchWalkTracking.setChecked(isTrackingEnabled);

        btnStartRun.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToActiveRun();
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), IntroActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        switchWalkTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_IS_TRACKING, isChecked).apply();

            Intent intent = new Intent(requireContext(), StepTrackingService.class);
            if (isChecked) {
                intent.setAction(StepTrackingService.ACTION_START_WALK);
                requireContext().startService(intent);
                Toast.makeText(getContext(), "Walk Tracking Resumed", Toast.LENGTH_SHORT).show();
            } else {
                intent.setAction(StepTrackingService.ACTION_STOP_TRACKING);
                requireContext().startService(intent);
                Toast.makeText(getContext(), "Walk Tracking Paused", Toast.LENGTH_SHORT).show();
            }
        });

        if (isTrackingEnabled) {
            startDailyTracking();
        }

        loadCurrentStats();

        // Load exactly the last 3 runs!
        loadRecentRuns();
    }

    private void loadCurrentStats() {
        long currentSteps = com.example.fitnesstracker.util.StepPrefs.getSteps(requireContext());
        float dist = currentSteps * 0.00075f;
        int cals = (int) (currentSteps * 0.04);

        tvStepCount.setText(String.valueOf(currentSteps));
        tvDistance.setText(String.format(Locale.US, "%.2f km", dist));
        tvCalories.setText(String.format(Locale.US, "%d kcal", cals));

        int goalMax = 10000;
        int progress = (int) currentSteps;
        int percentage = (int) (((double) progress / goalMax) * 100);

        if (percentage > 100) percentage = 100;

        pbGoalProgress.setProgress(progress);
        tvGoalProgress.setText("Progress: " + percentage + "%");
    }

    private void loadRecentRuns() {
        com.example.fitnesstracker.data.StepHistoryRepository.listenHistory(records -> {
            // Safely check context to avoid crashes
            if (!isAdded() || getContext() == null || getView() == null) return;

            android.widget.LinearLayout runContainer = getView().findViewById(R.id.activityScrollContainer);
            if (runContainer == null) return;

            runContainer.removeAllViews();

            int runCount = 0;

            for (com.example.fitnesstracker.data.model.DayRecord day : records) {
                if (day.runs != null && !day.runs.isEmpty()) {
                    for (com.example.fitnesstracker.data.model.RunRecord run : day.runs.values()) {

                        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_recent_run_card, runContainer, false);

                        TextView tvDist = cardView.findViewById(R.id.tvCardDist);
                        TextView tvTime = cardView.findViewById(R.id.tvCardTime);

                        tvDist.setText(String.format(Locale.US, "%.2f km", run.distance));
                        tvTime.setText(run.time + " min");

                        runContainer.addView(cardView);

                        runCount++;
                        if (runCount >= 5) break; // Break out if we hit 5 runs
                    }
                }
                if (runCount >= 5) break; // Break out of outer loop if we hit 5 runs
            }

            // If zero runs exist, load the empty state
            if (runCount == 0) {
                View emptyCard = LayoutInflater.from(getContext()).inflate(R.layout.item_recent_run_card, runContainer, false);
                TextView tvTitle = emptyCard.findViewById(R.id.tvCardTitle);
                TextView tvDist = emptyCard.findViewById(R.id.tvCardDist);
                TextView tvTime = emptyCard.findViewById(R.id.tvCardTime);

                tvTitle.setText("NO RUNS");
                tvTitle.setTextColor(Color.GRAY);
                tvDist.setText("-.- km");
                tvTime.setText("Go for a run!");

                runContainer.addView(emptyCard);
            }
        });
    }

    private void startDailyTracking() {
        Intent intent = new Intent(requireContext(), StepTrackingService.class);
        intent.setAction(StepTrackingService.ACTION_START_WALK);
        requireContext().startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(statsReceiver, new IntentFilter(StepTrackingService.ACTION_UPDATE_STATS));
        loadCurrentStats();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(statsReceiver);
    }
}