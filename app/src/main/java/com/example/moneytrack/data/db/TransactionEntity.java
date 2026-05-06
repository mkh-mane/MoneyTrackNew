package com.example.moneytrack.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;
    public double amount;
    public String category;
    public String type;
    public long date;
    public String note;
    public String source;
    public String icon; // ✅ emoji

    public TransactionEntity() {}

    // 🔥 FULL constructor
    public TransactionEntity(double amount, String category, String type,
                             long date, String note, String userId,
                             String source, String icon) {

        this.amount = amount;
        this.category = category;
        this.type = type;
        this.date = date;
        this.note = note;
        this.userId = userId;
        this.source = source;
        this.icon = icon;
    }

    // 🔥 fallback constructor (icon default)
    public TransactionEntity(double amount, String category, String type,
                             long date, String note, String userId,
                             String source) {

        this(amount, category, type, date, note, userId, source, "📦");
    }

    // 🔥 fallback constructor 2
    public TransactionEntity(double amount, String category, String type,
                             long date, String note, String userId) {

        this(amount, category, type, date, note, userId, "Cash", "📦");
    }
}