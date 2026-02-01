package com.example.fitnesstracker.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.DailyStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<DailyStats> statsList = new ArrayList<>();

    public void setStatsList(List<DailyStats> statsList) {
        this.statsList = statsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_stats, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyStats stats = statsList.get(position);
        holder.tvDate.setText(stats.getDate());
        holder.tvSteps.setText(String.valueOf(stats.getTotalSteps()));
        holder.tvDistance.setText(String.format(Locale.US, "%.2f km", stats.getTotalDistance()));
        holder.tvCalories.setText(String.format(Locale.US, "%d", stats.getTotalCalories()));
    }

    @Override
    public int getItemCount() {
        return statsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvSteps, tvDistance, tvCalories;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSteps = itemView.findViewById(R.id.tvSteps);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvCalories = itemView.findViewById(R.id.tvCalories);
        }
    }
}