package com.example.project;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class profile extends Fragment {

    private TextView tvName, tvUsername, tvEmail, tvUserId, tvJoinedCount;
    private Button btnChangePass, btnLogout, btnEditProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public profile() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvName = view.findViewById(R.id.tvName);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvJoinedCount = view.findViewById(R.id.tvJoinedCount);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnChangePass = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadUserProfile();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePass.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String authEmail = currentUser.getEmail();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String fullname = document.getString("fullName");
                        String name = document.getString("username");
                        String dbEmail = document.getString("email");

                        tvName.setText(fullname != null ? fullname : "No Full Name");
                        tvUsername.setText(name != null ? name : "No Username");

                        if (dbEmail != null && !dbEmail.isEmpty()) {
                            tvEmail.setText(dbEmail);
                        } else {
                            tvEmail.setText(authEmail != null ? authEmail : "No Email");
                        }

                        tvUserId.setText(userId);

                        loadJoinedEventsCount(userId);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // --- FIX: Count from 'registrations' collection ---
    private void loadJoinedEventsCount(String userId) {
        db.collection("registrations") // CHANGED from 'participants' to 'registrations'
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (tvJoinedCount != null) {
                        tvJoinedCount.setText("Events Joined: " + snapshots.size());
                    }
                });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile_part, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextInputEditText etName = dialogView.findViewById(R.id.etEditName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEditEmail);
        Button btnSave = dialogView.findViewById(R.id.btnSaveProfile);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEdit);
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseEdit);

        etName.setText(tvUsername.getText().toString());
        etEmail.setText(tvEmail.getText().toString());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newEmail)) {
                Toast.makeText(getContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            updateProfile(newName, newEmail, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateProfile(String newName, String newEmail, AlertDialog dialog) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Updating Profile...");
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newName);
        updates.put("email", newEmail);

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!newEmail.equals(user.getEmail())) {
                        user.updateEmail(newEmail)
                                .addOnSuccessListener(unused -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                                    loadUserProfile();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Updated info, but failed to update login email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    loadUserProfile();
                                    dialog.dismiss();
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                        loadUserProfile();
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextInputEditText etCurrentPass = dialogView.findViewById(R.id.etDialogCurrentPass);
        TextInputEditText etNewPass = dialogView.findViewById(R.id.etDialogNewPass);
        TextInputEditText etConfirmPass = dialogView.findViewById(R.id.etDialogConfirmPass);
        Button btnSavePass = dialogView.findViewById(R.id.btnDialogSave);

        Button btnCancelPass = dialogView.findViewById(R.id.btnDialogCancel);
        ImageView btnClosePass = dialogView.findViewById(R.id.btnCloseEditProfile);

        if (btnCancelPass != null) btnCancelPass.setOnClickListener(v -> dialog.dismiss());
        if (btnClosePass != null) btnClosePass.setOnClickListener(v -> dialog.dismiss());

        btnSavePass.setOnClickListener(v -> {
            String currentPass = etCurrentPass.getText().toString();
            String newPass = etNewPass.getText().toString();
            String confirmPass = etConfirmPass.getText().toString();

            if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass)) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            changePassword(currentPass, newPass, dialog);
        });

        dialog.show();
    }

    private void changePassword(String currentPass, String newPass, AlertDialog dialog) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(getContext(), "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Current password incorrect", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void logoutUser() {
        auth.signOut();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}