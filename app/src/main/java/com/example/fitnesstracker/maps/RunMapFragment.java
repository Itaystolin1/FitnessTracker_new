package com.example.fitnesstracker.maps;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.service.StepTrackingService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class RunMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    // Receiver to listen for updates (even if just to verify service is running)
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // FIX: Using the correct constant ACTION_UPDATE_STATS
            if (StepTrackingService.ACTION_UPDATE_STATS.equals(intent.getAction())) {
                // If you later add location coordinates to the service broadcast,
                // you would extract them here to draw the line.
            }
        }
    };

    // 2. Paste this new dialog method right below it:
    private void showStopWarningDialog() {
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_run_stop_warning, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Ensure these IDs match the ones in your dialog_run_stop_warning.xml
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirmStop);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancelStop);

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                // THE FIX: Completely halt location updates immediately so numbers don't jump
                if (fusedLocationClient != null && locationCallback != null) {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }

                stopRunSession();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    // 3. Make sure your stopRunSession method clears everything properly:
    private void stopRunSession() {
        // Stop your RunTimer
        if (runTimer != null) {
            runTimer.stopTimer();
        }

        // Safety check: ensure location updates are dead
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // ... Any other code you have to save the run to Firebase goes here ...

        // Reset UI buttons
        btnStartRun.setVisibility(android.view.View.VISIBLE);
        btnStopRun.setVisibility(android.view.View.GONE);

        // Fix for Jumping Numbers: Reset the local distance variables for the next run
        totalDistance = 0f;
        // updateDistanceUI(0f); // Optional: if you have a method to reset the text on screen
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_run_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Show the blue "My Location" dot
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(updateReceiver, new IntentFilter(StepTrackingService.ACTION_UPDATE_STATS));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateReceiver);
    }
}