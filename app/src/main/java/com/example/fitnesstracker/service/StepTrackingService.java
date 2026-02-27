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
import com.example.fitnesstracker.util.StepPrefs;
import com.example.fitnesstracker.util.RunTimer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;


public class StepTrackingService extends Service implements SensorEventListener {

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
    public static final String EXTRA_PATH = "EXTRA_PATH";

    private ArrayList<LatLng> currentPath = new ArrayList<>();
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FitnessTrackerChannel";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private RunTimer runTimer;

    private MovementMode currentMode = MovementMode.WALK;

    // --- DAILY TRACKING VARIABLES ---
    private long dailyBaseline = -1;
    private long dailySteps = 0;

    // --- RUN SESSION VARIABLES ---
    private long runSessionBaseline = -1;
    private int runSessionSteps = 0;
    private float runSessionDistance = 0f;
    private int runSessionCalories = 0;

    // THE FIX: Create a global Handler and Runnable we can control and kill!
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

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

        StepPrefs.ensureToday(this);
        dailyBaseline = StepPrefs.getBaseline(this);
        dailySteps = StepPrefs.getSteps(this);

        // THE FIX: Define the loop here, but don't start it yet
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentMode == MovementMode.RUN) {
                    broadcastUpdates();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    currentPath.add(new LatLng(location.getLatitude(), location.getLongitude()));
                }
                broadcastUpdates();
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
        currentPath.clear();

        // THE FIX: Kill any leftover run loop when starting walk mode
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void startRunMode() {
        currentMode = MovementMode.RUN;

        runSessionBaseline = -1;
        runSessionSteps = 0;
        runSessionDistance = 0f;
        runSessionCalories = 0;

        if (currentPath != null) {
            currentPath.clear();
        }
        if (runTimer != null) {
            runTimer.reset();
        }

        runTimer.start();
        startForeground(NOTIFICATION_ID, getNotification("Tracking Run"));
        registerSensors();
        startLocationUpdates();

        // THE FIX: Kill any zombie loops, then start exactly ONE clean loop
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);
    }

    private void stopTracking() {
        if (currentMode == MovementMode.RUN) {
            runTimer.stop();

            // --- SAVE ONLY THE RUN TO HISTORY ---
            String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (userId != null) {
                String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());

                // Point to today's "runs" folder
                com.google.firebase.database.DatabaseReference runRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("users").child(userId).child("history").child(todayDate).child("runs").push();

                // Calculate Pace for the save file
                String paceStr = "--:-- /km";
                if (runSessionDistance > 0.01) {
                    double totalMinutes = runTimer.getElapsedSeconds() / 60.0;
                    double paceVal = totalMinutes / runSessionDistance;
                    int paceMin = (int) paceVal;
                    int paceSec = (int) ((paceVal - paceMin) * 60);
                    paceStr = String.format(java.util.Locale.US, "%d:%02d /km", paceMin, paceSec);
                }

                // Push the RunRecord to Firebase
                com.example.fitnesstracker.data.model.RunRecord run =
                        new com.example.fitnesstracker.data.model.RunRecord(runSessionDistance, runSessionCalories, paceStr, runTimer.getFormattedTime());
                runRef.setValue(run);
            }
        }

        timerHandler.removeCallbacks(timerRunnable);
        unregisterSensors();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
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
            long totalBootSteps = (long) event.values[0];

            StepPrefs.ensureToday(this);
            dailyBaseline = StepPrefs.getBaseline(this);

            if (dailyBaseline == -1) {
                dailyBaseline = totalBootSteps;
                StepPrefs.setBaseline(this, dailyBaseline);
            }

            long calculatedDailySteps = totalBootSteps - dailyBaseline;

            if (calculatedDailySteps < 0) {
                dailyBaseline = totalBootSteps;
                StepPrefs.setBaseline(this, dailyBaseline);
                calculatedDailySteps = 0;
            }

            if (calculatedDailySteps >= dailySteps) {
                dailySteps = calculatedDailySteps;
                StepPrefs.setSteps(this, dailySteps);
            }

            if (currentMode == MovementMode.RUN) {
                if (runSessionBaseline == -1) {
                    runSessionBaseline = totalBootSteps;
                }
                int currentRunSteps = (int) (totalBootSteps - runSessionBaseline);
                if (currentRunSteps > runSessionSteps) {
                    int delta = currentRunSteps - runSessionSteps;
                    runSessionSteps = currentRunSteps;
                    runSessionDistance += delta * 0.00075f;
                    runSessionCalories += (int)(delta * 0.04);
                }
            }

            broadcastUpdates();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void broadcastUpdates() {
        Intent intent = new Intent(ACTION_UPDATE_STATS);
        intent.putExtra(EXTRA_MODE, currentMode.name());
        intent.putParcelableArrayListExtra(EXTRA_PATH, currentPath);

        if (currentMode == MovementMode.WALK) {
            intent.putExtra(EXTRA_STEPS, (int) dailySteps);
            intent.putExtra(EXTRA_DISTANCE, dailySteps * 0.00075f);
            intent.putExtra(EXTRA_CALORIES, (int)(dailySteps * 0.04));
        } else {
            intent.putExtra(EXTRA_STEPS, runSessionSteps);
            intent.putExtra(EXTRA_DISTANCE, runSessionDistance);
            intent.putExtra(EXTRA_CALORIES, runSessionCalories);
            intent.putExtra(EXTRA_ELAPSED_TIME, runTimer.getFormattedTime());

            if (runSessionDistance > 0.01) {
                double totalMinutes = runTimer.getElapsedSeconds() / 60.0;
                double paceVal = totalMinutes / runSessionDistance;
                int paceMin = (int) paceVal;
                int paceSec = (int) ((paceVal - paceMin) * 60);
                intent.putExtra(EXTRA_PACE, String.format(Locale.US, "%d:%02d /km", paceMin, paceSec));
            } else {
                intent.putExtra(EXTRA_PACE, "--:-- /km");
            }
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(
                    currentMode == MovementMode.RUN ? runTimer.getFormattedTime() : dailySteps + " steps"
            ));
        }
    }

    private Notification getNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentMode == MovementMode.RUN ? "Running Session" : "Daily Tracking Active")
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

    // THE FIX: Cleanup everything if Android destroys the service
    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        unregisterSensors();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}