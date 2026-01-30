package com.example.fitnesstracker.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.DailyStats;
import com.example.fitnesstracker.data.model.RunSession;
import com.example.fitnesstracker.util.PolylineCodec;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.*;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private final List<DailyStats> items = new ArrayList<>();

    public void setData(List<DailyStats> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_day_stats, p, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        DailyStats d = items.get(i);

        h.tvDate.setText(d.date);
        h.tvStats.setText(
                "Steps: " + d.steps +
                        "\nDistance: " + String.format(Locale.getDefault(), "%.2f km", d.distanceKm) +
                        "\nCalories: " + String.format(Locale.getDefault(), "%.0f kcal", d.calories)
        );

        h.container.removeAllViews();

        // ===== RUN SESSIONS =====
        if (d.runs != null) {
            List<RunSession> runs = new ArrayList<>(d.runs.values());
            runs.sort((a, b) -> Long.compare(b.startMs, a.startMs));

            for (int idx = 0; idx < runs.size(); idx++) {
                RunSession r = runs.get(idx);

                TextView tv = new TextView(h.container.getContext());
                String duration = formatDuration(r.startMs, r.endMs);
                String pace = formatPace(r.startMs, r.endMs, r.distanceKm);

                tv.setText(
                        format(r.startMs) + " – " + format(r.endMs) +
                                "\nTime: " + duration +
                                "\nPace: " + pace +
                                "\n" + String.format(Locale.US, "%.2f km · %.0f kcal",
                                r.distanceKm, r.calories)
                );

                tv.setPadding(0, 8, 0, 8);
                h.container.addView(tv);

                // ➖ Divider between runs (not after last)
                if (idx < runs.size() - 1) {
                    View divider = new View(h.container.getContext());

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            4
                    );
                    lp.setMargins(0, 16, 0, 16);
                    divider.setLayoutParams(lp);

                    divider.setBackgroundColor(0xFF90A4AE); // darker gray
                    divider.setAlpha(0.9f);
                    h.container.addView(divider);
                }

            }

        }

        // ===== WALK MAP =====
        if (d.walkPolylines != null && !d.walkPolylines.isEmpty()) {
            h.bindMap(d.walkPolylines);
        } else {
            h.clearMap();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ================= VIEW HOLDER =================

    static class VH extends RecyclerView.ViewHolder implements OnMapReadyCallback {

        LinearLayout container;
        TextView tvDate, tvStats;
        FrameLayout mapContainer;

        private GoogleMap map;
        private SupportMapFragment mapFragment;
        private List<String> pendingPolylines;

        VH(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvStats = v.findViewById(R.id.tvStats);
            container = v.findViewById(R.id.container);
            mapContainer = v.findViewById(R.id.mapContainer);
        }

        void bindMap(List<String> polylines) {
            pendingPolylines = polylines;

            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance();
                ((FragmentActivity) itemView.getContext())
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(mapContainer.getId(), mapFragment)
                        .commitNow();
                mapFragment.getMapAsync(this);
            } else if (map != null) {
                drawRoutes();
            }
        }

        void clearMap() {
            if (map != null) map.clear();
        }

        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            map = googleMap;
            map.getUiSettings().setAllGesturesEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);
            drawRoutes();
        }

        private void drawRoutes() {
            if (map == null || pendingPolylines == null) return;

            map.clear();
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            boolean hasPoints = false;

            for (String encoded : pendingPolylines) {
                List<LatLng> pts = PolylineCodec.decode(encoded);
                if (pts.size() < 2) continue;

                hasPoints = true;
                map.addPolyline(
                        new PolylineOptions()
                                .color(0xFF1976D2)
                                .width(6f)
                                .addAll(pts)
                );
                for (LatLng p : pts) bounds.include(p);
            }

            if (hasPoints) {
                map.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds.build(), 40)
                );
            }
        }
    }

    // ================= TIME FORMAT =================

    private static String formatTime(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE)
        );
    }
    private static String formatDuration(long startMs, long endMs) {
        if (startMs <= 0 || endMs <= 0 || endMs <= startMs) return "--:--";

        long totalSec = (endMs - startMs) / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;

        return String.format(Locale.US, "%02d:%02d", min, sec);
    }
    private static String format(long ms) {
        if (ms <= 0) return "--:--";

        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());

        return sdf.format(new java.util.Date(ms));
    }
    private static String formatPace(long startMs, long endMs, float distanceKm) {
        if (distanceKm <= 0f || endMs <= startMs) return "--:-- / km";

        long totalSec = (endMs - startMs) / 1000;
        float secPerKm = totalSec / distanceKm;

        int min = (int) (secPerKm / 60);
        int sec = (int) (secPerKm % 60);

        return String.format(Locale.US, "%d:%02d / km", min, sec);
    }


}
