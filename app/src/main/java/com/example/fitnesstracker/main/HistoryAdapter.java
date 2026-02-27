package com.example.fitnesstracker.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.DayRecord;
import com.example.fitnesstracker.data.model.RunRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<DayRecord> records = new ArrayList<>();
    private Context context;

    public HistoryAdapter(Context context) {
        this.context = context;
    }

    public void setRecords(List<DayRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_stats, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayRecord record = records.get(position);

        // 1. Set Background Walk Summary
        if (record.summary != null) {
            holder.tvDate.setText(record.summary.date);
            holder.tvSteps.setText(String.valueOf(record.summary.walkSteps));
            holder.tvDistance.setText(String.format(Locale.US, "%.2f km", record.summary.walkDistance));
            holder.tvCalories.setText(String.valueOf(record.summary.walkCalories));
        }

        // 2. Clear old runs from the container
        holder.runsContainer.removeAllViews();

        // 3. Dynamically add the Runs (if they took any runs that day)
        if (record.runs != null && !record.runs.isEmpty()) {
            for (RunRecord run : record.runs.values()) {
                View runView = LayoutInflater.from(context).inflate(R.layout.item_history_run, holder.runsContainer, false);

                TextView tvDist = runView.findViewById(R.id.tvRunDist);
                TextView tvTime = runView.findViewById(R.id.tvRunTime);
                TextView tvPace = runView.findViewById(R.id.tvRunPace);

                tvDist.setText(String.format(Locale.US, "🏃 %.2f km", run.distance));
                tvTime.setText("⏱ " + run.time);
                tvPace.setText("⚡ " + run.pace);

                holder.runsContainer.addView(runView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvSteps, tvDistance, tvCalories;
        LinearLayout runsContainer;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSteps = itemView.findViewById(R.id.tvSteps);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            runsContainer = itemView.findViewById(R.id.llRunsContainer); // The new container
        }
    }
}