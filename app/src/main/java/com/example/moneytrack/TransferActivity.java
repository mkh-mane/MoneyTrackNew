package com.example.moneytrack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);

        etFromAmount = findViewById(R.id.etFromAmount);
        etToAmount = findViewById(R.id.etToAmount);

        btnTransferDone = findViewById(R.id.btnTransferDone);

        tvFromCurrency = findViewById(R.id.tvFromCurrency);
        tvToCurrency = findViewById(R.id.tvToCurrency);

        database = AppDatabase.getInstance(this);

        accountDao = database.accountDao();
        transactionDao = database.transactionDao();

        loadAccounts();

        new Handler().postDelayed(() -> {
            if(spinnerFrom.getSelectedItem() != null){
                String account = spinnerFrom.getSelectedItem().toString();
                tvFromCurrency.setText(getAccountCurrency(account));
            }

            if(spinnerTo.getSelectedItem() != null){
                String account =
                        spinnerTo.getSelectedItem().toString();
                tvToCurrency.setText(getAccountCurrency(account));
            }
            updateConvertedAmount();
        },300);


        spinnerFrom.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        String account = spinnerFrom.getSelectedItem().toString();
                        tvFromCurrency.setText(getAccountCurrency(account));
                        updateConvertedAmount();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {}
                });


        spinnerTo.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        String account = spinnerTo.getSelectedItem().toString();
                        tvToCurrency.setText(getAccountCurrency(account));
                        updateConvertedAmount();
                    }
                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {}
                });



        etFromAmount.addTextChangedListener(new TextWatcher() {
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

                if(isUpdating) return;
                try{
                    isUpdating = true;
                    if(s.toString().trim().isEmpty()){
                        etToAmount.setText("");
                    }
                    else{
                        double amount = Double.parseDouble(s.toString());

                        String fromCurrency =
                                getAccountCurrency(
                                        spinnerFrom.getSelectedItem().toString()
                                );

                        String toCurrency =
                                getAccountCurrency(
                                        spinnerTo.getSelectedItem().toString()
                                );

                        double converted =
                                CurrencyUtils.convertBetweenAccounts(
                                        amount,
                                        fromCurrency,
                                        toCurrency
                                );

                        etToAmount.setText(
                                String.format(
                                        "%.2f",
                                        converted
                                )
                        );
                    }

                    isUpdating = false;
                }catch(Exception e){
                    isUpdating = false;
                }
            }

            @Override
            public void afterTextChanged(
                    Editable s
            ) {}
        });


        etToAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(isUpdating) return;
                try{
                    isUpdating = true;
                    if(s.toString().trim().isEmpty()){
                        etFromAmount.setText("");
                    }
                    else{
                        double amount = Double.parseDouble(s.toString());
                        String fromCurrency = getAccountCurrency(spinnerTo.getSelectedItem().toString());
                        String toCurrency = getAccountCurrency(spinnerFrom.getSelectedItem().toString());
                        double converted =
                                CurrencyUtils.convertBetweenAccounts(
                                        amount,
                                        fromCurrency,
                                        toCurrency
                                );

                        etFromAmount.setText(
                                String.format(
                                        "%.2f",
                                        converted
                                )
                        );
                    }

                    isUpdating = false;

                }catch(Exception e){
                    isUpdating = false;
                }
            }

            @Override
            public void afterTextChanged(
                    Editable s
            ) {}
        });



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

            String fromAccount = spinnerFrom.getSelectedItem().toString();
            String toAccount = spinnerTo.getSelectedItem().toString();

            if(fromAccount.equals(toAccount)){
                Toast.makeText(
                        this,
                        "Choose different accounts",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            double fromAmount = Double.parseDouble(value);
            String fromCurrency = getAccountCurrency(fromAccount);
            String toCurrency = getAccountCurrency(toAccount);


            double convertedAmount =
                    CurrencyUtils.convertBetweenAccounts(
                            fromAmount,
                            fromCurrency,
                            toCurrency
                    );

            String userId =
                    FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();

            TransactionEntity expense = new TransactionEntity(
                    fromAmount,
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


            TransactionEntity income = new TransactionEntity(
                    convertedAmount,
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



    private void loadAccounts(){
        new Thread(() -> {
            accountNames.clear();
            currencyMap.clear();

            accountNames.add("Cash");
            accountNames.add("Card");
            currencyMap.put("Cash", "AMD ֏");
            currencyMap.put("Card", "AMD ֏");

            List<AccountEntity> accounts = accountDao.getAllAccounts();
            for(AccountEntity account : accounts){
                accountNames.add(account.name);
                currencyMap.put(
                        account.name,
                        account.currency
                );
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter =
                        new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                accountNames
                        );

                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );

                spinnerFrom.setAdapter(adapter);
                spinnerTo.setAdapter(adapter);

                if(accountNames.size() > 0){

                    tvFromCurrency.setText(
                            getAccountCurrency(
                                    accountNames.get(0)
                            )
                    );

                    tvToCurrency.setText(
                            getAccountCurrency(
                                    accountNames.get(0)
                            )
                    );
                }

            });
        }).start();
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



    private void updateConvertedAmount(){
        String value =
                etFromAmount.getText()
                        .toString()
                        .trim();

        if(value.isEmpty()){
            etToAmount.setText("");
            return;
        }

        try{

            double amount = Double.parseDouble(value);

            String fromCurrency =
                    currencyMap.get(spinnerFrom.getSelectedItem().toString());

            String toCurrency = currencyMap.get(spinnerTo.getSelectedItem().toString());

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