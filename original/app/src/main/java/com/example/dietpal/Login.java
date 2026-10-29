package com.example.dietpal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Login extends AppCompatActivity {
  Executor authExecutor = Executors.newSingleThreadExecutor();

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_login);
    EditText usernameView = findViewById(R.id.editText);
    EditText passwordView = findViewById(R.id.editPassword);
    Button login = findViewById(R.id.LoginButton);
    findViewById(R.id.RegBtn)
        .setOnClickListener(v -> startActivity(new Intent(this, Register.class)));
    login.setOnClickListener(
        v -> {
          String username = usernameView.getText().toString().trim();
          String password = passwordView.getText().toString();
          if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter your username and password", Toast.LENGTH_SHORT).show();
            return;
          }
          login.setEnabled(false);
          authExecutor.execute(
              () -> {
                boolean accepted = false;
                boolean unavailable = false;
                try (DatabaseHelper database = new DatabaseHelper(getApplicationContext())) {
                  accepted = database.checkUser(username, password);
                } catch (RuntimeException error) {
                  unavailable = true;
                }
                final boolean valid = accepted;
                final boolean failed = unavailable;
                runOnUiThread(
                    () -> {
                      if (isFinishing() || isDestroyed()) return;
                      login.setEnabled(true);
                      if (valid) {
                        // MainActivity owns the fragment container and creates its home screen.
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                      } else {
                        Toast.makeText(
                                this,
                                failed
                                    ? "Sign-in is temporarily unavailable"
                                    : "Invalid username or password",
                                Toast.LENGTH_SHORT)
                            .show();
                      }
                    });
              });
        });
  }

  @Override
  protected void onDestroy() {
    if (authExecutor instanceof ExecutorService) ((ExecutorService) authExecutor).shutdown();
    super.onDestroy();
  }
}
