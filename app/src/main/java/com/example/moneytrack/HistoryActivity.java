package com.example.moneytrack;

import android.content.Intent;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneytrack.data.db.AppDatabase;
import com.example.moneytrack.data.db.TransactionDao;
import com.example.moneytrack.data.db.TransactionEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
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

        Button btnAll = findViewById(R.id.btnAll);
        Button btnIncomeFilter = findViewById(R.id.btnIncomeFilter);
        Button btnExpenseFilter = findViewById(R.id.btnExpenseFilter);
        Button btnMonth = findViewById(R.id.btnMonth);

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
        btnAll.setOnClickListener(v -> loadData());
        btnIncomeFilter.setOnClickListener(v -> loadFilteredData("INCOME"));
        btnExpenseFilter.setOnClickListener(v -> loadFilteredData("EXPENSE"));
        btnMonth.setOnClickListener(v -> loadThisMonthData());


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
    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);
        }
    }
    private void loadData() {
        new Thread(() -> {
            List<TransactionEntity> list = transactionDao.getAllTransactions();
            runOnUiThread(() -> adapter.setData(list));
        }).start();
    }

    private void loadFilteredData(String type) {
        new Thread(() -> {
            List<TransactionEntity> list =
                    transactionDao.getTransactionsByType(type);
            runOnUiThread(() ->
                    adapter.setData(list));
        }).start();
    }

    private void loadThisMonthData() {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long start = calendar.getTimeInMillis();
        new Thread(() -> {
            List<TransactionEntity> list = transactionDao.getTransactionsThisMonth(start);
            runOnUiThread(() ->adapter.setData(list));
        }).start();
    }

}