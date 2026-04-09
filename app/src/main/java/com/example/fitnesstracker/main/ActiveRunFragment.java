package com.example.fitnesstracker.main;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private View llRunControls;
    private Button btnPauseResume, btnFinish, btnExitRun;

    private boolean isPaused = false;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() == null) return;

            String mode = intent.getStringExtra(StepTrackingService.EXTRA_MODE);
            if (!MovementMode.RUN.name().equals(mode)) return;

            if (intent.hasExtra(StepTrackingService.EXTRA_ELAPSED_TIME)) {
                tvTimer.setText(intent.getStringExtra(StepTrackingService.EXTRA_ELAPSED_TIME));
            }
            if (intent.hasExtra(StepTrackingService.EXTRA_DISTANCE)) {
                float dist = intent.getFloatExtra(StepTrackingService.EXTRA_DISTANCE, 0f);
                tvDistance.setText(String.format(Locale.US, "%.2f km", dist));
            }
            if (intent.hasExtra(StepTrackingService.EXTRA_PACE)) {
                tvPace.setText(intent.getStringExtra(StepTrackingService.EXTRA_PACE));
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

        tvTimer = view.findViewById(R.id.tvTimer);
        tvDistance = view.findViewById(R.id.tvRunDistance);
        tvPace = view.findViewById(R.id.tvRunPace);

        llRunControls = view.findViewById(R.id.llRunControls);
        btnPauseResume = view.findViewById(R.id.btnPauseResume);
        btnFinish = view.findViewById(R.id.btnFinish);
        btnExitRun = view.findViewById(R.id.btnExitRun);

        if (getChildFragmentManager().findFragmentById(R.id.mapContainer) == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, new RunMapFragment())
                    .commit();
        }

        // Pause / Resume Logic
        btnPauseResume.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(requireContext(), StepTrackingService.class);
            if (isPaused) {
                serviceIntent.setAction(StepTrackingService.ACTION_RESUME_RUN);
                btnPauseResume.setText("PAUSE");
                isPaused = false;
            } else {
                serviceIntent.setAction(StepTrackingService.ACTION_PAUSE_RUN);
                btnPauseResume.setText("RESUME");
                isPaused = true;
            }
            requireContext().startService(serviceIntent);
        });

        // Finish Logic
        btnFinish.setOnClickListener(v -> showFinishWarning());

        // Exit Logic
        btnExitRun.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToDashboard();
            }
        });

        startRunSession();
    }

    private void showFinishWarning() {
        new AlertDialog.Builder(requireContext())
                .setTitle("End Run")
                .setMessage("Do you want to save this run to your history, or discard it entirely?")

                // Option 1: Save
                .setPositiveButton("Save", (dialog, which) -> {
                    endRun(true);
                })

                // Option 2: Delete/Discard (This uses a red/negative system theme usually)
                .setNegativeButton("Discard", (dialog, which) -> {
                    endRun(false);
                })

                // Option 3: Cancel and go back to the run
                .setNeutralButton("Keep Running", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void endRun(boolean saveRun) {
        // 1. Tell the service to Stop (and pass our Save/Discard choice!)
        Intent serviceIntent = new Intent(requireContext(), StepTrackingService.class);
        serviceIntent.setAction(StepTrackingService.ACTION_STOP_TRACKING);
        serviceIntent.putExtra(StepTrackingService.EXTRA_SAVE_RUN, saveRun);
        requireContext().startService(serviceIntent);

        // 2. Change the UI! Hide controls, show Exit button.
        llRunControls.setVisibility(View.GONE);
        btnExitRun.setVisibility(View.VISIBLE);

        // 3. Let the user know what happened
        if (saveRun) {
            android.widget.Toast.makeText(getContext(), "Run saved to history!", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(getContext(), "Run discarded.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void startRunSession() {
        Intent intent = new Intent(requireContext(), StepTrackingService.class);
        intent.setAction(StepTrackingService.ACTION_START_RUN);
        requireContext().startService(intent);
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