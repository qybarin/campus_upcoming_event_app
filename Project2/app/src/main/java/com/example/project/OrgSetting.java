package com.example.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OrgSetting extends Fragment {

    // Views
    private TextView tvName, tvEmail, tvPhone;
    private ImageView ivProfile;
    private Button btnEditProfile, btnLogout;
    private LinearLayout rowChangePassword, rowChatUs, rowContactUs; // Added rows
    private Switch switchAll;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Variables for Editing
    private Uri newImageUri = null;
    private ImageView ivTempProfilePreview; // To show preview in popup
    private String currentBase64Image = "";

    public OrgSetting() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_org_setting, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        loadUserData();

        return view;
    }

    private void initializeViews(View view) {
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        ivProfile = view.findViewById(R.id.ivProfile);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        rowChangePassword = view.findViewById(R.id.rowChangePassword);

        // Find the new Rows (Make sure IDs match your XML)
        // Assuming you haven't added IDs yet, you need to add android:id="@+id/rowChatUs"
        // and android:id="@+id/rowContactUs" to the LinearLayouts in XML.
        // If they don't have IDs yet, update XML first or find them by child index (not recommended).
        // Based on previous XML, I will assume you will add these IDs.
        rowChatUs = view.findViewById(R.id.rowChatUs);
        rowContactUs = view.findViewById(R.id.rowContactUs);

        btnLogout = view.findViewById(R.id.btn_logout);

        // Listeners with Animation Feedback
        btnEditProfile.setOnClickListener(v -> {
            animateButton(v);
            showEditProfileDialog();
        });

        rowChangePassword.setOnClickListener(v -> {
            animateButton(v);
            showChangePasswordDialog();
        });

        if (rowChatUs != null) {
            rowChatUs.setOnClickListener(v -> {
                animateButton(v);
                openWhatsAppLink();
            });
        }

        if (rowContactUs != null) {
            rowContactUs.setOnClickListener(v -> {
                animateButton(v);
                openDialer();
            });
        }

        btnLogout.setOnClickListener(v -> {
            animateButton(v);
            showLogoutConfirmationDialog();
        });
    }

    // --- NEW: LINK FUNCTIONS ---
    private void openWhatsAppLink() {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.link/wozfrn"));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDialer() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+60172066180"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open dialer", Toast.LENGTH_SHORT).show();
        }
    }

    // --- NEW: ANIMATION FEEDBACK ---
    private void animateButton(View v) {
        v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    // --- 1. LOAD USER DATA ---
    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("username");
                        String email = document.getString("email");
                        String phone = document.getString("phone");
                        String base64Image = document.getString("profileImage");

                        tvName.setText(name != null ? name : "No Name");
                        tvEmail.setText(email != null ? email : user.getEmail());
                        tvPhone.setText(phone != null ? phone : "No Phone");

                        if (base64Image != null && !base64Image.isEmpty()) {
                            currentBase64Image = base64Image;
                            decodeBase64AndSetImage(base64Image, ivProfile);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // --- 2. EDIT PROFILE DIALOG ---
    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Init Dialog Views
        TextInputEditText etName = dialogView.findViewById(R.id.etEditName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEditEmail);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etEditPhone);
        ivTempProfilePreview = dialogView.findViewById(R.id.ivEditProfileImage);

        Button btnSave = dialogView.findViewById(R.id.btnSaveProfile);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEdit);

        // Close "X" Button
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseEditProfile);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Pre-fill Data
        etName.setText(tvName.getText().toString());
        etEmail.setText(tvEmail.getText().toString());
        etPhone.setText(tvPhone.getText().toString());
        if (!currentBase64Image.isEmpty()) {
            decodeBase64AndSetImage(currentBase64Image, ivTempProfilePreview);
        }

        // Image Click
        ivTempProfilePreview.setOnClickListener(v -> openImagePicker());

        // Save Click
        btnSave.setOnClickListener(v -> {
            animateButton(v); // Feedback on Save
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(getContext(), "Name and Email are required", Toast.LENGTH_SHORT).show();
                return;
            }

            updateProfile(newName, newEmail, newPhone, dialog);
        });

        btnCancel.setOnClickListener(v -> {
            animateButton(v);
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- 3. CHANGE PASSWORD DIALOG ---
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etNewPass = dialogView.findViewById(R.id.etDialogNewPass);
        TextInputEditText etOldPass = dialogView.findViewById(R.id.etDialogCurrentPass);
        TextInputEditText etConfirmPass = dialogView.findViewById(R.id.etDialogConfirmPass);
        Button btnUpdate = dialogView.findViewById(R.id.btnDialogSave);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);

        btnUpdate.setOnClickListener(v -> {
            // animateButton(v); // Uncomment if you have this method
            String oldPass = etOldPass.getText().toString().trim();
            String newPass = etNewPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            // 1. Basic Validations
            if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 chars", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Verify Old Password (Re-authentication)
            verifyAndUpdatePassword(oldPass, newPass, dialog);
        });

        btnCancel.setOnClickListener(v -> {
            // animateButton(v); // Uncomment if you have this method
            dialog.dismiss();
        });

        dialog.show();
    }

    // Helper method to handle Re-auth and Update
    private void verifyAndUpdatePassword(String oldPass, String newPass, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            // Create credentials from the OLD password entered
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);

            // Attempt to Re-authenticate
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // --- OLD PASSWORD IS CORRECT ---
                    // Now we can update to the new password
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // --- OLD PASSWORD IS WRONG ---
                    Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // --- LOGIC: UPDATE PROFILE ---
    private void updateProfile(String name, String email, String phone, AlertDialog dialog) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // 1. Process Image if changed
        String finalBase64Image = currentBase64Image;
        if (newImageUri != null) {
            finalBase64Image = encodeImageToBase64(newImageUri);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("profileImage", finalBase64Image);

        // 2. Update Firestore
        String finalBase64Image1 = finalBase64Image;
        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 3. Update Auth Email if changed
                    if (!email.equals(user.getEmail())) {
                        user.verifyBeforeUpdateEmail(email).addOnCompleteListener(task -> {
                            if (task.isSuccessful())
                                Toast.makeText(getContext(), "Verification email sent to new address", Toast.LENGTH_LONG).show();
                        });
                    }

                    // Refresh UI
                    tvName.setText(name);
                    tvEmail.setText(email);
                    tvPhone.setText(phone);
                    currentBase64Image = finalBase64Image1;
                    decodeBase64AndSetImage(finalBase64Image1, ivProfile);

                    Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    newImageUri = null; // Reset
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating profile", Toast.LENGTH_SHORT).show());
    }

    // --- LOGIC: UPDATE PASSWORD ---
    private void updatePassword(String newPass, AlertDialog dialog) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPass)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Password Changed", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // --- HELPERS: IMAGES ---
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 100);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == -1 && data != null && data.getData() != null) {
            newImageUri = data.getData();
            ivTempProfilePreview.setImageURI(newImageUri); // Show in popup
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            // Resize to save space
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            byte[] bytes = stream.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void decodeBase64AndSetImage(String base64, ImageView imageView) {
        try {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.profile); // Fallback
        }
    }

    private void showLogoutConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // 1. Sign out from Firebase
                    auth.signOut();

                    // 2. Navigate to Login Page
                    Intent intent = new Intent(getActivity(), login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    Toast.makeText(getContext(), "Logged Out Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }


}