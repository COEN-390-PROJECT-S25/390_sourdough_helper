package com.example.mainactivity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackgroundRoutine extends Worker{
    public BackgroundRoutine(@NonNull Context context, @NonNull WorkerParameters params){
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationHelper helper = new NotificationHelper(getApplicationContext());
        //helper.Alert_Reminder(); // Example: Send a reminder
        return Result.success();
    }

}
