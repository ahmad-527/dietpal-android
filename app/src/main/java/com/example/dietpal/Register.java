package com.example.dietpal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Register extends AppCompatActivity {

    // Declare variables
    EditText edUser, edPass, edPass2, edEmail;
    Button btnLogin, btnRegister;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize variables with corresponding views
        edUser = findViewById(R.id.UsrReg);
        edEmail = findViewById(R.id.editEmail);
        edPass = findViewById(R.id.editPassword);
        edPass2 = findViewById(R.id.editPassword2);
        btnLogin = findViewById(R.id.LogBtn);
        btnRegister = findViewById(R.id.RegisterButton1);

        // Initialize the database helper
        dbHelper = new DatabaseHelper(this);

        // Set onClickListener for the login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Register.this, Login.class));
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get values from EditText fields
                String username = edUser.getText().toString();
                String email = edEmail.getText().toString();
                String password = edPass.getText().toString();
                String password2 = edPass2.getText().toString();

                // Check for empty fields and password match
                if (username.length() == 0 || email.length() == 0 || password.length() == 0 || password2.length() == 0) {
                    Toast.makeText(getApplicationContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(getApplicationContext(), "Enter a valid email address", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(password2)) {
                    Toast.makeText(getApplicationContext(), "Password and Confirm Password DO NOT MATCH", Toast.LENGTH_SHORT).show();
                } else if (dbHelper.checkUsernameExists(username)) {
                    Toast.makeText(getApplicationContext(), "Username already exists", Toast.LENGTH_SHORT).show();
                } else {
                    // Insert user data into the database
                    boolean isInserted = dbHelper.insertData(username, email, password);
                    if (isInserted) {
                        Toast.makeText(getApplicationContext(), "Registration Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Register.this, Login.class));
                    } else {
                        Toast.makeText(getApplicationContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
