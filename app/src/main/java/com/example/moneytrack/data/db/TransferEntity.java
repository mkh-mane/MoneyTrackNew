package com.example.moneytrack.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transfers")
public class TransferEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String fromAccount;
    public String toAccount;
    public double amount;
    public long date;
    public TransferEntity(
            String fromAccount,
            String toAccount,
            double amount,
            long date
    ){
        this.fromAccount=fromAccount;
        this.toAccount=toAccount;
        this.amount=amount;
        this.date=date;
    }

    public TransferEntity(){}
}