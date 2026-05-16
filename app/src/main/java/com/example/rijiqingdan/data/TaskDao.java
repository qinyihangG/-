package com.example.rijiqingdan.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt DESC")
    LiveData<List<Task>> getTasksByDate(String date);

    @Query("SELECT DISTINCT date FROM tasks ORDER BY date ASC")
    LiveData<List<String>> getAllDates();
}
