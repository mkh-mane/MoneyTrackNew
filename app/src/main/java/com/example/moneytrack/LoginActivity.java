package com.example.moneytrack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        TextView forgotPassword = findViewById(R.id.forgotPassword);
        Button goToRegister = findViewById(R.id.goToRegister);

        forgotPassword.setOnClickListener(v -> {

            String email = emailEditText.getText().toString().trim();

            if (email.isEmpty()) {
                emailEditText.setError("Enter your email first");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                    "Reset link sent to your email",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        goToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        loginButton.setOnClickListener(v -> loginUser());
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {

            SharedPreferences prefs =
                    getSharedPreferences("MoneyTrackPrefs", MODE_PRIVATE);

            String savedPin = prefs.getString("user_pin", null);

            if (savedPin == null) {
                // PIN չկա → Create
                startActivity(new Intent(this, CreatePinActivity.class));
            } else {
                // PIN կա → Verify
                startActivity(new Intent(this, VerifyPinActivity.class));
            }

            finish();
        }
    }

    private void loginUser() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Enter password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        SharedPreferences prefs =
                                getSharedPreferences("MoneyTrackPrefs", MODE_PRIVATE);

                        String savedPin = prefs.getString("user_pin", null);

                        if (savedPin == null) {
                            startActivity(new Intent(this, CreatePinActivity.class));
                        } else {
                            startActivity(new Intent(this, VerifyPinActivity.class));
                        }

                        finish();

                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Authentication Failed: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}