package com.example.budgetbuddy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText emailEt, passwordEt;
    private Button   loginBtn, signupBtn;
    private TextView forgotTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        /* ── bind views ───────────────────────────────────────── */
        emailEt   = findViewById(R.id.emailEditText);
        passwordEt= findViewById(R.id.passwordEditText);
        loginBtn  = findViewById(R.id.loginButton);
        signupBtn = findViewById(R.id.signupButton);
        forgotTv  = findViewById(R.id.forgotPasswordText);

        /* ── LOGIN ───────────────────────────────────────────── */
        loginBtn.setOnClickListener(v -> attemptLogin());

        /* ── SIGN-UP (we’ll build RegisterActivity later) ───── */
        signupBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        /* ── FORGOT PASSWORD ────────────────────────────────── */
        forgotTv.setOnClickListener(v -> showForgotDialog());
    }

    private void attemptLogin() {
        String email = emailEt.getText().toString().trim();
        String pass  = passwordEt.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Email & password required", Toast.LENGTH_SHORT).show();
            return;
        }

        loginBtn.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    loginBtn.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = ((AuthResult) task.getResult()).getUser();
                        Toast.makeText(this, "Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();                // stop returning to login on back press
                    } else {
                        Toast.makeText(this,
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your email");
        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (d, w) -> {
            String mail = input.getText().toString().trim();
            if (TextUtils.isEmpty(mail)) {
                Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(mail)
                    .addOnCompleteListener(t -> {
                        if (t.isSuccessful())
                            Toast.makeText(this, "Reset link sent", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(this, t.getException().getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());
        builder.show();
    }

}
