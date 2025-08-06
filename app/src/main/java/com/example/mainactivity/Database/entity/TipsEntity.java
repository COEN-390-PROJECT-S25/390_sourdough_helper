package com.example.mainactivity.Database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tips_table")
public class TipsEntity {

    //TODO: public enum for conditions, declare in fermetationanalyzerhelper

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "condition")
    private String condition; //default, temp, humidity, day 7, day 7+
    @ColumnInfo(name = "tip")
    private String tip;

    //constructor
    public TipsEntity(int id, String condition, String tip) {
        this.id = id; // pass 0 to auto-generate
        this.condition = condition;
        this.tip = tip;
    }

    //getters and setters
    public int getId() {
        return id;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }
}
