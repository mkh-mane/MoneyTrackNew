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

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.moneytrack.data.db.AccountDao;
import com.example.moneytrack.data.db.AccountEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.example.moneytrack.data.db.AppDatabase;
import com.example.moneytrack.data.db.TransactionDao;
import com.example.moneytrack.data.db.TransactionEntity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private TextView tvBalance;
    private MaterialButton btnIncome, btnExpense, btnHistory, btnVoice, btnScan, btnTransfer;
    private AppDatabase database;
    private TransactionDao transactionDao;
    private AccountDao accountDao;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String currentPhotoPath;
    private double usdRate = 1;
    private double eurRate = 0.92;
    private double rubRate = 89;
    private double amdPerUsd = 390;
    private ViewPager2 balancePager;

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

//        tvBalance = findViewById(R.id.tvBalance);
        balancePager = findViewById(R.id.balancePager);
        btnIncome = findViewById(R.id.btnIncome);
        btnExpense = findViewById(R.id.btnExpense);
        btnHistory = findViewById(R.id.btnHistory);
        btnVoice = findViewById(R.id.btnVoice);
        btnScan = findViewById(R.id.btnScan);
        btnTransfer = findViewById(R.id.btnTransfer);

        database = AppDatabase.getInstance(this);
        transactionDao = database.transactionDao();
        accountDao = database.accountDao();

        calculateAndUpdateBalance();

        btnIncome.setOnClickListener(v -> showIncomeDialog());
        btnExpense.setOnClickListener(v -> showExpenseDialog());

        btnTransfer.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TransferActivity.class)
            );
        });

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

        //currency
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.exchangerate.host/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ExchangeApi api = retrofit.create(ExchangeApi.class);

        api.getRates().enqueue(new Callback<ExchangeResponse>() {
            @Override
            public void onResponse(Call<ExchangeResponse> call,
                                   Response<ExchangeResponse> response) {

                if (response.body() != null &&
                        response.body().rates != null) {
                    Double amd = response.body().rates.get("AMD");
                    if (amd != null) {
                        amdPerUsd = amd;
                    }
                    calculateAndUpdateBalance();
                }
            }

            @Override
            public void onFailure(Call<ExchangeResponse> call, Throwable t) {

            }
        });
    }

    private double convertCurrency(double amount, String currency) {
        switch (currency) {
            case "USD $":
                return amount / amdPerUsd;
            case "EUR €":
                return (amount / amdPerUsd) * 0.92;
            case "RUB ₽":
                return (amount / amdPerUsd) * 89;
            default:
                return amount;
        }
    }


    private double convertToAMD(double amount,String currency) {
        switch (currency) {
            case "USD $":
                return amount * amdPerUsd;
            case "EUR €":
                return (amount / 0.92) * amdPerUsd;
            case "RUB ₽":
                return (amount / 89) * amdPerUsd;
            default:
                return amount;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        calculateAndUpdateBalance();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if(bottomNav != null){
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }


    private void showIncomeDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_income, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        EditText etAmount = view.findViewById(R.id.etAmountIncome);
        Button btnSave = view.findViewById(R.id.btnSaveIncome);

        // SOURCE
        final String[] selectedSource = {"Cash"};

        // ACCOUNT
        final String[] selectedAccount = {"Cash"};
        LinearLayout accountContainer = view.findViewById(R.id.accountContainer);
        LinearLayout srcCash = view.findViewById(R.id.srcCash);
        LinearLayout srcCard = view.findViewById(R.id.srcCard);
        View[] allSources = {srcCash, srcCard};


        srcCash.setOnClickListener(v -> {
            selectedSource[0] = "Cash";
            selectedAccount[0] = "Cash";
            highlightSelected(srcCash, allSources);
            clearAccountSelection(accountContainer);
        });
        srcCard.setOnClickListener(v -> {
            selectedSource[0] = "Card";
            selectedAccount[0] = "Card";
            highlightSelected(srcCard, allSources);
            clearAccountSelection(accountContainer);
        });


        // CATEGORY
        final String[] selectedCategory = {"Salary"};
        final String[] selectedIcon = {"💰"};


        // LOAD ACCOUNTS

        new Thread(() -> {
            List<AccountEntity> accounts = accountDao.getAllAccounts();

            runOnUiThread(() -> {
                for(AccountEntity account : accounts){
                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.VERTICAL);
                    item.setGravity(Gravity.CENTER);
                    item.setPadding(20, 20, 20, 20);

                    ImageView image = new ImageView(this);
                    image.setImageResource(R.drawable.ic_wallet);
                    image.setBackgroundResource(R.drawable.bg_circle);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
                    image.setLayoutParams(params);

                    TextView text = new TextView(this);
                    text.setText(account.name);
                    item.addView(image);
                    item.addView(text);


                    item.setOnClickListener(v -> {
                        selectedAccount[0] = account.name;
                        selectedSource[0] = account.name;
                        clearAccountSelection(accountContainer);
                        item.setBackgroundResource(R.drawable.selected_background);
                        for(View source : allSources){
                            source.setBackground(null);
                        }
                    });
                    accountContainer.addView(item);
                }
            });
        }).start();



        // CATEGORIES

        LinearLayout catSalary = view.findViewById(R.id.catSalary);
        LinearLayout catBusiness = view.findViewById(R.id.catBonus);
        LinearLayout catGift = view.findViewById(R.id.catOther);

        LinearLayout catAdd = view.findViewById(R.id.catAdd);

        LinearLayout categoryContainer = view.findViewById(R.id.categoryContainer);
        View[] staticCats = {catSalary, catBusiness, catGift};
        catSalary.setOnClickListener(v -> {
            selectedCategory[0] = "Salary";
            selectedIcon[0] = "💰";
            highlightAllCategories(catSalary, categoryContainer, staticCats);
        });


        catBusiness.setOnClickListener(v -> {
            selectedCategory[0] = "Business";
            selectedIcon[0] = "📈";
            highlightAllCategories(catBusiness, categoryContainer, staticCats);
        });

        catGift.setOnClickListener(v -> {
            selectedCategory[0] = "Gift";
            selectedIcon[0] = "🎁";
            highlightAllCategories(catGift, categoryContainer, staticCats);
        });


        List<String> savedCats = loadCategories("income_categories");
        for(String cat : savedCats){
            addCategoryView(
                    cat,
                    categoryContainer,
                    selectedCategory,
                    selectedIcon,
                    staticCats,
                    catAdd
            );
        }


        catAdd.setOnClickListener(v ->
                showAddCategoryDialog(
                        "income_categories",
                        categoryContainer,
                        selectedCategory,
                        selectedIcon,
                        staticCats,
                        catAdd
                )
        );


        // SAVE
        btnSave.setOnClickListener(v -> {
            String value = etAmount.getText().toString().trim();
            if(!value.isEmpty()){
                double enteredAmount = Double.parseDouble(value);
                SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                String currency = prefs.getString("currency", "AMD ֏");
                double amount = convertToAMD(enteredAmount, currency);
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                TransactionEntity transaction = new TransactionEntity(
                                amount,
                                selectedCategory[0],
                                "INCOME",
                                System.currentTimeMillis(),
                                "",
                                userId,
                                selectedSource[0],
                                selectedIcon[0],
                                selectedAccount[0]
                        );

                new Thread(() -> {
                    transactionDao.insert(transaction);

                    FirebaseFirestore
                            .getInstance()
                            .collection(
                                    "transactions"
                            )
                            .add(
                                    transaction
                            );

                    runOnUiThread(
                            this::calculateAndUpdateBalance
                    );

                }).start();
                dialog.dismiss();

            }
            else{
                Toast.makeText(
                        this,
                        "Enter amount",
                        Toast.LENGTH_SHORT
                ).show();
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

        // ACCOUNT
        final String[] selectedAccount = {"Cash"};
        LinearLayout accountContainer = view.findViewById(R.id.accountContainer);
        LinearLayout srcCash = view.findViewById(R.id.srcCash);
        LinearLayout srcCard = view.findViewById(R.id.srcCard);
        View[] allSources = {srcCash, srcCard};


        // Cash selected
        srcCash.setOnClickListener(v -> {
            selectedAccount[0] = "Cash";
            selectedSource[0] = "Cash";
            highlightSelected(srcCash, allSources);
            clearAccountSelection(accountContainer);
        });

        // Card selected
        srcCard.setOnClickListener(v -> {
            selectedAccount[0] = "Card";
            selectedSource[0] = "Card";
            highlightSelected(srcCard, allSources);
            clearAccountSelection(accountContainer);
        });

        // CATEGORY
        final String[] selectedCategory = {"Food"};
        final String[] selectedIcon = {"🍔"};


        // LOAD ACCOUNTS
        new Thread(() -> {
            List<AccountEntity> accounts = accountDao.getAllAccounts();
            runOnUiThread(() -> {
                for(AccountEntity account : accounts){
                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.VERTICAL);
                    item.setGravity(Gravity.CENTER);
                    item.setPadding(20, 20, 20, 20);
                    ImageView image = new ImageView(this);
                    image.setImageResource(R.drawable.ic_wallet);
                    image.setBackgroundResource(R.drawable.bg_circle);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
                    image.setLayoutParams(params);

                    TextView text = new TextView(this);
                    text.setText(account.name);

                    item.addView(image);
                    item.addView(text);

                    item.setOnClickListener(v -> {
                        selectedAccount[0] = account.name;
                        selectedSource[0] = account.name;
                        clearAccountSelection(accountContainer);

                        item.setBackgroundResource(R.drawable.selected_background);

                        // Remove Cash/Card selection
                        for(View source : allSources){
                            source.setBackground(
                                    null
                            );
                        }
                    });
                    accountContainer.addView(item);
                }

            });

        }).start();


        // CATEGORIES
        LinearLayout catFood = view.findViewById(R.id.catFood);
        LinearLayout catTransport = view.findViewById(R.id.catTransport);
        LinearLayout catShopping = view.findViewById(R.id.catShopping);
        LinearLayout catAdd = view.findViewById(R.id.catAdd);
        LinearLayout categoryContainer = view.findViewById(R.id.categoryContainer);
        View[] staticCats = {catFood, catTransport, catShopping};

        catFood.setOnClickListener(v -> {
            selectedCategory[0] = "Food";
            selectedIcon[0] = "🍔";
            highlightAllCategories(catFood, categoryContainer, staticCats);
        });

        catTransport.setOnClickListener(v -> {
            selectedCategory[0] = "Transport";
            selectedIcon[0] = "🚗";
            highlightAllCategories(catTransport, categoryContainer, staticCats);
        });

        catShopping.setOnClickListener(v -> {
            selectedCategory[0] = "Shopping";
            selectedIcon[0] = "🛒";
            highlightAllCategories(catShopping, categoryContainer, staticCats);
        });


        // LOAD SAVED CATEGORIES
        List<String> savedCats = loadCategories("expense_categories");
        for(String cat : savedCats){
            addCategoryView(
                    cat,
                    categoryContainer,
                    selectedCategory,
                    selectedIcon,
                    staticCats,
                    catAdd
            );
        }


        catAdd.setOnClickListener(v ->
                showAddCategoryDialog(
                        "expense_categories",
                        categoryContainer,
                        selectedCategory,
                        selectedIcon,
                        staticCats,
                        catAdd
                )
        );


        // SAVE
        btnSave.setOnClickListener(v -> {
            String value = etAmount.getText().toString().trim();
            if(!value.isEmpty()){
                double enteredAmount = Double.parseDouble(value);
                SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                String currency = prefs.getString("currency", "AMD ֏");
                double amount = convertToAMD(enteredAmount, currency);
                String userId = FirebaseAuth
                                .getInstance()
                                .getCurrentUser()
                                .getUid();

                TransactionEntity transaction =
                        new TransactionEntity(
                                amount,
                                selectedCategory[0],
                                "EXPENSE",
                                System.currentTimeMillis(),
                                "",
                                userId,
                                selectedSource[0],
                                selectedIcon[0],
                                selectedAccount[0]
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

                Toast.makeText(
                        this,
                        "Enter amount",
                        Toast.LENGTH_SHORT
                ).show();

            }
        });
        dialog.show();
    }




    private void clearAccountSelection(LinearLayout accountContainer){
        for(int i = 0; i < accountContainer.getChildCount(); i++){
            View child = accountContainer.getChildAt(i);
            child.setBackground(null);
        }
    }


    private void addCategoryView(String cat,
                                 LinearLayout container,
                                 String[] selectedCategory,
                                 String[] selectedIcon,
                                 View[] staticCats,
                                 View catAdd) {

        String[] parts = cat.split("\\|");
        String emoji = parts[0];
        String name = parts.length > 1 ? parts[1] : cat;

        LinearLayout newCat = new LinearLayout(this);
        newCat.setOrientation(LinearLayout.VERTICAL);
        newCat.setGravity(Gravity.CENTER);
        newCat.setPadding(16,16,16,16);

        TextView icon = new TextView(this);
        icon.setText(emoji);
        icon.setTextSize(24);

        TextView text = new TextView(this);
        text.setText(name);

        newCat.addView(icon);
        newCat.addView(text);

        newCat.setOnClickListener(v -> {
            selectedCategory[0] = name;
            selectedIcon[0] = emoji;
            highlightAllCategories(newCat, container, staticCats);
        });

        int index = container.indexOfChild(catAdd);
        container.addView(newCat, index);
    }

    private void showAddCategoryDialog(String key,
                                       LinearLayout container,
                                       String[] selectedCategory,
                                       String[] selectedIcon,
                                       View[] staticCats,
                                       View catAdd) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Category");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30,20,30,10);

        EditText input = new EditText(this);
        input.setHint("Category name");

        EditText emojiInput = new EditText(this);
        emojiInput.setHint("Emoji (example: 🍔 🚗 💰)");

        layout.addView(input);
        layout.addView(emojiInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (d, which) -> {

            String name = input.getText().toString().trim();
            String emoji = emojiInput.getText().toString().trim();

            if (!name.isEmpty()) {

                if (emoji.isEmpty()) emoji = "📦";

                String finalCat = emoji + "|" + name;

                List<String> list = loadCategories(key);
                list.add(0, finalCat);
                saveCategories(list, key);

                addCategoryView(finalCat, container, selectedCategory, selectedIcon, staticCats, catAdd);

                Toast.makeText(this, "Added: " + name, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void calculateAndUpdateBalance() {
        new Thread(() -> {
            List<TransactionEntity> list = transactionDao.getAllTransactions();
            double totalAMD = 0;
            double cashAMD = 0;
            double cardAMD = 0;
            for(TransactionEntity t:list){
                double amountAMD = convertToAMD(t.amount, getAccountCurrency(t.accountName));
                double value = t.type.equals("INCOME") ? amountAMD : -amountAMD;
                totalAMD += value;
                if("Cash".equals(t.accountName))
                    cashAMD += value;
                else if("Card".equals(t.accountName))
                    cardAMD += value;
            }

            SharedPreferences prefs =
                    getSharedPreferences(
                            "settings",
                            MODE_PRIVATE
                    );

            String currency =
                    prefs.getString(
                            "currency",
                            "AMD ֏"
                    );

            String symbol="֏";

            if(currency.contains("$"))
                symbol="$";
            else if(currency.contains("€"))
                symbol="€";
            else if(currency.contains("₽"))
                symbol="₽";


            List<BalanceItem> balanceItems =
                    new ArrayList<>();


            // Total
            balanceItems.add(
                    new BalanceItem(
                            "Total Balance",
                            String.format(
                                    "%.2f %s",
                                    CurrencyUtils.convert(
                                            totalAMD,
                                            currency
                                    ),
                                    symbol
                            ),
                            false
                    )
            );

            // Cash
            balanceItems.add(
                    new BalanceItem(
                            "Cash Balance",
                            String.format(
                                    "%.2f %s",
                                    CurrencyUtils.convert(
                                            cashAMD,
                                            currency
                                    ),
                                    symbol
                            ),
                            false
                    )
            );

            // Card
            balanceItems.add(
                    new BalanceItem(
                            "Card Balance",
                            String.format(
                                    "%.2f %s",
                                    CurrencyUtils.convert(
                                            cardAMD,
                                            currency
                                    ),
                                    symbol
                            ),
                            false
                    )
            );


            // Custom accounts
            List<AccountEntity> accounts = accountDao.getAllAccounts();
            for(AccountEntity account : accounts){
                double accountBalance = 0;
                for(TransactionEntity t : list){
                    if(account.name.equals(t.accountName)){
                        double value =
                                t.type.equals("INCOME")
                                        ? t.amount
                                        : -t.amount;

                        accountBalance += value;
                    }
                }

                String accountSymbol="֏";

                if(account.currency.contains("$"))
                    accountSymbol="$";

                else if(account.currency.contains("€"))
                    accountSymbol="€";

                else if(account.currency.contains("₽"))
                    accountSymbol="₽";

                balanceItems.add(
                        new BalanceItem(
                                account.name,
                                String.format(
                                        "%.2f %s",
                                        accountBalance,
                                        accountSymbol
                                ),
                                false
                        )
                );
            }

            balanceItems.add(
                    new BalanceItem(
                            "Add Account",
                            "",
                            true
                    )
            );

            runOnUiThread(() -> {

                balancePager.setAdapter(
                        new BalancePagerAdapter(
                                balanceItems,
                                MainActivity.this
                        )
                );

            });
        }).start();
    }


    private String getAccountCurrency(String accountName){
        if(accountName.equals("Cash"))
            return "AMD ֏";
        if(accountName.equals("Card"))
            return "AMD ֏";
        List<AccountEntity> accounts = accountDao.getAllAccounts();
        for(AccountEntity account : accounts){
            if(account.name.equals(accountName))
                return account.currency;
        }
        return "AMD ֏";
    }



    void showAddAccountDialog() {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setTitle("Create Account");

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);

        layout.setPadding(
                50,
                30,
                50,
                10
        );

        EditText input = new EditText(this);
        input.setHint("Account Name");
        layout.addView(input);

        Spinner spinner = new Spinner(this);
        ArrayList<String> currencies = new ArrayList<>();

        currencies.add("AMD ֏");
        currencies.add("USD $");
        currencies.add("EUR €");
        currencies.add("RUB ₽");

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        currencies
                );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(spinnerAdapter);
        layout.addView(spinner);
        builder.setView(layout);
        builder.setPositiveButton(
                "Create",
                (dialog, which) -> {

                    String name =
                            input.getText()
                                    .toString()
                                    .trim();

                    if(name.isEmpty()){

                        Toast.makeText(
                                this,
                                "Enter name",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String currency =
                            spinner.getSelectedItem()
                                    .toString();

                    new Thread(() -> {

                        AccountEntity account =
                                new AccountEntity();

                        account.name = name;
                        account.currency = currency;

                        accountDao.insert(account);

                        runOnUiThread(
                                this::calculateAndUpdateBalance
                        );

                    }).start();

                });

        builder.setNegativeButton(
                "Cancel",
                null
        );

        builder.show();
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

        JSONArray array = new JSONArray();
        for (String cat : categories) {
            array.put(cat);
        }
        editor.putString(key, array.toString());
        editor.apply();
    }

    private List<String> loadCategories(String key) {
        SharedPreferences prefs = getSharedPreferences("cats", MODE_PRIVATE);
        String data = prefs.getString(key, "");

        List<String> list = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (Exception ignored) {}

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
        for(String word : text.split(" ")){
            try{
                amount = Double.parseDouble(
                        word.replace(",", "")
                                .replace(".", "")
                );
                break;
            }
            catch(Exception ignored){}
        }

        if(amount == 0)
            return;
        String type = text.contains("income") ? "INCOME" : "EXPENSE";
        String source = "Cash";

        if(text.contains("card")
                || text.contains("visa")
                || text.contains("mastercard")){

            source = "Card";
        }

        String category = "Other";
        for(String word : text.split(" ")){
            word = word.replaceAll("[^a-z]","");
            if(!word.isEmpty()
                    && !word.equals("spent")
                    && !word.equals("on")
                    && !word.equals("for")
                    && !word.equals("income")
                    && !word.equals("expense")
                    && !word.equals("cash")
                    && !word.equals("card")){
                category = word;
                break;
            }
        }

        category = category.substring(0,1).toUpperCase() + category.substring(1);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user == null){
            Toast.makeText(
                    this,
                    "User not found",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String userId = user.getUid();

        TransactionEntity transaction =
                new TransactionEntity(
                        amount,
                        category,
                        type,
                        System.currentTimeMillis(),
                        "",
                        userId,
                        source,
                        "🎤",
                        source
                );

        transaction.currency = source.equals("Cash") ? "AMD ֏" : "AMD ֏";

        new Thread(() -> {
            transactionDao.insert(transaction);

            FirebaseFirestore.getInstance()
                    .collection("transactions")
                    .add(transaction);

            runOnUiThread(() -> {
                calculateAndUpdateBalance();

                Toast.makeText(
                        this,
                        "Added",
                        Toast.LENGTH_SHORT
                ).show();
            });

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
        ArrayList<Double> amounts = new ArrayList<>();
        String[] lines = text.split("\n");
        for(String line : lines){
            double value = extractNumberFromLine(line);

            if(value > 20 && value < 1000000 && !amounts.contains(value)){
                amounts.add(value);
            }
        }
        Collections.sort(amounts, Collections.reverseOrder());
        if(amounts.isEmpty()){
            Toast.makeText(
                    this,
                    "Amount not found",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        showAmountSelection(amounts);
    }


    private void showAmountSelection(ArrayList<Double> amounts){
        String[] items = new String[amounts.size()];
        for(int i=0;i<amounts.size();i++){
            items[i]=String.format(
                    "%.2f",
                    amounts.get(i)
            );
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose amount 💰")
                .setItems(items,
                        (dialog,which)->{
                            double selectedAmount = amounts.get(which);
                            showDetectedAmount(selectedAmount);
                        })
                .show();
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
                        "com.example.moneytrack.fileprovider",
                        photoFile
                );

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
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
        rbCash.setId(View.generateViewId());
        rbCash.setChecked(true);

        RadioButton rbCard = new RadioButton(this);
        rbCard.setText("Card");
        rbCard.setId(View.generateViewId());
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
                .setPositiveButton("Add",(d,w)->{

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(user == null){
                        Toast.makeText(
                                this,
                                "User not found",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String userId = user.getUid();
                    TransactionEntity transaction =
                            new TransactionEntity(
                                    amount,
                                    "Receipt",
                                    "EXPENSE",
                                    System.currentTimeMillis(),
                                    "",
                                    userId,
                                    selectedSource[0],
                                    "🧾",
                                    selectedSource[0]
                            );

                    transaction.currency =
                            selectedSource[0].equals("Cash")
                                    ? getSharedPreferences("settings", MODE_PRIVATE
                            ).getString("currency", "AMD ֏"
                            ) : getSharedPreferences("settings", MODE_PRIVATE
                            ).getString("currency", "AMD ֏");

                    new Thread(() -> {
                        transactionDao.insert(transaction);
                        FirebaseFirestore.getInstance()
                                .collection("transactions")
                                .add(transaction);

                        runOnUiThread(() -> {
                            calculateAndUpdateBalance();
                            Toast.makeText(
                                    this,
                                    "Added",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}