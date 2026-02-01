package com.example.fitnesstracker.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.fitnesstracker.MainActivity;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.data.model.MovementMode;
import com.example.fitnesstracker.util.DistanceEstimator;
import com.example.fitnesstracker.util.RunTimer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Locale;

public class StepTrackingService extends Service implements SensorEventListener {

    // --- CONSTANTS (Must match Fragments) ---
    public static final String ACTION_START_WALK = "ACTION_START_WALK";
    public static final String ACTION_START_RUN = "ACTION_START_RUN";
    public static final String ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING";
    public static final String ACTION_UPDATE_STATS = "ACTION_UPDATE_STATS";

    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_STEPS = "EXTRA_STEPS";
    public static final String EXTRA_DISTANCE = "EXTRA_DISTANCE";
    public static final String EXTRA_CALORIES = "EXTRA_CALORIES";
    public static final String EXTRA_ELAPSED_TIME = "EXTRA_ELAPSED_TIME";
    public static final String EXTRA_PACE = "EXTRA_PACE";

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FitnessTrackerChannel";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private RunTimer runTimer;

    private MovementMode currentMode = MovementMode.WALK;
    private int sessionSteps = 0;
    private float sessionDistance = 0f;
    private int sessionCalories = 0;
    private int baselineSteps = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        runTimer = new RunTimer();

        setupLocationCallback();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    // In a real app, we would add points to the route here
                    // For now, we rely on distance updates
                    broadcastUpdates();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_WALK:
                    startWalkMode();
                    break;
                case ACTION_START_RUN:
                    startRunMode();
                    break;
                case ACTION_STOP_TRACKING:
                    stopTracking();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startWalkMode() {
        currentMode = MovementMode.WALK;
        startForeground(NOTIFICATION_ID, getNotification("Tracking Walk"));
        registerSensors();
    }

    private void startRunMode() {
        currentMode = MovementMode.RUN;
        sessionSteps = 0;
        sessionDistance = 0f;
        sessionCalories = 0;
        baselineSteps = -1;

        runTimer.start();
        startForeground(NOTIFICATION_ID, getNotification("Tracking Run"));
        registerSensors();
        startLocationUpdates();

        // Timer updates every second
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (currentMode == MovementMode.RUN) {
                    broadcastUpdates();
                    new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
                }
            }
        });
    }

    private void stopTracking() {
        if (currentMode == MovementMode.RUN) {
            runTimer.stop();
        }
        unregisterSensors();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    private void registerSensors() {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void startLocationUpdates() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateIntervalMillis(2000)
                    .build();
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];
            if (baselineSteps == -1) {
                baselineSteps = totalSteps;
            }

            int newSteps = totalSteps - baselineSteps;
            // Simple delta check to avoid huge jumps on restart
            if (newSteps < 0) newSteps = 0;

            // Calculate delta since last update to add distance
            int stepDelta = newSteps - sessionSteps;
            if (stepDelta > 0) {
                sessionSteps = newSteps;
                // Rough estimate: 0.75m per step
                float addedDistance = stepDelta * 0.00075f;
                sessionDistance += addedDistance;
                // Rough estimate: 0.04 kcal per step
                sessionCalories += (int)(stepDelta * 0.04);

                broadcastUpdates();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void broadcastUpdates() {
        Intent intent = new Intent(ACTION_UPDATE_STATS);
        intent.putExtra(EXTRA_MODE, currentMode.name());
        intent.putExtra(EXTRA_STEPS, sessionSteps);
        intent.putExtra(EXTRA_DISTANCE, sessionDistance);
        intent.putExtra(EXTRA_CALORIES, sessionCalories);

        if (currentMode == MovementMode.RUN) {
            intent.putExtra(EXTRA_ELAPSED_TIME, runTimer.getFormattedTime());

            // Calculate Pace (min/km)
            if (sessionDistance > 0.01) {
                double totalMinutes = runTimer.getElapsedSeconds() / 60.0;
                double paceVal = totalMinutes / sessionDistance;
                int paceMin = (int) paceVal;
                int paceSec = (int) ((paceVal - paceMin) * 60);
                intent.putExtra(EXTRA_PACE, String.format(Locale.US, "%d:%02d /km", paceMin, paceSec));
            } else {
                intent.putExtra(EXTRA_PACE, "--:-- /km");
            }
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // Update notification
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(
                    currentMode == MovementMode.RUN ? runTimer.getFormattedTime() : sessionSteps + " steps"
            ));
        }
    }

    private Notification getNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentMode == MovementMode.RUN ? "Running Session" : "Walking Tracker")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fitness Tracker Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}