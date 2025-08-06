package com.example.mainactivity.Database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "info_table")
public class InfoEntity {
    //define access types

    @PrimaryKey(autoGenerate = false)
    private int day;
    @ColumnInfo(name = "dayName")
    private String dayName;
    @ColumnInfo(name = "info")
    private String info;


    //constructor
    public InfoEntity(int day, String dayName, String info) {
        this.day = day; // pass 0 to auto-generate
        this.info = info;
        this.dayName = dayName;
    }

    //getters and setters
    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
