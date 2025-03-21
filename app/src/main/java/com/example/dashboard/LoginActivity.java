package com.example.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dashboard.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.example.dashboard.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import androidx.annotation.NonNull;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private TextView registerText, forgotPasswordText;
    private ProgressBar loginProgress;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.text_register);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        loginProgress = findViewById(R.id.loginProgress);

        loginButton.setOnClickListener(v -> handleLogin());
        registerText.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordText.setOnClickListener(v -> handleForgotPassword());
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loginButton.setEnabled(false);
            loginButton.setText("");
            loginProgress.setVisibility(View.VISIBLE);
        } else {
            loginButton.setEnabled(true);
            loginButton.setText(R.string.login);
            loginProgress.setVisibility(View.GONE);
        }
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        FirebaseHelper.loginUser(email, password, new FirebaseHelper.OnAuthListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setLoading(false);
                sessionManager.setLoggedIn(true, user.getUid(), user.getEmail());
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                if (errorMessage.contains("invalid") || errorMessage.contains("incorrect")) {
                    showError("Invalid email or password. Please try again.");
                } else if (errorMessage.contains("no user")) {
                    showError("No account found with this email. Please check your email or sign up.");
                } else {
                    showError("Unable to sign in. Please check your internet connection and try again.");
                }
                hideProgressBar();
            }
        });
    }

    private void handleForgotPassword() {
        String email = emailInput.getText().toString().trim();
        
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, 
                        "Password reset email sent to " + email, 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, 
                        "Failed to send reset email. Please check your email address.", 
                        Toast.LENGTH_LONG).show();
                }
            });
    }

    private void hideProgressBar() {
        setLoading(false);
    }

    private void showError(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
