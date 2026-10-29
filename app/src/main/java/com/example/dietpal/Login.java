package com.example.dietpal;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends AppCompatActivity {
    // Declare variables
    EditText edUser, edPass;
    Button btnLog, btnReg;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize variables with corresponding views
        edUser = findViewById(R.id.editText);
        edPass = findViewById(R.id.editPassword);
        btnLog = findViewById(R.id.LoginButton);
        btnReg = findViewById(R.id.RegBtn);
        databaseHelper = new DatabaseHelper(this);

        // Set onClickListener for the login button
        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the entered username and password
                String username = edUser.getText().toString();
                String password = edPass.getText().toString();

                // Check for empty username
                if (username.length() == 0) {
                    Toast.makeText(getApplicationContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                }
                // Check for empty password
                else if (password.length() == 0) {
                    Toast.makeText(getApplicationContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
                }
                // If both username and password are entered, check for valid login
                else {
                    if (databaseHelper.checkUser(username, password)) {
                        startActivity(new Intent(Login.this, MainActivity.class));
                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout,new HomeFragment()).commit();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Invalid Username or Password", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Set onClickListener for the register button
        btnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, Register.class));
            }
        });
    }


}
