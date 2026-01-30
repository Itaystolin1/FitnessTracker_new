package com.example.fitnesstracker.main;

import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.service.StepTrackingService;

public class RunStatsFragment extends Fragment {

    private TextView tvTime, tvDistance, tvCalories;
    private BroadcastReceiver receiver;

    public RunStatsFragment() {
        super(R.layout.fragment_run_stats);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        tvTime = v.findViewById(R.id.tvTime);
        tvDistance = v.findViewById(R.id.tvDistance);
        tvCalories = v.findViewById(R.id.tvCalories);

        // RUN highlight: km primary
        tvDistance.setAlpha(1f);
        tvTime.setAlpha(0.85f);
        tvCalories.setAlpha(0.85f);
    }

    @Override
    public void onStart() {
        super.onStart();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if (!StepTrackingService.ACTION_UPDATE.equals(i.getAction())) return;

                String modeStr = i.getStringExtra(StepTrackingService.EXTRA_MODE);
                if (!MovementMode.RUN.name().equals(modeStr)) return;

                long timeMs = i.getLongExtra(StepTrackingService.EXTRA_TIME_MS, 0);
                float km = i.getFloatExtra(StepTrackingService.EXTRA_KM, 0f);
                float cal = i.getFloatExtra(StepTrackingService.EXTRA_CAL, 0f);

                tvTime.setText(formatDuration(timeMs));
                tvDistance.setText(String.format("%.2f km", km));
                tvCalories.setText(String.format("%.0f kcal", cal));

            }
        };

        IntentFilter filter = new IntentFilter(StepTrackingService.ACTION_UPDATE);

        if (Build.VERSION.SDK_INT >= 33) {
            requireContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(
                    requireContext(), receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (receiver != null) {
            requireContext().unregisterReceiver(receiver);
            receiver = null;
        }
    }

    private static String formatDuration(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;

        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}
