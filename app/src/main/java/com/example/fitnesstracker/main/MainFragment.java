package com.example.fitnesstracker.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.CheckBox;

import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.util.RunStopWarningPrefs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.maps.RunMapFragment;
import com.example.fitnesstracker.maps.WalkMapFragment;
import com.example.fitnesstracker.service.StepTrackingService;

public class MainFragment extends Fragment {

    private static final int REQ_LOCATION = 1001;
    private static final int REQ_ACTIVITY = 1002;

    private TextView tvStatus, tvTrackingInfo;
    private FrameLayout mapContainer;
    private FrameLayout statsContainer;
    private MovementMode currentMode = MovementMode.PAUSED_INVALID;
    private Button btnWalk, btnRun, btnStop, btnHistory;

    public MainFragment() {
        super(R.layout.fragment_main);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        tvStatus = v.findViewById(R.id.tvStatus);
        tvTrackingInfo = v.findViewById(R.id.tvTrackingInfo);

        mapContainer = v.findViewById(R.id.mapContainer);
        statsContainer = v.findViewById(R.id.statsContainer);

        btnWalk = v.findViewById(R.id.btnWalk);
        btnRun  = v.findViewById(R.id.btnRun);
        btnStop = v.findViewById(R.id.btnStop);

        btnWalk.setOnClickListener(x -> startWalk());
        btnRun.setOnClickListener(x -> startRun());
        btnStop.setOnClickListener(x -> stopTracking());
        btnHistory = v.findViewById(R.id.btnHistory);

        btnHistory.setOnClickListener(v1 ->
                androidx.navigation.Navigation
                        .findNavController(v1)
                        .navigate(R.id.action_mainFragment_to_historyFragment)
        );


        updateButtons(); // initial state

    }

    // ================= PERMISSION =================

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_LOCATION
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            // We intentionally do NOT auto-start tracking.
            // User must press Start again → predictable UX.
        }
    }

    // ================= WALK =================

    private void startWalk() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        currentMode = MovementMode.WALK;

        Intent i = new Intent(requireContext(), StepTrackingService.class);
        i.setAction("MODE_WALK");
        requireContext().startService(i);

        tvStatus.setText("Tracking your walk");
        tvTrackingInfo.setText("Steps, distance and calories");

        showMap(new WalkMapFragment());
        showStats(new WalkStatsFragment());

        updateButtons();
    }

    // ================= RUN =================

    private void startRun() {
        if (!hasActivityPermission()) {
            requestActivityPermission();
            return;
        }
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        currentMode = MovementMode.RUN;

        Intent i = new Intent(requireContext(), StepTrackingService.class);
        i.setAction("MODE_RUN");
        requireContext().startService(i);

        tvStatus.setText("Tracking your run");
        tvTrackingInfo.setText("Time, distance and calories");

        showMap(new RunMapFragment());
        showStats(new RunStatsFragment());

        updateButtons();
    }

    // ================= STOP =================

    private void stopTracking() {
        if (!hasActivityPermission()) {
            requestActivityPermission();
            return;
        }

        // Only warn for RUN
        if (currentMode == MovementMode.RUN &&
                RunStopWarningPrefs.shouldShow(requireContext())) {

            showRunStopWarning();
            return;
        }

        // Immediate stop (walk OR warning disabled)
        doStop();
    }


    // ================= UI HELPERS =================

    private void showMap(Fragment f) {
        mapContainer.setVisibility(View.VISIBLE);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, f)
                .commit();
    }

    private void showStats(Fragment f) {
        statsContainer.setVisibility(View.VISIBLE);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.statsContainer, f)
                .commit();
    }
    private void showRunStopWarning() {

        View v = View.inflate(requireContext(), R.layout.dialog_run_stop_warning, null);
        CheckBox cbNever = v.findViewById(R.id.cbNever);

        new AlertDialog.Builder(requireContext())
                .setTitle("End run?")
                .setView(v)
                .setCancelable(false)

                .setPositiveButton("Stop run", (d, w) -> {
                    if (cbNever.isChecked()) {
                        RunStopWarningPrefs.disable(requireContext());
                    }
                    doStop();
                })

                .setNegativeButton("Continue run", (d, w) -> {
                    d.dismiss(); // nothing stops, run continues
                })

                .show();
    }
    private void doStop() {
        Intent i = new Intent(requireContext(), StepTrackingService.class);
        i.setAction(StepTrackingService.ACTION_STOP);
        requireContext().startService(i);

        currentMode = MovementMode.PAUSED_INVALID;

        tvStatus.setText("Not tracking");
        tvTrackingInfo.setText("Nothing is being tracked");

        updateButtons();
    }

    private void updateButtons() {
        boolean tracking = currentMode != MovementMode.PAUSED_INVALID;

        btnWalk.setEnabled(!tracking);
        btnRun.setEnabled(!tracking);
        btnStop.setEnabled(tracking);
    }
    private boolean hasActivityPermission() {
        if (Build.VERSION.SDK_INT < 29) return true;
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestActivityPermission() {
        if (Build.VERSION.SDK_INT >= 29) {
            requestPermissions(
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    REQ_ACTIVITY
            );
        }
    }


}
