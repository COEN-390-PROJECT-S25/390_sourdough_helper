package com.example.mainactivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class NotificationHelper {
    public static final int FOREGROUND_NOTIFICATION_ID = 1001;
    private static final String TAG = "NotificationHelper";
    private Context context;
    private static final String FOREGROUND_CHANNEL_ID = "foreground_channel";
    private static final String ALERTS_CHANNEL_ID = "alerts";
    private DatabaseReference firebaseRef;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannels();
    }

    public void startFirebaseMonitoring(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("DEVICE_PREFS", Context.MODE_PRIVATE);
        Set<String> savedDevices = prefs.getStringSet("CONNECTED_DEVICES", new HashSet<>());

        for (String deviceInfo : savedDevices) {
            // Extract MAC address from the saved device info
            String[] parts = deviceInfo.split(" - ");
            if (parts.length >= 3) {
                String macAddress = parts[2]; // MAC is the third part
                monitorDevice(macAddress);
            }
        }
    }

    private void monitorDevice(String macAddress) {
        DatabaseReference deviceRef = FirebaseDatabase.getInstance()
                .getReference("sensors/" + macAddress + "/general");

        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot generalSnapshot) {
                if (!generalSnapshot.exists()) return;

                // Get current attempt and day
                Integer attempt = generalSnapshot.child("attempt").getValue(Integer.class);
                Integer day = generalSnapshot.child("current_day").getValue(Integer.class);
                System.out.println("DAY IS: " + day + "ATTEMPT IS: " + attempt);
                if (attempt != null && day != null) {
                    // Monitor the specific day's data
                    DatabaseReference dataRef = FirebaseDatabase.getInstance()
                            .getReference("sensors/" + macAddress + "/attempt_" + attempt + "/day_" + day);

                    dataRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            checkSensorData(dataSnapshot, macAddress);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Data monitoring cancelled for " + macAddress, error.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "General monitoring cancelled for " + macAddress, error.toException());
            }
        });
    }

    void checkSensorData(DataSnapshot entrySnapshot, String macAddress) {
        // Check for CO2 levels in the specific entry
        if (entrySnapshot.child("co2").exists()) {
            Double co2 = entrySnapshot.child("co2").getValue(Double.class);
            Log.d(TAG, "CO2 value detected: " + co2 + " in entry " +
                    entrySnapshot.getKey() + " for device " + macAddress);

            if (co2 != null && co2 > 1000) { // Alert when CO2 is too high
                Alert_CO2(macAddress);
            }
        }

        // Temperature check remains the same
        if (entrySnapshot.child("temperature").exists()) {
            Double temp = entrySnapshot.child("temperature").getValue(Double.class);
            if (temp != null && (temp < 20 || temp > 28)) {
                Alert_Temperature(macAddress);
            }
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Foreground service channel
            NotificationChannel foregroundChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            foregroundChannel.setDescription("Service running in background");

            // Alerts channel (higher priority)
            NotificationChannel alertsChannel = new NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    "Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription("Important alerts about your sourdough starter");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(foregroundChannel);
            manager.createNotificationChannel(alertsChannel);
        }
    }

    public void Alert_CO2(String macAddress) {
        Log.d(TAG, "Showing CO2 alert for device " + macAddress);
        showAlertNotification(
                1,
                "Sourdough Alert (" + macAddress + ")",
                "Your starter has stopped growing (low CO2)."
        );
    }


    public void Alert_Temperature(String macAddress) {
        Log.d(TAG, "Showing temperature alert for device " + macAddress);
        showAlertNotification(
                2,
                "Sourdough Alert (" + macAddress + ")",
                "Temperature issue detected with your starter!"
        );
    }

    public void Alert_Reminder() {
        Log.d(TAG, "Showing reminder");
        showAlertNotification(
                4,
                "Sourdough Helper",
                "Don't forget to check on your starter!"
        );
    }

    private void showAlertNotification(int id, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_circle_notifications_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        showNotification(builder, id);
    }

    private void showNotification(NotificationCompat.Builder builder, int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        } else {
            Log.w(TAG, "Notification permission not granted");
        }
    }

    public Notification getForegroundNotification() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Sourdough Monitor")
                .setContentText("Monitoring your starter...")
                .setSmallIcon(R.drawable.baseline_circle_notifications_24)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}