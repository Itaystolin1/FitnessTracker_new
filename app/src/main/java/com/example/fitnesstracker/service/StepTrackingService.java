package com.example.fitnesstracker.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.fitnesstracker.MainActivity;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.util.StepPrefs;

public class StepTrackingService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private StepPrefs stepPrefs;

    // These variables stop the steps from resetting!
    private int baselineSteps = 0;
    private int previousSavedSteps = 0;
    private boolean isNewSession = true;

    @Override
    public void onCreate() {
        super.onCreate();
        stepPrefs = new StepPrefs(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "StepChannel")
                .setContentTitle("Fitness Tracker")
                .setContentText("Tracking steps...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // FIX: Load history and force a new baseline calculation
        previousSavedSteps = stepPrefs.getDailySteps();
        baselineSteps = 0;
        isNewSession = true;

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int currentSensorValue = (int) event.values[0];

            // Set the baseline ONLY on the first step of this specific session
            if (isNewSession || baselineSteps == 0) {
                baselineSteps = currentSensorValue;
                isNewSession = false;
            }

            int stepsSinceStart = currentSensorValue - baselineSteps;

            if (stepsSinceStart > 0) {
                // Add the delta to whatever we had saved previously today
                int totalToday = previousSavedSteps + stepsSinceStart;
                stepPrefs.saveDailySteps(totalToday);

                // Broadcast update to the UI
                Intent broadcastIntent = new Intent("STEP_UPDATE");
                broadcastIntent.putExtra("steps", totalToday);
                sendBroadcast(broadcastIntent);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "StepChannel",
                    "Step Tracking Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}