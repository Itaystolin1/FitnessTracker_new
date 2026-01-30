package com.example.fitnesstracker.maps;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.service.StepTrackingService;
import com.example.fitnesstracker.util.WalkRouteStore;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

public class WalkMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private final List<Polyline> lines = new ArrayList<>();
    private BroadcastReceiver gpsReceiver;
    private boolean centered = false;

    public WalkMapFragment() {
        super(R.layout.fragment_run_map);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        SupportMapFragment mf = new SupportMapFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.map, mf)
                .commitNow();
        mf.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap g) {
        map = g;

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        redraw();
    }

    private void redraw() {
        if (map == null) return;

        for (Polyline p : lines) p.remove();
        lines.clear();

        List<List<double[]>> segs =
                WalkRouteStore.load(requireContext());

        for (List<double[]> seg : segs) {
            if (seg.size() < 2) continue;

            List<LatLng> pts = new ArrayList<>();
            for (double[] p : seg)
                pts.add(new LatLng(p[0], p[1]));

            lines.add(
                    map.addPolyline(
                            new PolylineOptions()
                                    .color(Color.BLUE)
                                    .width(7f)
                                    .addAll(pts)
                    )
            );
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        gpsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {

                if (!StepTrackingService.ACTION_UPDATE.equals(i.getAction()))
                    return;

                if (!MovementMode.WALK.name()
                        .equals(i.getStringExtra(
                                StepTrackingService.EXTRA_MODE)))
                    return;

                if (!i.hasExtra(StepTrackingService.EXTRA_LAT))
                    return;

                double lat = i.getDoubleExtra(
                        StepTrackingService.EXTRA_LAT, 0);
                double lng = i.getDoubleExtra(
                        StepTrackingService.EXTRA_LNG, 0);

                WalkRouteStore.append(requireContext(), lat, lng);
                redraw();

                if (!centered) {
                    map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lat, lng), 17f));
                    centered = true;
                }
            }
        };

        IntentFilter f =
                new IntentFilter(StepTrackingService.ACTION_UPDATE);

        if (Build.VERSION.SDK_INT >= 33) {
            requireContext().registerReceiver(
                    gpsReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(
                    requireContext(), gpsReceiver, f,
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
