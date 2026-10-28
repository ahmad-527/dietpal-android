package com.example.dietpal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Register extends AppCompatActivity {
    Executor authExecutor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_register);
        EditText user = findViewById(R.id.UsrReg);
        EditText email = findViewById(R.id.editEmail);
        EditText password = findViewById(R.id.editPassword);
        EditText confirmation = findViewById(R.id.editPassword2);
        Button register = findViewById(R.id.RegisterButton1);
        findViewById(R.id.LogBtn).setOnClickListener(v -> startActivity(new Intent(this, Login.class)));
        register.setOnClickListener(v -> {
            String username = user.getText().toString().trim();
            String address = email.getText().toString().trim();
            String secret = password.getText().toString();
            String repeated = confirmation.getText().toString();
            String error = null;
            if (username.isEmpty() || address.isEmpty() || secret.isEmpty() || repeated.isEmpty()) error = "All fields are required";
            else if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) error = "Enter a valid email address";
            else if (secret.length() < 8 || secret.length() > 256) error = "Use a password between 8 and 256 characters";
            else if (!secret.equals(repeated)) error = "Passwords do not match";
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
            register.setEnabled(false);
            authExecutor.execute(() -> {
                String message;
                boolean inserted = false;
                try (DatabaseHelper database = new DatabaseHelper(getApplicationContext())) {
                    if (database.checkUsernameExists(username)) message = "Username already exists";
                    else {
                        inserted = database.insertData(username, address, secret);
                        message = inserted ? "Registration successful" : "Registration failed";
                    }
                } catch (RuntimeException unavailable) {
                    message = "Registration is temporarily unavailable";
                }
                final String result = message;
                final boolean success = inserted;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    register.setEnabled(true);
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                    if (success) {
                        startActivity(new Intent(this, Login.class));
                        finish();
                    }
                });
            });
        });
    }

    @Override protected void onDestroy() {
        if (authExecutor instanceof ExecutorService) ((ExecutorService) authExecutor).shutdown();
        super.onDestroy();
    }
}
