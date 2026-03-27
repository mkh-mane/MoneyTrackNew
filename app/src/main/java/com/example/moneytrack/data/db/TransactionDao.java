package com.example.moneytrack.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insert(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<TransactionEntity> getAllTransactions();

    @Delete
    void delete(TransactionEntity transaction);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    Double getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    Double getTotalExpense();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND date >= :startTime")
    Double getIncomeLast30Days(long startTime);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date >= :startTime")
    Double getExpenseLast30Days(long startTime);


    @Query("SELECT SUM(amount) FROM transactions WHERE type='INCOME' AND date >= :startTime")
    Double getIncomeFrom(long startTime);

    @Query("SELECT SUM(amount) FROM transactions WHERE type='EXPENSE' AND date >= :startTime")
    Double getExpenseFrom(long startTime);

}
