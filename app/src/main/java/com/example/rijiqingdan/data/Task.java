package com.example.rijiqingdan.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String date;

    public String content;

    public long createdAt;

    public long updatedAt;
}
