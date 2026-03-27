//package com.example.moneytrack.data.db;
//
//import com.example.moneytrack.data.user.UserEntity;
//import com.example.moneytrack.data.user.UserDao;
//
//
//import android.content.Context;
//
//import androidx.room.Database;
//import androidx.room.Room;
//import androidx.room.RoomDatabase;
//
//@Database(
//        entities = {TransactionEntity.class, UserEntity.class},
//        version = 1,
//        exportSchema = false
//)
//public abstract class AppDatabase extends RoomDatabase {
//
//    private static AppDatabase INSTANCE;
//
//    public abstract TransactionDao transactionDao();
//    public abstract UserDao userDao();   // սա ավելացրու
//
//    public static synchronized AppDatabase getInstance(Context context) {
//        if (INSTANCE == null) {
//            INSTANCE = Room.databaseBuilder(
//                    context.getApplicationContext(),
//                    AppDatabase.class,
//                    "moneytrack_db"
//            ).build();
//        }
//        return INSTANCE;
//    }
//}


package com.example.moneytrack.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.moneytrack.data.user.UserDao;
import com.example.moneytrack.data.user.UserEntity;

@Database(entities = {TransactionEntity.class,UserEntity.class, GoalEntity.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TransactionDao transactionDao();
    public abstract UserDao userDao();
    public abstract GoalDao goalDao();

    public static synchronized AppDatabase getInstance(Context context) {

        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "moneytrack_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return instance;
    }
}
