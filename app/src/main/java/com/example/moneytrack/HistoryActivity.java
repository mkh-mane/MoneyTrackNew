package com.example.moneytrack;

import android.content.Intent;
import android.os.Bundle;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneytrack.data.db.AppDatabase;
import com.example.moneytrack.data.db.TransactionDao;
import com.example.moneytrack.data.db.TransactionEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TransactionDao transactionDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        transactionDao = AppDatabase.getInstance(this).transactionDao();

        // Adapter with click listener
        adapter = new TransactionAdapter(new ArrayList<>(), transaction -> {

            new AlertDialog.Builder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Do you want to delete this transaction?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        new Thread(() -> {

                            transactionDao.delete(transaction);

                            runOnUiThread(this::loadData);

                        }).start();

                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        });

        recyclerView.setAdapter(adapter);

        loadData();


        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_history);

        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_history) {
                return true;
            }

            if (item.getItemId() == R.id.nav_analyze) {
                startActivity(new Intent(this, AnalyzeActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }

            return false;
        });
    }

    private void loadData() {

        new Thread(() -> {

            List<TransactionEntity> list = transactionDao.getAllTransactions();

            runOnUiThread(() -> adapter.setData(list));

        }).start();
    }
}