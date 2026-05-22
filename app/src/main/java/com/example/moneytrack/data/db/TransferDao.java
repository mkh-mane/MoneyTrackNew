package com.example.moneytrack.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransferDao {
    @Insert
    void insert(TransferEntity transfer);
    @Query("SELECT * FROM transfers")
    List<TransferEntity> getAllTransfers();
}