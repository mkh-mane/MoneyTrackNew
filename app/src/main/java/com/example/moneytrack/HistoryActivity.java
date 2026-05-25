package com.example.moneytrack;

import android.content.Intent;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.TextView;

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

        TextView btnAll = findViewById(R.id.btnAll);
        TextView btnIncomeFilter = findViewById(R.id.btnIncomeFilter);
        TextView btnExpenseFilter = findViewById(R.id.btnExpenseFilter);
        TextView btnMonth = findViewById(R.id.btnMonth);

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
        btnAll.setOnClickListener(v -> {
            selectFilter(
                    btnAll,
                    btnAll,
                    btnIncomeFilter,
                    btnExpenseFilter,
                    btnMonth
            );
            loadData();
        });


        btnIncomeFilter.setOnClickListener(v -> {
            selectFilter(
                    btnIncomeFilter,
                    btnAll,
                    btnIncomeFilter,
                    btnExpenseFilter,
                    btnMonth
            );
            loadFilteredData("INCOME");
        });


        btnExpenseFilter.setOnClickListener(v -> {
            selectFilter(
                    btnExpenseFilter,
                    btnAll,
                    btnIncomeFilter,
                    btnExpenseFilter,
                    btnMonth
            );
            loadFilteredData("EXPENSE");
        });


        btnMonth.setOnClickListener(v -> {
            selectFilter(
                    btnMonth,
                    btnAll,
                    btnIncomeFilter,
                    btnExpenseFilter,
                    btnMonth
            );
            loadThisMonthData();
        });


        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_history);
        bottomNav.setOnItemSelectedListener(item -> {
            Intent intent = null;
            if(item.getItemId()==R.id.nav_home){
                intent = new Intent(this,MainActivity.class);
            }
            else if(item.getItemId()==R.id.nav_analyze){
                intent = new Intent(this,AnalyzeActivity.class);
            }
            else if(item.getItemId()==R.id.nav_profile){
                intent = new Intent(this,ProfileActivity.class);
            }
            else if(item.getItemId()==R.id.nav_history){
                return true;
            }
            if(intent!=null){
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                );
                startActivity(intent);
                finish();
            }
            return true;
        });
    }

    private void selectFilter(TextView selected, TextView... filters){
        for(TextView filter : filters){
            filter.setBackgroundResource(R.drawable.bg_chip_unselected);
            filter.setTextColor(getColor(android.R.color.black));
        }
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(getColor(android.R.color.white));
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