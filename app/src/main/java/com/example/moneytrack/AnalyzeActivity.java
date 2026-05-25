package com.example.moneytrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneytrack.data.db.AppDatabase;
import com.example.moneytrack.data.db.GoalDao;
import com.example.moneytrack.data.db.GoalEntity;
import com.example.moneytrack.data.db.TransactionDao;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeActivity extends AppCompatActivity {

    BarChart barChart;
    TransactionDao transactionDao;
    GoalDao goalDao;

    RecyclerView goalsRecycler;
    GoalAdapter goalAdapter;
    int previousTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);

        barChart = findViewById(R.id.barChart);

        TextView btnToday = findViewById(R.id.btnToday);
        TextView btnWeek = findViewById(R.id.btnWeek);
        TextView btnMonth = findViewById(R.id.btnMonth);

        AppDatabase db = AppDatabase.getInstance(this);
        transactionDao = db.transactionDao();
        goalDao = db.goalDao();

        btnToday.setOnClickListener(v -> {
            selectPeriod(btnToday, btnToday, btnWeek, btnMonth);
            loadData(1);
        });

        btnWeek.setOnClickListener(v -> {
            selectPeriod(btnWeek, btnToday, btnWeek, btnMonth);
            loadData(7);
        });

        btnMonth.setOnClickListener(v -> {
            selectPeriod(btnMonth, btnToday, btnWeek, btnMonth);
            loadData(30);
        });

        loadData(30);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_analyze);
        bottomNav.setOnItemSelectedListener(item -> {
            Intent intent = null;
            if(item.getItemId()==R.id.nav_home){
                intent = new Intent(this,MainActivity.class);
            }
            else if(item.getItemId()==R.id.nav_history){
                intent = new Intent(this,HistoryActivity.class);
            }
            else if(item.getItemId()==R.id.nav_profile){
                intent = new Intent(this,ProfileActivity.class);
            }
            else if(item.getItemId()==R.id.nav_analyze){
                return true;
            }
            if(intent!=null){
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            return true;
        });
        goalsRecycler = findViewById(R.id.goalsRecycler);
        goalsRecycler.setLayoutManager(new LinearLayoutManager(this));
        goalAdapter = new GoalAdapter(new ArrayList<>(), goal -> {
            if (goal.savedAmount >= goal.targetAmount) {
                new AlertDialog.Builder(this)
                        .setTitle("Goal Completed")
                        .setMessage("You already reached this goal 🎉")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add money to " + goal.name);

            EditText input = new EditText(this);
            input.setHint("Amount");
            input.setInputType(InputType.TYPE_CLASS_NUMBER);

            builder.setView(input);
            builder.setPositiveButton("Add", (dialog, which) -> {
                String text = input.getText().toString();
                if (text.isEmpty()) return;
                double amount = Double.parseDouble(text);
                new Thread(() -> {
                    goalDao.addMoney(goal.id, amount);
                    runOnUiThread(() -> loadGoals());
                }).start();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        goalsRecycler.setAdapter(goalAdapter);
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }
                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        GoalEntity goal = goalAdapter.getGoalAt(position);

                        new AlertDialog.Builder(AnalyzeActivity.this)
                                .setTitle("Delete Goal")
                                .setMessage("Are you sure you want to delete \"" + goal.name + "\"?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    new Thread(() -> {
                                        goalDao.deleteGoal(goal.id);
                                        runOnUiThread(() -> loadGoals());
                                    }).start();
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    goalAdapter.notifyItemChanged(position);
                                })
                                .show();
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(goalsRecycler);

        Button btnAddGoal = findViewById(R.id.btnAddGoal);

        btnAddGoal.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add New Goal");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);

            EditText nameInput = new EditText(this);
            nameInput.setHint("Goal name");

            EditText amountInput = new EditText(this);
            amountInput.setHint("Target amount");
            amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);

            layout.addView(nameInput);
            layout.addView(amountInput);

            builder.setView(layout);
            builder.setPositiveButton("Add", (dialog, which) -> {
                String name = nameInput.getText().toString();
                double amount = Double.parseDouble(amountInput.getText().toString());
                new Thread(() -> {
                    GoalEntity goal = new GoalEntity(name, amount);
                    goalDao.insert(goal);

                    runOnUiThread(this::loadGoals);
                }).start();

            });
            builder.setNegativeButton("Cancel", null);
            builder.show();

        });

        loadGoals();
    }



    private void selectPeriod(TextView selected, TextView... buttons){
        for(TextView btn : buttons){
            btn.setBackgroundResource(R.drawable.bg_chip_unselected);
            btn.setTextColor(getColor(android.R.color.black));
        }
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(getColor(android.R.color.white));
    }

    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_analyze);
        }
    }

    private void loadData(int days) {
        long now = System.currentTimeMillis();
        long startTime = now - (days * 24L * 60 * 60 * 1000);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String currency = prefs.getString("currency", "AMD ֏");
        new Thread(() -> {

            Double income = transactionDao.getIncomeLast30Days(startTime);
            Double expense = transactionDao.getExpenseLast30Days(startTime);

            if (income == null) income = 0.0;
            if (expense == null) expense = 0.0;

            //  CONVERT
            double convertedIncome = CurrencyUtils.convert(income, currency);
            double convertedExpense = CurrencyUtils.convert(expense, currency);
            runOnUiThread(() -> {
                ArrayList<BarEntry> entries = new ArrayList<>();

                entries.add(new BarEntry(1f, (float) convertedIncome));
                entries.add(new BarEntry(2f, (float) convertedExpense));

                BarDataSet dataSet = new BarDataSet(entries, "Money Flow");

                ArrayList<Integer> colors = new ArrayList<>();
                colors.add(Color.GREEN);
                colors.add(Color.RED);

                dataSet.setColors(colors);

                BarData barData = new BarData(dataSet);

                barChart.setData(barData);
                // 🔥 currency symbol
                String symbol = "֏";
                if (currency.contains("$"))
                    symbol = "$";
                else if (currency.contains("€"))
                    symbol = "€";
                else if (currency.contains("₽"))
                    symbol = "₽";
                barChart.getDescription()
                        .setText("Currency: " + symbol);
                barChart.invalidate();

            });

        }).start();
    }

    private void loadGoals() {
        new Thread(() -> {
            List<GoalEntity> goals = goalDao.getAllGoals();
            runOnUiThread(() -> goalAdapter.setGoals(goals));
        }).start();
    }
}
