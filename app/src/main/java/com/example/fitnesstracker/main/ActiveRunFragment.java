package com.example.fitnesstracker.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.fitnesstracker.MainActivity;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.maps.RunMapFragment;
import com.example.fitnesstracker.service.StepTrackingService;
import java.util.Locale;

public class ActiveRunFragment extends Fragment {

    private TextView tvTimer, tvDistance, tvPace;
    private ImageButton btnStop;

    // Broadcast Receiver for updates from Service
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() == null) return;

            // Only update if we are in RUN mode
            String mode = intent.getStringExtra(StepTrackingService.EXTRA_MODE);
            if (!MovementMode.RUN.name().equals(mode)) return;

            // Update Timer
            if (intent.hasExtra(StepTrackingService.EXTRA_ELAPSED_TIME)) {
                String time = intent.getStringExtra(StepTrackingService.EXTRA_ELAPSED_TIME);
                tvTimer.setText(time);
            }

            // Update Distance
            if (intent.hasExtra(StepTrackingService.EXTRA_DISTANCE)) {
                float dist = intent.getFloatExtra(StepTrackingService.EXTRA_DISTANCE, 0f);
                tvDistance.setText(String.format(Locale.US, "%.2f km", dist));
            }

            // Update Pace
            if (intent.hasExtra(StepTrackingService.EXTRA_PACE)) {
                String pace = intent.getStringExtra(StepTrackingService.EXTRA_PACE);
                tvPace.setText(pace);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_run_neon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views
        tvTimer = view.findViewById(R.id.tvTimer);
        tvDistance = view.findViewById(R.id.tvRunDistance);
        tvPace = view.findViewById(R.id.tvRunPace);
        btnStop = view.findViewById(R.id.btnStop);

        // 2. Embed the Map Fragment into the top container
        if (getChildFragmentManager().findFragmentById(R.id.mapContainer) == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, new RunMapFragment())
                    .commit();
        }

        // 3. Stop Button Logic
        btnStop.setOnClickListener(v -> {
            // Stop the service tracking
            Intent serviceIntent = new Intent(requireContext(), StepTrackingService.class);
            serviceIntent.setAction(StepTrackingService.ACTION_STOP_TRACKING);
            requireContext().startService(serviceIntent);

            // Navigate back to Dashboard
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToDashboard();
            }
        });

        // 4. Start the Run Service immediately
        startRunSession();
    }

    private void startRunSession() {
        Intent intent = new Intent(requireContext(), StepTrackingService.class);
        intent.setAction(StepTrackingService.ACTION_START_RUN);
        requireContext().startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Listen for updates
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(updateReceiver, new IntentFilter(StepTrackingService.ACTION_UPDATE_STATS));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateReceiver);
    }
}