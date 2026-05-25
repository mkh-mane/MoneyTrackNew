package com.example.moneytrack;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moneytrack.data.db.AccountDao;
import com.example.moneytrack.data.db.AccountEntity;
import com.example.moneytrack.data.db.AppDatabase;
import com.example.moneytrack.data.db.TransactionDao;
import com.example.moneytrack.data.db.TransactionEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TransferActivity extends AppCompatActivity {
    private AppDatabase database;
    private AccountDao accountDao;
    private TransactionDao transactionDao;
    private Spinner spinnerFrom, spinnerTo;
    private EditText etFromAmount, etToAmount;
    private Button btnTransferDone;
    private TextView tvFromCurrency, tvToCurrency;
    boolean isUpdating = false;
    ArrayList<String> accountNames = new ArrayList<>();
    HashMap<String,String> currencyMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        database = AppDatabase.getInstance(this);

        accountDao = database.accountDao();
        transactionDao = database.transactionDao();

        LinearLayout srcCashFrom = findViewById(R.id.srcCashFrom);

        LinearLayout srcCardFrom = findViewById(R.id.srcCardFrom);

        LinearLayout srcCashTo = findViewById(R.id.srcCashTo);

        LinearLayout srcCardTo = findViewById(R.id.srcCardTo);

        LinearLayout containerFromAccounts = findViewById(R.id.containerFromAccounts);

        LinearLayout containerToAccounts =
                findViewById(R.id.containerToAccounts);

        etFromAmount =
                findViewById(R.id.etFromAmount);

        etToAmount =
                findViewById(R.id.etToAmount);

        btnTransferDone =
                findViewById(R.id.btnTransferDone);

        tvFromCurrency =
                findViewById(R.id.tvFromCurrency);

        tvToCurrency =
                findViewById(R.id.tvToCurrency);

        final String[] selectedFrom = {"Cash"};
        final String[] selectedTo = {"Card"};


        View[] fromViews = {
                srcCashFrom,
                srcCardFrom
        };

        View[] toViews = {
                srcCashTo,
                srcCardTo
        };

        tvFromCurrency.setText(
                getAccountCurrency(
                        selectedFrom[0]
                )
        );

        tvToCurrency.setText(
                getAccountCurrency(
                        selectedTo[0]
                )
        );


        srcCashFrom.setOnClickListener(v -> {
            selectedFrom[0]="Cash";
            clearAccountSelection(containerFromAccounts, fromViews);
            ImageView img = (ImageView) srcCashFrom.getChildAt(0);
            img.setBackgroundResource(R.drawable.selected_background);
            tvFromCurrency.setText(getAccountCurrency("Cash"));
            updateConvertedAmount(
                    selectedFrom[0],
                    selectedTo[0]
            );

        });


        srcCardFrom.setOnClickListener(v -> {
            selectedFrom[0] = "Card";
            clearAccountSelection(
                    containerFromAccounts,
                    fromViews
            );
            ImageView img = (ImageView) srcCardFrom.getChildAt(0);
            img.setBackgroundResource(R.drawable.selected_background);
            tvFromCurrency.setText(getAccountCurrency("Card"));

            updateConvertedAmount(
                    selectedFrom[0],
                    selectedTo[0]
            );

        });


        srcCashTo.setOnClickListener(v -> {
            selectedTo[0] = "Cash";
            clearAccountSelection(containerToAccounts, toViews);
            ImageView img = (ImageView) srcCashTo.getChildAt(0);
            img.setBackgroundResource(R.drawable.selected_background);
            tvToCurrency.setText(getAccountCurrency("Cash"));
            updateConvertedAmount(selectedFrom[0], selectedTo[0]);
        });


        srcCardTo.setOnClickListener(v -> {

            selectedTo[0] = "Card";

            clearAccountSelection(containerToAccounts, toViews);
            ImageView img = (ImageView) srcCardTo.getChildAt(0);
            img.setBackgroundResource(R.drawable.selected_background);
            tvToCurrency.setText(getAccountCurrency("Card"));
            updateConvertedAmount(selectedFrom[0], selectedTo[0]);

        });


        // Load custom accounts

        loadAccounts(
                containerFromAccounts,
                containerToAccounts,
                selectedFrom,
                selectedTo,
                fromViews,
                toViews
        );


        etFromAmount.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        updateConvertedAmount(
                                selectedFrom[0],
                                selectedTo[0]
                        );

                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {}
                }
        );


        btnTransferDone.setOnClickListener(v -> {

            String value = etFromAmount.getText().toString().trim();

            if(value.isEmpty()){
                Toast.makeText(
                        this,
                        "Enter amount",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String fromAccount = selectedFrom[0];
            String toAccount = selectedTo[0];

            if(fromAccount.equals(toAccount)){
                Toast.makeText(
                        this,
                        "Choose different accounts",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            double amount = Double.parseDouble(value);
            String fromCurrency = getAccountCurrency(fromAccount);
            String toCurrency = getAccountCurrency(toAccount);

            double converted = CurrencyUtils.convertBetweenAccounts(
                            amount,
                            fromCurrency,
                            toCurrency
                    );

            String userId =
                    FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();

            TransactionEntity expense =
                    new TransactionEntity(
                            amount,
                            "Transfer Out",
                            "EXPENSE",
                            System.currentTimeMillis(),
                            "",
                            userId,
                            fromAccount,
                            "⬆",
                            fromAccount,
                            fromCurrency
                    );

            expense.currency = fromCurrency;

            TransactionEntity income =
                    new TransactionEntity(
                            converted,
                            "Transfer In",
                            "INCOME",
                            System.currentTimeMillis(),
                            "",
                            userId,
                            toAccount,
                            "⬇",
                            toAccount,
                            toCurrency
                    );

            income.currency = toCurrency;

            new Thread(() -> {
                transactionDao.insert(expense);
                transactionDao.insert(income);
                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            "Transfer completed",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                });

            }).start();

        });

    }



    private void loadAccounts(
            LinearLayout containerFromAccounts,
            LinearLayout containerToAccounts,
            String[] selectedFrom,
            String[] selectedTo,
            View[] fromViews,
            View[] toViews
    ){

        new Thread(() -> {

            List<AccountEntity> accounts =
                    accountDao.getAllAccounts();

            runOnUiThread(() -> {

                containerFromAccounts.removeAllViews();
                containerToAccounts.removeAllViews();

                currencyMap.clear();


                for(AccountEntity account : accounts){

                    // currency պահում ենք
                    currencyMap.put(
                            account.name,
                            account.currency
                    );

                    // FROM
                    LinearLayout fromItem =
                            createAccountItem(
                                    account.name
                            );

                    fromItem.setOnClickListener(v -> {

                        selectedFrom[0] =
                                account.name;

                        clearAccountSelection(
                                containerFromAccounts,
                                fromViews
                        );

                        ImageView img =
                                (ImageView)
                                        fromItem.getChildAt(0);

                        img.setBackgroundResource(
                                R.drawable.selected_background
                        );

                        tvFromCurrency.setText(
                                account.currency
                        );

                        updateConvertedAmount(
                                selectedFrom[0],
                                selectedTo[0]
                        );

                    });


                    // TO
                    LinearLayout toItem =
                            createAccountItem(
                                    account.name
                            );

                    toItem.setOnClickListener(v -> {

                        selectedTo[0] =
                                account.name;

                        clearAccountSelection(
                                containerToAccounts,
                                toViews
                        );

                        ImageView img =
                                (ImageView)
                                        toItem.getChildAt(0);

                        img.setBackgroundResource(
                                R.drawable.selected_background
                        );

                        tvToCurrency.setText(
                                account.currency
                        );

                        updateConvertedAmount(
                                selectedFrom[0],
                                selectedTo[0]
                        );

                    });

                    containerFromAccounts.addView(
                            fromItem
                    );

                    containerToAccounts.addView(
                            toItem
                    );

                }

            });

        }).start();
    }

    private void highlightSelected(View selected, View[] all) {
        for (View v : all) {
            v.setBackgroundResource(android.R.color.transparent);
        }
        selected.setBackgroundResource(R.drawable.selected_background);
    }


    private LinearLayout createAccountItem(
            String accountName
    ){

        LinearLayout item =
                new LinearLayout(this);

        item.setOrientation(
                LinearLayout.VERTICAL
        );

        item.setGravity(
                Gravity.CENTER
        );

        item.setPadding(
                4,4,4,4
        );

        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        220,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        item.setLayoutParams(
                itemParams
        );


        ImageView image =
                new ImageView(this);

        image.setImageResource(
                R.drawable.ic_wallet
        );

        image.setBackgroundResource(
                R.drawable.bg_circle
        );

        image.setPadding(
                10,
                10,
                10,
                10
        );

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        130,
                        130
                );

        image.setLayoutParams(imageParams);


        TextView text =
                new TextView(this);

        text.setText(accountName);

        text.setTextSize(12);

        text.setTextColor(
                Color.parseColor("#4A4A4A")
        );

        text.setGravity(
                Gravity.CENTER
        );

        text.setPadding(
                0,
                6,
                0,
                0
        );


        item.addView(image);
        item.addView(text);

        return item;
    }



    private void clearAccountSelection(
            LinearLayout container,
            View[] staticViews
    ){

        // custom account-ներ
        int count = container.getChildCount();

        for(int i=0;i<count;i++){

            LinearLayout item =
                    (LinearLayout)
                            container.getChildAt(i);

            ImageView image =
                    (ImageView)
                            item.getChildAt(0);

            image.setBackgroundResource(
                    R.drawable.bg_circle
            );
        }

        // Cash/Card
        for(View v : staticViews){
            ImageView img =
                    (ImageView)
                            ((LinearLayout)v)
                                    .getChildAt(0);

            img.setBackgroundResource(
                    R.drawable.bg_circle
            );
        }
    }

    private String getAccountCurrency(String accountName){
        if(accountName.equals("Cash") || accountName.equals("Card")){
            SharedPreferences prefs =
                    getSharedPreferences(
                            "settings",
                            MODE_PRIVATE
                    );
            return prefs.getString(
                    "currency",
                    "AMD ֏"
            );
        }

        String currency = currencyMap.get(accountName);
        if(currency == null)
            return "AMD ֏";
        return currency;
    }



    private void updateConvertedAmount(String fromAccount, String toAccount){
        String value = etFromAmount.getText().toString().trim();

        if(value.isEmpty()){
            etToAmount.setText("");
            return;
        }

        try{
            double amount = Double.parseDouble(value);

            String fromCurrency = getAccountCurrency(fromAccount);
            String toCurrency = getAccountCurrency(toAccount);

            double converted = CurrencyUtils.convertBetweenAccounts(
                            amount,
                            fromCurrency,
                            toCurrency
                    );

            etToAmount.setText(String.format("%.2f", converted)
            );

        }
        catch(Exception e){
            etToAmount.setText("");

        }
    }
}