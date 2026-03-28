package com.example.moneytrack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.example.moneytrack.data.db.AppDatabase;
import com.example.moneytrack.data.db.TransactionDao;
import com.example.moneytrack.data.db.TransactionEntity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvBalance;
//    private Button btnIncome, btnExpense, btnHistory, btnVoice, btnScan;
    private MaterialButton btnIncome, btnExpense, btnHistory, btnVoice, btnScan;

    private AppDatabase database;
    private TransactionDao transactionDao;

    ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            Bundle extras = result.getData().getExtras();

                            if (extras != null) {
                                Bitmap imageBitmap = (Bitmap) extras.get("data");

                                if (imageBitmap != null) {
                                    processReceiptImage(imageBitmap);
                                }
                            }
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBalance = findViewById(R.id.tvBalance);
        btnIncome = findViewById(R.id.btnIncome);
        btnExpense = findViewById(R.id.btnExpense);
        btnHistory = findViewById(R.id.btnHistory);
        btnVoice = findViewById(R.id.btnVoice);
        btnScan = findViewById(R.id.btnScan);

        database = AppDatabase.getInstance(this);
        transactionDao = database.transactionDao();

        calculateAndUpdateBalance();

        if (btnIncome != null) btnIncome.setOnClickListener(v -> showAmountDialog("income"));
        if (btnExpense != null) btnExpense.setOnClickListener(v -> showAmountDialog("expense"));

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, HistoryActivity.class))
            );
        }

        if (btnVoice != null) btnVoice.setOnClickListener(v -> startVoiceInput());

        // Bottom Navigation
        View navView = findViewById(R.id.bottomNav);
        if (navView instanceof BottomNavigationView) {
            BottomNavigationView bottomNav = (BottomNavigationView) navView;
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;

                if (id == R.id.nav_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    return true;
                }
                if (id == R.id.nav_analyze) {
                    startActivity(new Intent(this, AnalyzeActivity.class));
                    return true;
                }
                if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        //  Scan Button
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    cameraLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showAmountDialog(String type) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Transaction");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_transaction, null);
        builder.setView(view);

        EditText etAmount = view.findViewById(R.id.etAmount);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        EditText etCustomCategory = view.findViewById(R.id.etCustomCategory);

        String[] categories = {"Food", "Transport", "Salary", "Shopping", "Other"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );

        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (categories[position].equals("Other")) {
                    etCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    etCustomCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setPositiveButton("OK", (dialog, which) -> {

            String value = etAmount.getText().toString().trim();

            if (!value.isEmpty()) {

                double amount = Double.parseDouble(value);

                String selectedCategory;

                if (spinnerCategory.getSelectedItem().toString().equals("Other")) {
                    selectedCategory = etCustomCategory.getText().toString().trim();

                    if (selectedCategory.isEmpty()) selectedCategory = "Other";

                } else {
                    selectedCategory = spinnerCategory.getSelectedItem().toString();
                }

                String finalSelectedCategory = selectedCategory;

                new Thread(() -> {

                    TransactionEntity transaction = new TransactionEntity(
                            amount,
                            finalSelectedCategory,
                            type.toUpperCase(),
                            System.currentTimeMillis(),
                            ""
                    );

                    transactionDao.insert(transaction);

                    runOnUiThread(this::calculateAndUpdateBalance);

                }).start();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void calculateAndUpdateBalance() {

        new Thread(() -> {

            List<TransactionEntity> list = transactionDao.getAllTransactions();

            double total = 0;

            for (TransactionEntity t : list) {
                if (t.type.equals("INCOME")) total += t.amount;
                else total -= t.amount;
            }

            double finalTotal = total;

            runOnUiThread(() ->
                    tvBalance.setText("Balance: " + finalTotal)
            );

        }).start();
    }

   //voice
    private void startVoiceInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {

            ArrayList<String> result =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (result != null && !result.isEmpty()) {
                processVoiceInput(result.get(0));
            }
        }
    }

    private void processVoiceInput(String text) {
        text = text.toLowerCase();
        double amount = 0;
        for (String word : text.split(" ")) {
            try {
                amount = Double.parseDouble(word.replace(",", "").replace(".", ""));
                break;
            } catch (Exception ignored) {}
        }
        if (amount == 0) return;
        String type = text.contains("income") ? "INCOME" : "EXPENSE";
        String category = "Other";
        for (String word : text.split(" ")) {
            word = word.replaceAll("[^a-z]", "");
            if (!word.isEmpty()
                    && !word.equals("spent")
                    && !word.equals("on")
                    && !word.equals("for")
                    && !word.equals("the")
                    && !word.equals("a")
                    && !word.equals("i")
                    && !word.equals("income")
                    && !word.equals("expense")
                    && !word.equals("salary")) {
                category = word;
                break;
            }
        }

        category = category.substring(0,1).toUpperCase() + category.substring(1);

        double finalAmount = amount;
        String finalCategory = category;
        String finalType = type;

        new Thread(() -> {
            transactionDao.insert(new TransactionEntity(
                    finalAmount,
                    finalCategory,
                    finalType,
                    System.currentTimeMillis(),
                    ""
            ));

            runOnUiThread(this::calculateAndUpdateBalance);
        }).start();
    }

    // scan
    private void processReceiptImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(visionText ->
                        extractAmount(visionText.getText()))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show());
    }

    private void extractAmount(String text) {
        double amount = 0;

        Pattern pattern = Pattern.compile("(\\d+[\\s.,]?\\d+)+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {

            try {
                String number = matcher.group();

                // ջնջում ենք space-երը → "2 560" → "2560"
                number = number.replace(" ", "");

                // փոխում ենք ստորակետը կետի
                number = number.replace(",", ".");

                double value = Double.parseDouble(number);

                if (value > amount) amount = value;

            } catch (Exception ignored) {}
        }
        if (amount > 0) showDetectedAmount(amount);
        else Toast.makeText(this, "Amount not found", Toast.LENGTH_SHORT).show();
    }

    private void showDetectedAmount(double amount) {
        new AlertDialog.Builder(this)
                .setTitle("Detected Amount 💰")
                .setMessage("Amount: " + amount)
                .setPositiveButton("Add", (d, w) -> {

                    new Thread(() -> {
                        transactionDao.insert(new TransactionEntity(
                                amount,
                                "Receipt",
                                "EXPENSE",
                                System.currentTimeMillis(),
                                ""
                        ));

                        runOnUiThread(this::calculateAndUpdateBalance);

                    }).start();

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}