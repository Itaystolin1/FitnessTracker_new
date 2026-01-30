package com.example.fitnesstracker.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.fitnesstracker.R;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.Locale;

public class WorkoutFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_REQ_CODE = 1001;

    private GoogleMap map;
    private FusedLocationProviderClient locationClient;

    private Location lastLocation;
    private float totalDistanceMeters = 0f;

    private TextView tvDistance;
    private Polyline polyline;
    private PolylineOptions polylineOptions = new PolylineOptions().width(8f);

    public WorkoutFragment() {
        super(R.layout.fragment_workout);
    }

    // --------------------------------------------------
    // LIFECYCLE
    // --------------------------------------------------

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        tvDistance = v.findViewById(R.id.tvDistance);
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // --------------------------------------------------
    // MAP READY
    // --------------------------------------------------

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // STEP 3: check permissions
        if (!hasLocationPermission()) {
            // STEP 2: request permissions
            requestLocationPermission();
            return;
        }

        enableLocationAndStartTracking();
    }

    // --------------------------------------------------
    // PERMISSION HELPERS
    // --------------------------------------------------

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_REQ_CODE
        );
    }

    // --------------------------------------------------
    // PERMISSION RESULT
    // --------------------------------------------------

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQ_CODE) {
            if (hasLocationPermission()) {
                enableLocationAndStartTracking();
            }
            // else: user denied → do nothing (map stays static)
        }
    }

    // --------------------------------------------------
    // LOCATION TRACKING
    // --------------------------------------------------

    @SuppressWarnings("MissingPermission")
    private void enableLocationAndStartTracking() {

        map.setMyLocationEnabled(true);
        startLocationUpdates();
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationClient.requestLocationUpdates(
                request,
                locationCallback,
                null
        );
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {

            Location loc = result.getLastLocation();
            if (loc == null || map == null) return;

            LatLng point = new LatLng(loc.getLatitude(), loc.getLongitude());

            if (lastLocation != null) {
                totalDistanceMeters += lastLocation.distanceTo(loc);
                tvDistance.setText(
                        String.format(
                                Locale.US,
                                "%.2f km",
                                totalDistanceMeters / 1000f
                        )
                );
            }

            lastLocation = loc;

            if (polyline == null) {
                polylineOptions.add(point);
                polyline = map.addPolyline(polylineOptions);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
            } else {
                polylineOptions.add(point);
                polyline.setPoints(polylineOptions.getPoints());
            }
        }
    };

    // --------------------------------------------------
    // CLEANUP
    // --------------------------------------------------

    @Override
    public void onStop() {
        super.onStop();
        locationClient.removeLocationUpdates(locationCallback);
    }
}
