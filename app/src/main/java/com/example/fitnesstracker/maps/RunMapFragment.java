package com.example.fitnesstracker.maps;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.service.StepTrackingService;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

public class RunMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "TRACK_MAP";

    private GoogleMap map;
    private Polyline polyline;
    private final List<LatLng> route = new ArrayList<>();
    private BroadcastReceiver gpsReceiver;
    private boolean cameraCentered = false;

    public RunMapFragment() {
        super(R.layout.fragment_run_map);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        SupportMapFragment mapFragment = new SupportMapFragment();

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.map, mapFragment)
                .commitNow();

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            map.setMyLocationEnabled(true);

            LocationServices
                    .getFusedLocationProviderClient(requireContext())
                    .getLastLocation()
                    .addOnSuccessListener(loc -> {
                        if (loc != null && !cameraCentered) {
                            map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                            new LatLng(
                                                    loc.getLatitude(),
                                                    loc.getLongitude()
                                            ), 17f)
                            );
                            cameraCentered = true;
                        }
                    });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        gpsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (!StepTrackingService.ACTION_UPDATE.equals(intent.getAction()))
                    return;

                String modeStr =
                        intent.getStringExtra(StepTrackingService.EXTRA_MODE);

                if (modeStr == null) return;
                if (MovementMode.valueOf(modeStr) != MovementMode.RUN)
                    return;

                if (!intent.hasExtra(StepTrackingService.EXTRA_LAT))
                    return;

                double lat = intent.getDoubleExtra(
                        StepTrackingService.EXTRA_LAT, 0);
                double lng = intent.getDoubleExtra(
                        StepTrackingService.EXTRA_LNG, 0);

                LatLng point = new LatLng(lat, lng);
                route.add(point);

                if (map == null) return;

                if (!cameraCentered) {
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(point, 17f)
                    );
                    cameraCentered = true;
                }

                if (polyline == null) {
                    polyline = map.addPolyline(
                            new PolylineOptions()
                                    .color(Color.RED)
                                    .width(8f)
                                    .addAll(route)
                    );
                } else {
                    polyline.setPoints(route);
                }
            }
        };

        IntentFilter filter =
                new IntentFilter(StepTrackingService.ACTION_UPDATE);

        if (Build.VERSION.SDK_INT >= 33) {
            requireContext().registerReceiver(
                    gpsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(
                    requireContext(), gpsReceiver, filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (gpsReceiver != null) {
            requireContext().unregisterReceiver(gpsReceiver);
            gpsReceiver = null;
        }
    }
}
