package com.example.fitnesstracker.main;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.WalkSessionsRepository;
import com.example.fitnesstracker.util.PolylineCodec;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.List;

public class HistoryWalkMapFragment extends Fragment implements OnMapReadyCallback {

    public static final String ARG_DATE = "date";

    private GoogleMap map;

    public HistoryWalkMapFragment() {
        super(R.layout.fragment_run_map); // reuse your map layout container (@id/map)
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

        String date = getArguments() != null ? getArguments().getString(ARG_DATE) : null;
        if (date == null) return;

        WalkSessionsRepository.listenDaySessions(date, snapshot -> {
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            boolean hasAny = false;

            for (var sess : snapshot.getChildren()) {
                String encoded = String.valueOf(sess.child("encodedPath").getValue());
                if (encoded == null || encoded.isEmpty() || "null".equals(encoded)) continue;

                List<LatLng> pts = PolylineCodec.decode(encoded);
                PolylineOptions opts = new PolylineOptions().width(7f);

                map.addPolyline(
                        new PolylineOptions()
                                .addAll(pts)
                                .width(6f)
                                .color(Color.BLUE)
                );
                map.addPolyline(opts);
            }

            if (hasAny) {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 60));
            }
        });
    }
}
