package com.example.moneytrack.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "goals")
public class GoalEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public double targetAmount;
    public double savedAmount;

    public GoalEntity(String name, double targetAmount) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = 0;
    }
}


