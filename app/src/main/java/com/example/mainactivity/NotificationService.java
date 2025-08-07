package com.example.mainactivity;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class NotificationService extends Service {
    private static final String TAG = "NotificationService";
    private NotificationHelper notificationHelper;
    private DatabaseReference firebaseRef;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                notificationHelper.getForegroundNotification());
        setupFirebaseMonitoring();
    }

    private void setupFirebaseMonitoring() {
        SharedPreferences prefs = getSharedPreferences("DEVICE_PREFS", MODE_PRIVATE);
        Set<String> savedDevices = prefs.getStringSet("CONNECTED_DEVICES", new HashSet<>());

        for (String deviceInfo : savedDevices) {
            String[] parts = deviceInfo.split(" - ");
            if (parts.length >= 3) {
                String macAddress = parts[2];
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

                Integer attempt = generalSnapshot.child("attempt").getValue(Integer.class);
                Integer day = generalSnapshot.child("day").getValue(Integer.class);

                if (attempt != null && day != null) {
                    DatabaseReference dayRef = FirebaseDatabase.getInstance()
                            .getReference("sensors/" + macAddress + "/attempt_" + attempt + "/day_" + day);

                    // Listen for ONLY new child additions (ignores existing data)
                    dayRef.limitToLast(1).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot newEntrySnapshot, @Nullable String previousChildName) {
                            // This will trigger ONLY when a new entry is added
                            Log.d(TAG, "Newest entry detected: " + newEntrySnapshot.getKey());
                            notificationHelper.checkSensorData(newEntrySnapshot, macAddress);
                        }

                        @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                        @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                        @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error monitoring new entries", error.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "General monitoring cancelled", error.toException());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (firebaseRef != null) {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}