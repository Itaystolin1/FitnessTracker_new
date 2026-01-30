package com.example.fitnesstracker.util;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public final class PolylineCodec {

    private PolylineCodec() {}

    // ===== ENCODE =====
    public static String encode(List<double[]> points) {
        List<LatLng> latLngs = new ArrayList<>(points.size());
        for (double[] p : points) {
            latLngs.add(new LatLng(p[0], p[1]));
        }
        return encodeLatLng(latLngs);
    }

    private static String encodeLatLng(List<LatLng> path) {
        StringBuilder result = new StringBuilder();
        long lastLat = 0;
        long lastLng = 0;

        for (LatLng point : path) {
            long lat = Math.round(point.latitude * 1e5);
            long lng = Math.round(point.longitude * 1e5);

            result.append(encodeDiff(lat - lastLat));
            result.append(encodeDiff(lng - lastLng));

            lastLat = lat;
            lastLng = lng;
        }
        return result.toString();
    }

    // ===== DECODE =====
    public static List<LatLng> decode(String encoded) {
        List<LatLng> path = new ArrayList<>();
        int index = 0;
        long lat = 0;
        long lng = 0;

        while (index < encoded.length()) {
            long[] r1 = decodeDiff(encoded, index);
            lat += r1[0];
            index = (int) r1[1];

            long[] r2 = decodeDiff(encoded, index);
            lng += r2[0];
            index = (int) r2[1];

            path.add(new LatLng(lat / 1e5, lng / 1e5));
        }
        return path;
    }

    private static long[] decodeDiff(String s, int start) {
        long result = 0;
        int shift = 0;
        int b;
        int index = start;

        do {
            b = s.charAt(index++) - 63;
            result |= (long) (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);

        long delta = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
        return new long[]{delta, index};
    }

    private static String encodeDiff(long diff) {
        diff = diff < 0 ? ~(diff << 1) : diff << 1;
        StringBuilder sb = new StringBuilder();
        while (diff >= 0x20) {
            sb.append((char) ((0x20 | (diff & 0x1f)) + 63));
            diff >>= 5;
        }
        sb.append((char) (diff + 63));
        return sb.toString();
    }
}
