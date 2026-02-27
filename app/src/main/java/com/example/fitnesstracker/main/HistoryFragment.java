package com.example.fitnesstracker.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.DayRecord;
import com.example.fitnesstracker.data.model.DaySummary;
import com.example.fitnesstracker.util.StepPrefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        // THE MAGIC: Force a live sync of today's background walk before loading!
        syncTodayWalkToFirebaseLive();
        loadHistory();
    }

    private void syncTodayWalkToFirebaseLive() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        // Grab the exact steps you've taken today directly from the local memory
        long liveSteps = StepPrefs.getSteps(requireContext());

        DatabaseReference summaryRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("history").child(todayDate).child("summary");

        // Calculate distance and calories based on the pure background walk
        float distance = liveSteps * 0.00075f;
        int calories = (int) (liveSteps * 0.04);

        DaySummary summary = new DaySummary(todayDate, (int) liveSteps, distance, calories);
        summaryRef.setValue(summary); // Boom! Firebase is instantly up to date.
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);

        // We now use your updated clean Repository!
        com.example.fitnesstracker.data.StepHistoryRepository.listenHistory(records -> {
            adapter.setRecords(records);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        });
    }
}