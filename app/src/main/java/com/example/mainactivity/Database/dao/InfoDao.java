package com.example.mainactivity.Database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mainactivity.Database.entity.InfoEntity;

import java.util.List;

@Dao
public interface InfoDao {
    //GET-ALL FUNCTIONS
    @Query("SELECT * FROM info_table")
    List<InfoEntity> getAllInfo();

    //FIND BY DAY FUNCTIONS
    @Query("SELECT * FROM info_table WHERE day = :day")
    InfoEntity getInfoByDay(int day);

    //INSERT FUNCTIONS
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertInfo(InfoEntity... infoEntity); //can insert 1 or multiple entries

    //DELETE FUNCTION
    @Query("DELETE FROM info_table")
    void deleteAllInfo();

}
