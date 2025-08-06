package com.example.mainactivity.Database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mainactivity.Database.entity.TipsEntity;

import java.util.List;

@Dao
public interface TipsDao {
    //GET-ALL FUNCTIONS
    @Query("SELECT * FROM tips_table")
    List<TipsEntity> getAllTips();

    //FIND BY CONDITION FUNCTIONS
    @Query("SELECT * FROM tips_table WHERE condition = :condition")
    List<TipsEntity> getTipsByCondition(String condition);

    //INSERT FUNCTIONS
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTips(TipsEntity... tipsEntity); //can insert 1 or multiple entries

    //DELETE FUNCTION
    @Query("DELETE FROM tips_table")
    void deleteAllTips();
}
