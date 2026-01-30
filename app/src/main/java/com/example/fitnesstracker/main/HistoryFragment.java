package com.example.fitnesstracker.main;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.StepHistoryRepository;

public class HistoryFragment extends Fragment {

    private HistoryAdapter adapter;

    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new HistoryAdapter();
        rv.setAdapter(adapter);

        StepHistoryRepository.listenHistory(adapter::setData);
    }
}
