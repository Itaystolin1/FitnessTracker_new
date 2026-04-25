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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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

    // THE FIX: Only the new Dual Goal variables are here!
    private TextView tvGoalStepsTitle, tvGoalStepsProgress, tvGoalRunTitle, tvGoalRunProgress;
    private android.widget.ProgressBar pbGoalSteps, pbGoalRun;
    private ImageButton btnEditGoalSteps, btnEditGoalRun;

    private Button btnStartRun;
    private ImageView ivDashboardAvatar;
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

                // --- UPDATE LIVE DUAL GOAL BAR ---
                if (pbGoalSteps != null && tvGoalStepsProgress != null) {
                    int stepGoal = com.example.fitnesstracker.util.StepPrefs.getStepGoal(requireContext());
                    pbGoalSteps.setProgress(steps);
                    int percentage = (int) (((double) steps / stepGoal) * 100);
                    if (percentage > 100) percentage = 100;
                    tvGoalStepsProgress.setText("Progress: " + percentage + "%");
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

        // BIND DUAL GOALS
        tvGoalStepsTitle = view.findViewById(R.id.tvGoalStepsTitle);
        tvGoalStepsProgress = view.findViewById(R.id.tvGoalStepsProgress);
        pbGoalSteps = view.findViewById(R.id.pbGoalSteps);
        btnEditGoalSteps = view.findViewById(R.id.btnEditGoalSteps);
        ivDashboardAvatar = view.findViewById(R.id.ivDashboardAvatar);

        tvGoalRunTitle = view.findViewById(R.id.tvGoalRunTitle);
        tvGoalRunProgress = view.findViewById(R.id.tvGoalRunProgress);
        pbGoalRun = view.findViewById(R.id.pbGoalRun);
        btnEditGoalRun = view.findViewById(R.id.btnEditGoalRun);

        btnStartRun = view.findViewById(R.id.btnStartRun);
        switchWalkTracking = view.findViewById(R.id.switchWalkTracking);

        // THE FIX: Safely apply colors only to the NEW progress bars!
        if (pbGoalSteps != null) pbGoalSteps.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#39FF14")));
        if (pbGoalRun != null) pbGoalRun.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#39FF14")));

        int[][] states = new int[][] {
                new int[] {-android.R.attr.state_checked}, // State: OFF
                new int[] {android.R.attr.state_checked}   // State: ON
        };

        int[] thumbColors = new int[] {
                Color.parseColor("#B0BEC5"), // Grey when OFF
                Color.parseColor("#39FF14")  // Neon when ON
        };

        int[] trackColors = new int[] {
                Color.parseColor("#455A64"), // Dark Grey when OFF
                Color.parseColor("#8039FF14") // Faded Neon when ON
        };

        switchWalkTracking.setThumbTintList(new ColorStateList(states, thumbColors));
        switchWalkTracking.setTrackTintList(new ColorStateList(states, trackColors));
        btnEditGoalSteps.setOnClickListener(v -> showEditStepsDialog());
        btnEditGoalRun.setOnClickListener(v -> showEditRunDialog());

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

        ivDashboardAvatar.setOnClickListener(v -> {
            ProfileDialogFragment dialog = new ProfileDialogFragment();
            dialog.show(getParentFragmentManager(), "ProfileDialog");
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
        loadRecentRuns();
    }

    private void loadCurrentStats() {
        if (getContext() == null) return;
        long currentSteps = com.example.fitnesstracker.util.StepPrefs.getSteps(requireContext());
        float dist = currentSteps * 0.00075f;
        int cals = (int) (currentSteps * 0.04);

        tvStepCount.setText(String.valueOf(currentSteps));
        tvDistance.setText(String.format(Locale.US, "%.2f km", dist));
        tvCalories.setText(String.format(Locale.US, "%d kcal", cals));

        // --- DUAL GOAL MATH ---
        if (pbGoalSteps != null && pbGoalRun != null) {
            int stepGoal = com.example.fitnesstracker.util.StepPrefs.getStepGoal(requireContext());
            tvGoalStepsTitle.setText(String.format(Locale.US, "%,d Steps", stepGoal));
            pbGoalSteps.setMax(stepGoal);
            int stepProgress = (int) currentSteps;
            int stepPercentage = (int) (((double) stepProgress / stepGoal) * 100);
            if (stepPercentage > 100) stepPercentage = 100;
            pbGoalSteps.setProgress(stepProgress);
            tvGoalStepsProgress.setText("Progress: " + stepPercentage + "%");

            float runGoal = com.example.fitnesstracker.util.StepPrefs.getRunGoal(requireContext());
            float currentRunDist = com.example.fitnesstracker.util.StepPrefs.getTodayRunDistance(requireContext());
            tvGoalRunTitle.setText(String.format(Locale.US, "%.1f km", runGoal));
            pbGoalRun.setMax((int) (runGoal * 1000)); // Multiply by 1000 to handle decimals smoothly!
            int runPercentage = (int) ((currentRunDist / runGoal) * 100);
            if (runPercentage > 100) runPercentage = 100;
            pbGoalRun.setProgress((int) (currentRunDist * 1000));
            tvGoalRunProgress.setText("Progress: " + runPercentage + "%");
        }
    }

    private void loadRecentRuns() {
        com.example.fitnesstracker.data.StepHistoryRepository.listenHistory(records -> {
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
                        if (runCount >= 5) break;
                    }
                }
                if (runCount >= 5) break;
            }

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

    private void showEditStepsDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_goal);

        // This makes the ugly default white background invisible so only your curved card shows!
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        android.widget.TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.EditText etInput = dialog.findViewById(R.id.etDialogInput);
        android.widget.TextView tvCancel = dialog.findViewById(R.id.tvDialogCancel);
        android.widget.TextView tvSave = dialog.findViewById(R.id.tvDialogSave);

        tvTitle.setText("Daily Step Goal");
        etInput.setHint("");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        tvCancel.setOnClickListener(v -> dialog.dismiss());
        tvSave.setOnClickListener(v -> {
            String val = etInput.getText().toString().trim();
            if (!val.isEmpty()) {
                com.example.fitnesstracker.util.StepPrefs.setStepGoal(requireContext(), Integer.parseInt(val));
                loadCurrentStats();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditRunDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_goal);

        // This makes the ugly default white background invisible so only your curved card shows!
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        android.widget.TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.EditText etInput = dialog.findViewById(R.id.etDialogInput);
        android.widget.TextView tvCancel = dialog.findViewById(R.id.tvDialogCancel);
        android.widget.TextView tvSave = dialog.findViewById(R.id.tvDialogSave);

        tvTitle.setText("Daily Running Goal");
        etInput.setHint("");
        // Allow decimals for running!
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        tvCancel.setOnClickListener(v -> dialog.dismiss());
        tvSave.setOnClickListener(v -> {
            String val = etInput.getText().toString().trim();
            if (!val.isEmpty()) {
                com.example.fitnesstracker.util.StepPrefs.setRunGoal(requireContext(), Float.parseFloat(val));
                loadCurrentStats();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(statsReceiver, new IntentFilter(StepTrackingService.ACTION_UPDATE_STATS));
        loadCurrentStats();

        String savedUri = com.example.fitnesstracker.util.StepPrefs.getProfilePicUri(requireContext());

        // POISON PILL FIX: Delete old expired temporary URIs
        if (savedUri.startsWith("content://")) {
            com.example.fitnesstracker.util.StepPrefs.setProfilePicUri(requireContext(), "");
            savedUri = "";
        }

        if (!savedUri.isEmpty() && ivDashboardAvatar != null) {
            java.io.File imgFile = new java.io.File(savedUri);
            if (imgFile.exists()) {
                ivDashboardAvatar.setImageURI(android.net.Uri.fromFile(imgFile));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(statsReceiver);
    }
}