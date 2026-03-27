package com.example.moneytrack.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GoalDao {

    @Insert
    void insert(GoalEntity goal);

    @Query("SELECT * FROM goals")
    List<GoalEntity> getAllGoals();
    @Query("UPDATE goals SET savedAmount = savedAmount + :amount WHERE id = :goalId")
    void addMoney(int goalId, double amount);

    @Query("DELETE FROM goals WHERE id = :goalId")
    void deleteGoal(int goalId);
}
