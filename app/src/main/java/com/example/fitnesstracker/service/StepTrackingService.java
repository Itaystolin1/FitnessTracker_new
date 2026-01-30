package com.example.fitnesstracker.service;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.location.Location;
import android.os.*;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.DailyRunRepository;
import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.data.model.RunSession;
import com.example.fitnesstracker.util.DistanceEstimator;
import com.example.fitnesstracker.util.RunInactivityController;
import com.example.fitnesstracker.util.RunTimer;
import com.example.fitnesstracker.util.StepPrefs;

import com.google.android.gms.location.*;

public class StepTrackingService extends Service
        implements SensorEventListener, RunInactivityController.Listener {

    private static final String NOTIF_CH = "track";
    private static final int NOTIF_ID = 1;

    // ✅ FIX 3: MUST MATCH what your UI fragments filter on.
    // In your project earlier, receivers used StepTrackingService.ACTION_UPDATE.
    // If your UI still uses "com.example.fitnesstracker.UPDATE" then keep that.
    // If your UI changed to "TRACK_UPDATE" then also change the UI filters.
    public static final String ACTION_UPDATE = "com.example.fitnesstracker.UPDATE"; // ✅ recommended
    public static final String ACTION_STOP   = "TRACK_STOP";

    public static final String EXTRA_MODE   = "extra_mode";
    public static final String EXTRA_STEPS  = "extra_steps";
    public static final String EXTRA_KM     = "extra_km";
    public static final String EXTRA_CAL    = "extra_cal";
    public static final String EXTRA_TIME_MS = "extra_time_ms";
    public static final String EXTRA_INACTIVE = "inactive";
    public static final String EXTRA_LAT   = "extra_lat";
    public static final String EXTRA_LNG   = "extra_lng";

    private SensorManager sm;
    private Sensor stepSensor;

    private FusedLocationProviderClient gps;
    private LocationCallback gpsCb;

    private MovementMode mode = MovementMode.PAUSED_INVALID;

    // step sensor bookkeeping
    private long baseline = -1;
    private long lastSensorSteps = -1;
    private long lastTimeMs = -1;

    // WALK daily totals (optional, but keeps behavior stable)
    private long savedWalkSteps = 0;

    // RUN session totals
    private float runKm = 0f;
    private float runCal = 0f;
    private final RunTimer runTimer = new RunTimer();

    private RunSession run;
    private String runId;

    private RunInactivityController inactivity;

    @Override
    public void onCreate() {
        super.onCreate();

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        gps = LocationServices.getFusedLocationProviderClient(this);

        inactivity = new RunInactivityController(this);

        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("Tracking"));

        StepPrefs.ensureToday(this);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i == null) return START_STICKY;

        String action = i.getAction();

        if ("MODE_RUN".equals(action)) {
            startRun();
        } else if ("MODE_WALK".equals(action)) {
            startWalk();
        } else if (ACTION_STOP.equals(action)) {
            stopTrackingAndSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    private void startRun() {
        mode = MovementMode.RUN;

        // reset session counters
        runKm = 0f;
        runCal = 0f;

        resetStepState();

        runTimer.reset();
        runTimer.start();

        run = new RunSession();
        run.startMs = System.currentTimeMillis();
        run.distanceKm = 0f;
        run.calories = 0f;

        runId = DailyRunRepository.startRun(run);

        inactivity.start();

        registerStepSensor();
        startGps();
        // push initial state so UI shows 00:00 etc.
        broadcastFullState(false, null);
    }

    private void startWalk() {
        mode = MovementMode.WALK;

        StepPrefs.ensureToday(this);
        savedWalkSteps = StepPrefs.getSteps(this);

        // baseline continuation: keep baseline saved if exists
        baseline = StepPrefs.getBaseline(this);
        lastSensorSteps = -1;
        lastTimeMs = -1;

        inactivity.stop(); // only for run
        runTimer.stop();

        registerStepSensor();
        startGps();
        broadcastFullState(false, null);
    }

    private void stopTrackingAndSelf() {
        // notify UI "not tracking" (but do not break maps if you don’t want)
        mode = MovementMode.PAUSED_INVALID;
        broadcastFullState(false, null);

        stopGps();
        unregisterStepSensor();

        inactivity.stop();
        runTimer.stop();

        // close run if exists
        if (run != null) {
            run.endMs = System.currentTimeMillis();
            run.distanceKm = runKm;
            run.calories = runCal;
            DailyRunRepository.endRun(runId, run);
            run = null;
            runId = null;
        }

        stopForeground(true);
        stopSelf();
    }

    private void registerStepSensor() {
        if (stepSensor != null) {
            sm.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void unregisterStepSensor() {
        sm.unregisterListener(this);
    }

    private void resetStepState() {
        baseline = -1;
        lastSensorSteps = -1;
        lastTimeMs = -1;
        savedWalkSteps = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        long now = System.currentTimeMillis();
        long sensorSteps = (long) e.values[0];

        if (mode == MovementMode.RUN) {
            inactivity.markMovement();
        }

        StepPrefs.ensureToday(this);

        // init baseline
        if (baseline < 0) {
            if (mode == MovementMode.WALK && savedWalkSteps > 0) {
                baseline = sensorSteps - savedWalkSteps;
            } else {
                baseline = sensorSteps;
            }
            StepPrefs.setBaseline(this, baseline);

            lastSensorSteps = sensorSteps;
            lastTimeMs = now;

            // still broadcast so UI starts at 0
            broadcastFullState(false, null);
            return;
        }

        // compute delta
        long dSteps = (lastSensorSteps < 0) ? 0 : (sensorSteps - lastSensorSteps);
        long dTime = (lastTimeMs < 0) ? 0 : (now - lastTimeMs);

        if (dSteps < 0) dSteps = 0;

        float h = StepPrefs.getHeightCm(this);
        float w = StepPrefs.getWeightKg(this);
        String g = StepPrefs.getGender(this);

        if (mode == MovementMode.WALK) {
            long totalSteps = sensorSteps - baseline;
            if (totalSteps < 0) totalSteps = 0;

            StepPrefs.setSteps(this, totalSteps);

            // compute from total (stable)
            // IMPORTANT: these must always be broadcast (not -1)
            broadcastFullState(false, null);

        } else if (mode == MovementMode.RUN) {
            // use deltas for run increments
            if (dSteps > 0 && dTime > 0) {
                runKm += DistanceEstimator.distanceKm(dSteps, h, g);
                runCal += DistanceEstimator.calories(dSteps, h, w, g);

                if (run != null && runId != null) {
                    run.distanceKm = runKm;
                    run.calories = runCal;
                    DailyRunRepository.updateRun(runId, run);
                }
            }

            broadcastFullState(false, null);
        }

        lastSensorSteps = sensorSteps;
        lastTimeMs = now;
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}

    @Override
    public void onInactivity() {
        // pause timer only (run still exists until user stops)
        runTimer.stop();
        inactivity.stop();
        broadcastFullState(true, null);
    }

    // ===== GPS =====

    private void startGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest req = LocationRequest.create()
                .setInterval(3000)
                .setFastestInterval(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        gpsCb = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                for (Location loc : result.getLocations()) {
                    if (loc.getAccuracy() > 25) continue;
                    broadcastFullState(false, loc);
                }
            }
        };

        gps.requestLocationUpdates(req, gpsCb, Looper.getMainLooper());
    }

    private void stopGps() {
        if (gpsCb != null) {
            gps.removeLocationUpdates(gpsCb);
            gpsCb = null;
        }
    }

    // ===== BROADCAST (always full state) =====

    private void broadcastFullState(boolean inactive, @Nullable Location loc) {
        StepPrefs.ensureToday(this);

        long stepsOut = 0;
        float kmOut = 0f;
        float calOut = 0f;
        long timeOut = 0L;

        if (mode == MovementMode.WALK) {
            stepsOut = StepPrefs.getSteps(this);
            float h = StepPrefs.getHeightCm(this);
            float w = StepPrefs.getWeightKg(this);
            String g = StepPrefs.getGender(this);

            kmOut = DistanceEstimator.distanceKm(stepsOut, h, g);
            calOut = DistanceEstimator.calories(stepsOut, h, w, g);
            timeOut = 0L; // not used for walk
        } else if (mode == MovementMode.RUN) {
            stepsOut = 0; // not used in UI for run
            kmOut = runKm;
            calOut = runCal;
            timeOut = runTimer.elapsedMs();
        } else {
            // paused: keep zeros (UI can decide what to show)
            stepsOut = 0;
            kmOut = 0f;
            calOut = 0f;
            timeOut = 0L;
        }

        Intent out = new Intent(ACTION_UPDATE);
        out.setPackage(getPackageName());

        out.putExtra(EXTRA_MODE, mode.name());
        out.putExtra(EXTRA_STEPS, stepsOut);
        out.putExtra(EXTRA_KM, kmOut);
        out.putExtra(EXTRA_CAL, calOut);
        out.putExtra(EXTRA_TIME_MS, timeOut);
        out.putExtra(EXTRA_INACTIVE, inactive);

        if (loc != null) {
            out.putExtra(EXTRA_LAT, loc.getLatitude());
            out.putExtra(EXTRA_LNG, loc.getLongitude());
        }

        sendBroadcast(out);
    }

    // ===== Notification =====

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    NOTIF_CH, "Tracking", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, NOTIF_CH)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText(text)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        // ensure cleanup even if system kills
        stopGps();
        unregisterStepSensor();

        if (run != null) {
            run.endMs = System.currentTimeMillis();
            run.distanceKm = runKm;
            run.calories = runCal;
            DailyRunRepository.endRun(runId, run);
            run = null;
            runId = null;
        }

        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
