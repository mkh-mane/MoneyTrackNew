package com.example.moneytrack.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public double amount;      // գումար
    public String category;    // category (Food, Transport...)
    public String type;        // INCOME / EXPENSE
    public long date;          // timestamp
    public String note;        // նշում (optional)

    public TransactionEntity(double amount, String category, String type, long date, String note) {
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.date = date;
        this.note = note;
    }
}
