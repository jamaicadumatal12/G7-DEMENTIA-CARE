package com.example.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dashboard.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;

public class RegisterActivity extends AppCompatActivity {
    private EditText editName, editEmail, editAddress, editPassword, editConfirmPassword;
    private MaterialButton buttonRegister;
    private TextView textLogin;
    private ProgressBar registerProgress;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editAddress = findViewById(R.id.edit_address);
        editPassword = findViewById(R.id.edit_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        buttonRegister = findViewById(R.id.button_register);
        textLogin = findViewById(R.id.text_login);
        registerProgress = findViewById(R.id.registerProgress);

        buttonRegister.setOnClickListener(v -> registerUser());
        textLogin.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            buttonRegister.setEnabled(false);
            buttonRegister.setText("");
            registerProgress.setVisibility(View.VISIBLE);
        } else {
            buttonRegister.setEnabled(true);
            buttonRegister.setText(R.string.register);
            registerProgress.setVisibility(View.GONE);
        }
    }

    private void registerUser() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || address.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        FirebaseHelper.registerUser(email, password, name, address, new FirebaseHelper.OnAuthListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setLoading(false);
                // Show verification email sent dialog
                new AlertDialog.Builder(RegisterActivity.this)
                    .setTitle("Verify Your Email")
                    .setMessage("A verification email has been sent to " + email + ". Please verify your email before logging in.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    })
                    .setCancelable(false)
                    .show();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Registration failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
