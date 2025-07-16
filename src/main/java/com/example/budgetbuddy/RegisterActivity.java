package com.example.budgetbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEt, passwordEt, confirmPasswordEt;
    private Button registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        emailEt = findViewById(R.id.emailEditText);
        passwordEt = findViewById(R.id.passwordEditText);
        confirmPasswordEt = findViewById(R.id.confirmPasswordEditText);
        registerBtn = findViewById(R.id.registerButton);

        registerBtn.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String email = emailEt.getText() != null ? emailEt.getText().toString().trim() : "";
        String pass = passwordEt.getText() != null ? passwordEt.getText().toString().trim() : "";
        String confirmPass = confirmPasswordEt.getText() != null ? confirmPasswordEt.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
