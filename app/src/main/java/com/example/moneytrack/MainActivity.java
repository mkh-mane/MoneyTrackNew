package com.example.moneytrack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String currentPhotoPath;

    ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            if (currentPhotoPath != null) {
                                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                                if (bitmap != null) {
                                    processReceiptImage(bitmap);
                                } else {
                                    Toast.makeText(this, "Image load failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvBalance = findViewById(R.id.tvBalance);
        btnIncome = findViewById(R.id.btnIncome);
        btnExpense = findViewById(R.id.btnExpense);
        btnHistory = findViewById(R.id.btnHistory);
        btnVoice = findViewById(R.id.btnVoice);
        btnScan = findViewById(R.id.btnScan);

        database = AppDatabase.getInstance(this);
        transactionDao = database.transactionDao();

        calculateAndUpdateBalance();

        btnIncome.setOnClickListener(v -> showIncomeDialog());
        btnExpense.setOnClickListener(v -> showExpenseDialog());

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
        btnScan.setOnClickListener(v -> openCamera());
//        btnScan.setOnClickListener(v -> {
//            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                try {
//                    File photoFile = createImageFile();
//
//                    Uri photoURI = FileProvider.getUriForFile(
//                            this,
//                            getPackageName() + ".provider",
//                            photoFile
//                    );
//                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//
//                    cameraLauncher.launch(intent);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }
@Override
protected void onResume() {
    super.onResume();

    BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
    if (bottomNav != null) {
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}


    private void showIncomeDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_income, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        EditText etAmount = view.findViewById(R.id.etAmountIncome);
        Button btnSave = view.findViewById(R.id.btnSaveIncome);

        // 🔥 SOURCE
        final String[] selectedSource = {" "};

        LinearLayout srcCash = view.findViewById(R.id.srcCashIncome);
        LinearLayout srcCard = view.findViewById(R.id.srcCardIncome);

        View[] allSources = {srcCash, srcCard};

        srcCash.setOnClickListener(v -> {
            selectedSource[0] = "Cash";
            highlightSelected(srcCash, allSources);
        });

        srcCard.setOnClickListener(v -> {
            selectedSource[0] = "Card";
            highlightSelected(srcCard, allSources);
        });

        // 🔥 CATEGORY
        final String[] selectedCategory = {"Cash"};

        LinearLayout catSalary = view.findViewById(R.id.catSalary);
        LinearLayout catBonus = view.findViewById(R.id.catBonus);
        LinearLayout catOther = view.findViewById(R.id.catOtherIncome);
        LinearLayout catAdd = view.findViewById(R.id.catAddIncome);
        LinearLayout categoryContainer = view.findViewById(R.id.categoryContainerIncome);

        View[] staticCats = {catSalary, catBonus, catOther};

        catSalary.setOnClickListener(v -> {
            selectedCategory[0] = "Salary";
            highlightAllCategories(catSalary, categoryContainer, staticCats);
        });

        catBonus.setOnClickListener(v -> {
            selectedCategory[0] = "Bonus";
            highlightAllCategories(catBonus, categoryContainer, staticCats);
        });

        catOther.setOnClickListener(v -> {
            selectedCategory[0] = "Other";
            highlightAllCategories(catOther, categoryContainer, staticCats);
        });

        // 🔥 LOAD SAVED
        List<String> savedCats = loadCategories("income_categories");
        for (String cat : savedCats) {

            LinearLayout newCat = new LinearLayout(this);
            newCat.setOrientation(LinearLayout.VERTICAL);
            newCat.setGravity(Gravity.CENTER);
            newCat.setPadding(16,16,16,16);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_other);
            icon.setLayoutParams(new LinearLayout.LayoutParams(80,80));

            TextView text = new TextView(this);
            text.setText(cat);

            newCat.addView(icon);
            newCat.addView(text);

            newCat.setOnClickListener(v -> {
                selectedCategory[0] = cat;
                highlightAllCategories(newCat, categoryContainer, staticCats);
            });

            int index = categoryContainer.indexOfChild(catAdd);
            categoryContainer.addView(newCat, index);
        }

        // ➕ ADD NEW
        catAdd.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("New Income Category");
            final EditText input = new EditText(this);
            input.setHint("Enter category name");
            builder.setView(input);
            builder.setPositiveButton("Add", (d, which) -> {
                String newCategory = input.getText().toString().trim();
                if (!newCategory.isEmpty()) {
                    selectedCategory[0] = newCategory;

                    List<String> list = loadCategories("income_categories");
                    list.add(newCategory);
                    saveCategories(list, "income_categories");

                    LinearLayout newCat = new LinearLayout(this);
                    newCat.setOrientation(LinearLayout.VERTICAL);
                    newCat.setGravity(Gravity.CENTER);
                    newCat.setPadding(16,16,16,16);

                    ImageView icon = new ImageView(this);
                    icon.setImageResource(R.drawable.ic_other);
                    icon.setLayoutParams(new LinearLayout.LayoutParams(80,80));

                    TextView text = new TextView(this);
                    text.setText(newCategory);

                    newCat.addView(icon);
                    newCat.addView(text);

                    newCat.setOnClickListener(v1 -> {
                        selectedCategory[0] = newCategory;
                        highlightAllCategories(newCat, categoryContainer, staticCats);
                    });

                    int index = categoryContainer.indexOfChild(catAdd);
                    categoryContainer.addView(newCat, index);
                    newCat.performClick();

                    Toast.makeText(this, "Added: " + newCategory, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        //  SAVE
        btnSave.setOnClickListener(v -> {
            String value = etAmount.getText().toString().trim();
            if (!value.isEmpty()) {
                double amount = Double.parseDouble(value);
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                TransactionEntity transaction = new TransactionEntity(
                        amount,
                        selectedCategory[0],
                        "INCOME",
                        System.currentTimeMillis(),
                        "",
                        userId,
                        selectedSource[0]
                );

                new Thread(() -> {
                    transactionDao.insert(transaction);

                    FirebaseFirestore.getInstance()
                            .collection("transactions")
                            .add(transaction);
                    runOnUiThread(this::calculateAndUpdateBalance);
                }).start();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }


    private void showExpenseDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        EditText etAmount = view.findViewById(R.id.etAmountExpense);
        Button btnSave = view.findViewById(R.id.btnSaveExpense);

        // SOURCE
        final String[] selectedSource = {"Cash"};

        LinearLayout srcCash = view.findViewById(R.id.srcCash);
        LinearLayout srcCard = view.findViewById(R.id.srcCard);

        View[] allSources = {srcCash, srcCard};

        srcCash.setOnClickListener(v -> {
            selectedSource[0] = "Cash";
            highlightSelected(srcCash, allSources);
        });

        srcCard.setOnClickListener(v -> {
            selectedSource[0] = "Card";
            highlightSelected(srcCard, allSources);
        });

        //  CATEGORY
        final String[] selectedCategory = {"Other"};

        LinearLayout catFood = view.findViewById(R.id.catFood);
        LinearLayout catTransport = view.findViewById(R.id.catTransport);
        LinearLayout catShopping = view.findViewById(R.id.catShopping);
        LinearLayout catAdd = view.findViewById(R.id.catAdd);
        LinearLayout categoryContainer = view.findViewById(R.id.categoryContainer);
        View[] staticCats = {catFood, catTransport, catShopping};

        catFood.setOnClickListener(v -> {
            selectedCategory[0] = "Food";
            highlightAllCategories(catFood, categoryContainer, staticCats);
        });

        catTransport.setOnClickListener(v -> {
            selectedCategory[0] = "Transport";
            highlightAllCategories(catTransport, categoryContainer, staticCats);
        });

        catShopping.setOnClickListener(v -> {
            selectedCategory[0] = "Shopping";
            highlightAllCategories(catShopping, categoryContainer, staticCats);
        });
        //  load saved categories
        List<String> savedCats = loadCategories("expense_categories");
        for (String cat : savedCats) {

            LinearLayout newCat = new LinearLayout(this);
            newCat.setOrientation(LinearLayout.VERTICAL);
            newCat.setGravity(Gravity.CENTER);
            newCat.setPadding(16,16,16,16);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_other);
            icon.setLayoutParams(new LinearLayout.LayoutParams(80,80));

            TextView text = new TextView(this);
            text.setText(cat);

            newCat.addView(icon);
            newCat.addView(text);

            newCat.setOnClickListener(v -> {
                selectedCategory[0] = cat;

                highlightAllCategories(newCat, categoryContainer, staticCats);
            });
            int index = categoryContainer.indexOfChild(catAdd);
            categoryContainer.addView(newCat, index);
        }

        //  ADD NEW CATEGORY
        catAdd.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("New Category");

            final EditText input = new EditText(this);
            input.setHint("Enter category name");
            builder.setView(input);

            builder.setPositiveButton("Add", (d, which) -> {
                String newCategory = input.getText().toString().trim();
                if (!newCategory.isEmpty()) {
                    selectedCategory[0] = newCategory;

                    List<String> list = loadCategories("expense_categories");
                    list.add(newCategory);
                    saveCategories(list, "expense_categories");
                    // ADD TO UI
                    LinearLayout newCat = new LinearLayout(this);
                    newCat.setOrientation(LinearLayout.VERTICAL);
                    newCat.setGravity(Gravity.CENTER);
                    newCat.setPadding(16,16,16,16);

                    ImageView icon = new ImageView(this);
                    icon.setImageResource(R.drawable.ic_other);
                    icon.setLayoutParams(new LinearLayout.LayoutParams(80,80));

                    TextView text = new TextView(this);
                    text.setText(newCategory);

                    newCat.addView(icon);
                    newCat.addView(text);
                        newCat.setOnClickListener(v1 -> {
                            selectedCategory[0] = newCategory;

                            highlightAllCategories(newCat, categoryContainer, staticCats);
                        });

                    int index = categoryContainer.indexOfChild(catAdd);
                    categoryContainer.addView(newCat, index);
                    newCat.performClick();

                    Toast.makeText(this, "Added: " + newCategory, Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        btnSave.setOnClickListener(v -> {
            String value = etAmount.getText().toString().trim();
            if (!value.isEmpty()) {
                double amount = Double.parseDouble(value);
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                TransactionEntity transaction = new TransactionEntity(
                        amount,
                        selectedCategory[0],
                        "EXPENSE",
                        System.currentTimeMillis(),
                        "",
                        userId,
                        selectedSource[0]
                );

                new Thread(() -> {
                    // Room
                    transactionDao.insert(transaction);
                    // Firebase
                    FirebaseFirestore.getInstance()
                            .collection("transactions")
                            .add(transaction);
                    runOnUiThread(this::calculateAndUpdateBalance);
                }).start();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
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

    private void highlightSelected(View selected, View[] all) {
        for (View v : all) {
            v.setBackgroundResource(android.R.color.transparent);
        }
        selected.setBackgroundResource(R.drawable.selected_circle);
    }

    private void highlightAllCategories(View selected,
                                        LinearLayout categoryContainer,
                                        View[] staticCats) {
        // static category-ներ
        for (View v : staticCats) {
            v.setBackgroundResource(android.R.color.transparent);
        }
        // dynamic category-ներ
        int count = categoryContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            categoryContainer.getChildAt(i)
                    .setBackgroundResource(android.R.color.transparent);
        }
        // highlight selected
        selected.setBackgroundResource(R.drawable.selected_circle);
    }

    private void saveCategories(List<String> categories, String key) {
        SharedPreferences prefs = getSharedPreferences("cats", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringBuilder sb = new StringBuilder();
        for (String cat : categories) {
            sb.append(cat).append(",");
        }

        editor.putString(key, sb.toString());
        editor.apply();
    }

    private List<String> loadCategories(String key) {
        SharedPreferences prefs = getSharedPreferences("cats", MODE_PRIVATE);
        String data = prefs.getString(key, "");

        List<String> list = new ArrayList<>();

        if (!data.isEmpty()) {
            String[] arr = data.split(",");
            for (String s : arr) {
                if (!s.trim().isEmpty()) list.add(s);
            }
        }

        return list;
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
            } catch (Exception ignored) {
            }
        }
        if (amount == 0) return;
        String type = text.contains("income") ? "INCOME" : "EXPENSE";
        String source = "Cash"; // default

        if (text.contains("card") || text.contains("visa") || text.contains("mastercard")) {
            source = "Card";
        }

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
                    && !word.equals("cash")
                    && !word.equals("card")) {
                category = word;
                break;
            }
        }

        category = category.substring(0, 1).toUpperCase() + category.substring(1);

        double finalAmount = amount;
        String finalCategory = category;
        String finalType = type;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        TransactionEntity transaction = new TransactionEntity(
                finalAmount,
                finalCategory,
                finalType,
                System.currentTimeMillis(),
                "",
                userId,
                source
        );

        new Thread(() -> {
            // Room
            transactionDao.insert(transaction);
            // Firebase
            FirebaseFirestore.getInstance()
                    .collection("transactions")
                    .add(transaction);

            runOnUiThread(this::calculateAndUpdateBalance);
        }).start();
    }

    // scan
    private void processReceiptImage(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, 1000, 1500, true);
        bitmap = fixImageRotation(bitmap);
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(visionText ->
                        extractAmount(visionText.getText()))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show());
    }

    private Bitmap fixImageRotation(Bitmap bitmap) {
        try {
            ExifInterface exif = new ExifInterface(currentPhotoPath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }
            return Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void extractAmount(String text) {
        double bestAmount = 0;
        String[] lines = text.split("\n");
        for (String line : lines) {
            String lower = line.toLowerCase();
            if (lower.contains("total") ||
                    lower.contains("amount") ||
                    lower.contains("total:") ||
                    lower.contains("итого") ||
                    lower.contains("ընդամենը") ||
                    lower.contains("Ընդամենը։")) {

                double value = extractNumberFromLine(line);

                if (value > bestAmount) {
                    bestAmount = value;
                }
            }
        }
        if (bestAmount == 0) {
            for (String line : lines) {
                double value = extractNumberFromLine(line);
                if (value > bestAmount && value < 1000000) {
                    bestAmount = value;
                }
            }
        }
        if (bestAmount > 0) {
            showDetectedAmount(bestAmount);
        } else {
            Toast.makeText(this, "Amount not found", Toast.LENGTH_SHORT).show();
        }
    }




    private double extractNumberFromLine(String line) {
        line = line.replaceAll("(\\d)\\s+(?=\\d)", "$1");
        line = line.replaceAll("[^0-9.,]", " ");
        Pattern pattern = Pattern.compile("\\d{2,}(?:[.,]\\d+)?");
        Matcher matcher = pattern.matcher(line);
        double max = 0;
        while (matcher.find()) {
            try {
                String num = matcher.group();
                num = num.replace(",", ".");
                double value = Double.parseDouble(num);
                if (value > max && value > 20 && value < 1000000) {
                    max = value;
                }
            } catch (Exception ignored) {}
        }
        return max;
    }


    private File createImageFile() throws IOException {
        String fileName = "receipt_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                fileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();

                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.moneytrack.fileprovider", // ⚠️ MUST MATCH MANIFEST
                        photoFile
                );

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // 🔥 կարևոր permission flag
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                cameraLauncher.launch(intent);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDetectedAmount(double amount) {
        final String[] selectedSource = {"Cash"}; // default
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);
        TextView tv = new TextView(this);
        tv.setText("Amount: " + amount);
        tv.setTextSize(18);

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbCash = new RadioButton(this);
        rbCash.setText("Cash");
        rbCash.setId(View.generateViewId()); // ✅ կարևոր
        rbCash.setChecked(true);

        RadioButton rbCard = new RadioButton(this);
        rbCard.setText("Card");
        rbCard.setId(View.generateViewId()); // ✅ կարևոր
        radioGroup.addView(rbCash);
        radioGroup.addView(rbCard);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbCash.getId()) {
                selectedSource[0] = "Cash";
            } else {
                selectedSource[0] = "Card";
            }
        });
        layout.addView(tv);
        layout.addView(radioGroup);

        new AlertDialog.Builder(this)
                .setTitle("Detected Amount 💰")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    TransactionEntity transaction = new TransactionEntity(
                            amount,
                            "Receipt",
                            "EXPENSE",
                            System.currentTimeMillis(),
                            "",
                            userId,
                            selectedSource[0]
                    );

                    new Thread(() -> {
                        transactionDao.insert(transaction);
                        FirebaseFirestore.getInstance()
                                .collection("transactions")
                                .add(transaction);
                        runOnUiThread(this::calculateAndUpdateBalance);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}