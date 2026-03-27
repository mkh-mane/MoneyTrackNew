package com.example.moneytrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CreatePinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pin);

        EditText pinEditText = findViewById(R.id.pinEditText);
        Button savePinButton = findViewById(R.id.savePinButton);

        savePinButton.setOnClickListener(v -> {

            String pin = pinEditText.getText().toString();

            if (pin.length() < 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            getSharedPreferences("MoneyTrackPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("user_pin", pin)
                    .apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}