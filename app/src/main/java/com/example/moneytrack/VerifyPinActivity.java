package com.example.moneytrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class VerifyPinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_pin);

        EditText pinEditText = findViewById(R.id.pinEditText);
        Button verifyButton = findViewById(R.id.verifyPinButton);
        Button forgotButton = findViewById(R.id.forgotPinButton);

        SharedPreferences prefs = getSharedPreferences("MoneyTrackPrefs", MODE_PRIVATE);
        String savedPin = prefs.getString("user_pin", "");

        verifyButton.setOnClickListener(v -> {

            String enteredPin = pinEditText.getText().toString();

            if (enteredPin.equals(savedPin)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show();
            }
        });

        forgotButton.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            prefs.edit().remove("user_pin").apply();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}