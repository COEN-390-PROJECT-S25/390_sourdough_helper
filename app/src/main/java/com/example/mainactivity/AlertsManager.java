package com.example.mainactivity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class AlertsManager extends Application {
    @Override
    public void onCreate(){
        super.onCreate();
        createNotificationChannel();
    }

    //Create a default notification channel for all of our alerts.
    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("alerts", "Alerts", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Alerts the user about the status of their sourdough starter.");

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
