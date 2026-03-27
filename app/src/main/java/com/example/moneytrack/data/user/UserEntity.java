package com.example.moneytrack.data.user;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String email;
    public String password;

    public UserEntity(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
