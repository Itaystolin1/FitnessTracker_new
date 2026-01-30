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

public class WalkStatsFragment extends Fragment {

    private TextView tvSteps, tvDistance, tvCalories;
    private BroadcastReceiver receiver;

    public WalkStatsFragment() {
        super(R.layout.fragment_walk_stats);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        tvSteps = v.findViewById(R.id.tvSteps);
        tvDistance = v.findViewById(R.id.tvDistance);
        tvCalories = v.findViewById(R.id.tvCalories);

        // WALK highlight: steps primary
        tvSteps.setAlpha(1f);
        tvDistance.setAlpha(0.85f);
        tvCalories.setAlpha(0.85f);
    }

    @Override
    public void onStart() {
        super.onStart();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                long steps = i.getLongExtra(StepTrackingService.EXTRA_STEPS, 0);
                float km = i.getFloatExtra(StepTrackingService.EXTRA_KM, 0f);
                float cal = i.getFloatExtra(StepTrackingService.EXTRA_CAL, 0f);

                tvSteps.setText(steps + " steps");
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
}
