package com.example.moneytrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvEmail, btnLogout, btnChangePin;
    Spinner spinnerCurrency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvEmail = findViewById(R.id.tvEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangePin = findViewById(R.id.btnChangePin);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            tvEmail.setText(user.getEmail());
        } else {
            tvEmail.setText("No user");
        }
        btnLogout.setOnClickListener(v -> logout());
        btnChangePin.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePinActivity.class));
        });
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                findViewById(R.id.bottomNav);

        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_analyze) {
                startActivity(new Intent(this, AnalyzeActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_profile) {
                return true;
            }

            return false;
        });

        spinnerCurrency = findViewById(R.id.spinnerCurrency);
        String[] currencies = {"AMD ֏", "USD $", "EUR €", "RUB ₽"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                currencies
        );
        spinnerCurrency.setAdapter(adapter);


        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String savedCurrency = prefs.getString("currency", "AMD ֏");
        int position = adapter.getPosition(savedCurrency);
        spinnerCurrency.setSelection(position);


        spinnerCurrency.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {
                        String selected = spinnerCurrency.getSelectedItem().toString();
                        SharedPreferences.Editor editor =
                                getSharedPreferences("settings", MODE_PRIVATE)
                                        .edit();
                        editor.putString("currency", selected);
                        editor.apply();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }
    private void logout() {
        FirebaseAuth.getInstance().signOut();

        SharedPreferences prefs =
                getSharedPreferences("MoneyTrackPrefs", MODE_PRIVATE);

        prefs.edit()
                .remove("PIN_VERIFIED")
                .apply();

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}