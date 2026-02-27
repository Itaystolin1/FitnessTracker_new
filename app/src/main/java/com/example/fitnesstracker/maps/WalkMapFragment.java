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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

// THE FIX: Change "Fragment" to "SupportMapFragment"
public class WalkMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Polyline currentPolyline;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepTrackingService.ACTION_UPDATE_STATS.equals(intent.getAction())) {

                // FIX: Extract the path points from the service
                ArrayList<LatLng> path = intent.getParcelableArrayListExtra(StepTrackingService.EXTRA_PATH);

                if (path != null && !path.isEmpty()) {
                    drawPathOnMap(path);
                }
            }
        }
    };

    private void drawPathOnMap(ArrayList<LatLng> path) {
        if (mMap == null) return;

        // Remove the old line before drawing the new updated one
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        // Draw the line (I set it to Neon Green to match your UI, width 12)
        PolylineOptions options = new PolylineOptions()
                .addAll(path)
                .color(Color.parseColor("#39FF14"))
                .width(12f)
                .geodesic(true);

        currentPolyline = mMap.addPolyline(options);

        // Optional: Make the camera automatically follow the user as they run!
        LatLng latestPoint = path.get(path.size() - 1);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestPoint, 17f));
    }@Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getMapAsync(this);
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