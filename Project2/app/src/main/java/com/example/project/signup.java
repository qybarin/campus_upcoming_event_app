package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class signup extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Added 'etUsername' to the list of EditTexts
    private EditText etFullName, etUsername, etEmail, etPhone, etPassword, etConfirmPassword;
    private RadioGroup rgUserType;
    private Button btnSignup;
    private TextView tvLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etFullName = findViewById(R.id.et_full_name); // Capture Full Name
        etUsername = findViewById(R.id.et_username);   // Capture Username (New ID)
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        rgUserType = findViewById(R.id.rg_user_type);
        btnSignup = findViewById(R.id.btn_signup);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progressBar);

        // Login Redirect
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(signup.this, login.class));
            finish();
        });

        // Signup Button Logic
        btnSignup.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validations
            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Full Name is required");
                return;
            }
            if (TextUtils.isEmpty(username)) {
                etUsername.setError("Username is required");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Phone number is required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Password is required");
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Password must be >= 6 characters");
                return;
            }
            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }

            // Check User Role
            int selectedId = rgUserType.getCheckedRadioButtonId();
            final String role;
            if (selectedId != -1) {
                RadioButton selectedRadioButton = findViewById(selectedId);
                String btnText = selectedRadioButton.getText().toString();
                if (btnText.equalsIgnoreCase("Organizer")) {
                    role = "organizer";
                } else {
                    role = "participant";
                }
            } else {
                Toast.makeText(signup.this, "Please select a user type", Toast.LENGTH_SHORT).show();
                return;
            }

            // Proceed to Signup
            progressBar.setVisibility(View.VISIBLE);
            btnSignup.setEnabled(false);

            registerUser(fullName, username, email, phone, password, role);
        });
    }

    private void registerUser(String fullName, String username, String email, String phone, String password, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Pass both Full Name and Username to save method
                            saveUserToFirestore(user.getUid(), fullName, username, email, phone, role);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnSignup.setEnabled(true);
                        Toast.makeText(signup.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String fullName, String username, String email, String phone, String role) {
        Map<String, Object> userMap = new HashMap<>();

        // Save both distinct fields
        userMap.put("fullName", fullName);
        userMap.put("username", username);

        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(signup.this, "Account Created!", Toast.LENGTH_SHORT).show();
                    redirectToDashboard(role);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                    Toast.makeText(signup.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        if (role.equals("organizer")) {
            intent = new Intent(signup.this, Organizer.class);
        } else {
            intent = new Intent(signup.this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}