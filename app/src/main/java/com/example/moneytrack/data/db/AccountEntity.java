package com.example.moneytrack.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class AccountEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public String currency;

    public AccountEntity(String name) {
        this.name = name;
        this.currency = "AMD ֏";
    }


    public AccountEntity(String name, String currency) {
        this.name = name;
        this.currency = currency;
    }

    public AccountEntity() {

    }
}