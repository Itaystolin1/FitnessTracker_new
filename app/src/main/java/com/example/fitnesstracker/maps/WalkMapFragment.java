package com.example.fitnesstracker.maps;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.fitnesstracker.service.StepTrackingService;
import com.example.fitnesstracker.util.StepPrefs;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class WalkMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Polyline currentPolyline;
    private boolean hasZoomedInitially = false;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepTrackingService.ACTION_UPDATE_STATS.equals(intent.getAction())) {
                drawDailyBreadcrumbs();
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        drawDailyBreadcrumbs();
    }

    private void drawDailyBreadcrumbs() {
        if (mMap == null || getContext() == null) return;

        // 1. Get the breadcrumb string from memory
        String savedPath = StepPrefs.getDailyPath(getContext());
        if (savedPath.isEmpty()) return;

        // 2. Decode the string back into GPS Coordinates
        ArrayList<LatLng> pathPoints = new ArrayList<>();
        String[] points = savedPath.split("\\|");
        for (String p : points) {
            String[] coords = p.split(",");
            if (coords.length == 2) {
                try {
                    pathPoints.add(new LatLng(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (pathPoints.isEmpty()) return;

        // 3. Remove the old line and draw the new thick red line
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        PolylineOptions options = new PolylineOptions()
                .addAll(pathPoints)
                .color(Color.parseColor("#FF3333")) // Thick Red Line!
                .width(12f)
                .geodesic(true);

        currentPolyline = mMap.addPolyline(options);

        // 4. Only auto-zoom once when the map opens, so the user can freely pan around!
        if (!hasZoomedInitially) {
            LatLng latestPoint = pathPoints.get(pathPoints.size() - 1);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestPoint, 15f));
            hasZoomedInitially = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(updateReceiver, new IntentFilter(StepTrackingService.ACTION_UPDATE_STATS));
        drawDailyBreadcrumbs();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateReceiver);
    }
}