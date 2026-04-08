package com.example.moneytrack.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String userId;

    public double amount;      // գումար
    public String category;    // category (Food, Transport...)
    public String type;        // INCOME / EXPENSE
    public long date;          // timestamp
    public String note;// նշում (optional)
    public String source;

    public TransactionEntity() {}

    public TransactionEntity(double amount, String category, String type, long date, String note, String userId, String source) {
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.date = date;
        this.note = note;
        this.userId=userId;
        this.source=source;
    }
    public TransactionEntity(double amount, String category, String type, long date, String note, String userId) {
        this(amount, category, type, date, note, userId, "Cash");
    }
}
