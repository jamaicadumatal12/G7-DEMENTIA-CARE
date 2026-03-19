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
import android.util.Log;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
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
        
        // Initialize Firebase
        try {
            FirebaseHelper.initializeFirebase(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
        }

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

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
<<<<<<< HEAD

        // If user is already logged in, skip login screen and go directly to MainActivity
        try {
            if (sessionManager != null && sessionManager.isLoggedIn()
                    && FirebaseHelper.getCurrentUser() != null) {
                Log.d(TAG, "User already logged in, redirecting to MainActivity");
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking login session in onStart: " + e.getMessage());
        }
=======
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
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
        Log.d(TAG, "Attempting login for email: " + email);

        try {
            FirebaseHelper.loginUser(email, password, new FirebaseHelper.OnAuthListener() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    Log.d(TAG, "Login successful for user: " + user.getEmail());
                    runOnUiThread(() -> {
                        setLoading(false);
                        sessionManager.setLoggedIn(true, user.getUid(), user.getEmail());
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Login error: " + errorMessage);
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during login: " + e.getMessage());
            setLoading(false);
            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_LONG).show();
        }
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
}
