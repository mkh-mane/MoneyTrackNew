package com.example.moneytrack.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AccountDao {

    @Insert
    void insert(AccountEntity account);

    @Query("SELECT * FROM accounts")
    List<AccountEntity> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE name=:name LIMIT 1")
    AccountEntity getAccountByName(String name);
}