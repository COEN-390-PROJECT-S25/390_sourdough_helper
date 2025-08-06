package com.example.mainactivity.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mainactivity.Database.dao.InfoDao;
import com.example.mainactivity.Database.dao.TipsDao;
import com.example.mainactivity.Database.entity.InfoEntity;

import java.security.AccessControlContext;

@Database(entities = {InfoEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "app_database";

    protected AppDatabase() {} //private constructor to prevent instantiation

    //creates the database
    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).allowMainThreadQueries().build();
    }

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = create(context);
        }
        return INSTANCE;
    }

    //dao methods
    public abstract InfoDao infoDao();
    public abstract TipsDao tipsDao();

}
