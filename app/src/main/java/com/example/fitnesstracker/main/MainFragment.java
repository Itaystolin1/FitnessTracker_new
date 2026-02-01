package com.example.fitnesstracker.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private Button btnStartRun;
    private ImageButton btnLogout;
    private SwitchMaterial switchWalkTracking;

    private final BroadcastReceiver statsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra(StepTrackingService.EXTRA_MODE);
            if (mode != null && !mode.equals(MovementMode.WALK.name())) return;

            if (intent.hasExtra(StepTrackingService.EXTRA_STEPS)) {
                int steps = intent.getIntExtra(StepTrackingService.EXTRA_STEPS, 0);
                tvStepCount.setText(String.valueOf(steps));
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
        btnStartRun = view.findViewById(R.id.btnStartRun);
        btnLogout = view.findViewById(R.id.btnLogout);
        switchWalkTracking = view.findViewById(R.id.switchWalkTracking);

        String today = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(today.toUpperCase());

        // Start Running Logic
        btnStartRun.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToActiveRun();
            }
        });

        // Logout Logic
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), IntroActivity.class);
            // Clear back stack so user can't press back to get in
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Walk Tracking Toggle Logic
        switchWalkTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
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

        // Ensure tracking is ON by default
        if (switchWalkTracking.isChecked()) {
            startDailyTracking();
        }
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
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(statsReceiver);
    }
}