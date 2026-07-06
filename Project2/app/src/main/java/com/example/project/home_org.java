package com.example.project;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class home_org extends Fragment {

    private CardView cvEventManager; // Ensure ID exists in XML if used
    private Button btnCreateEvent;

    // Stats TextViews
    private TextView tvCountTotal, tvCountPending, tvCountApproved, tvCountRejected;
    private TextView welcomeText;

    // Profile Image
    private ImageView profileImage;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public home_org() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_org, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize Views
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        // If 'btnManageEvent' doesn't exist in XML, remove this line to prevent crash
        cvEventManager = view.findViewById(R.id.btnManageEvent);

        tvCountTotal = view.findViewById(R.id.tvCountTotal);
        tvCountPending = view.findViewById(R.id.tvCountPending);
        tvCountApproved = view.findViewById(R.id.tvCountApproved);
        tvCountRejected = view.findViewById(R.id.tvCountRejected);
        welcomeText = view.findViewById(R.id.welcomeText);

        // Find Profile Image View
        profileImage = view.findViewById(R.id.profileImage);

        // Button Logic
        btnCreateEvent.setOnClickListener(v -> {
            animateButton(v);
            navigateToEventForm();
        });

        if (cvEventManager != null) {
            cvEventManager.setOnClickListener(v -> {
                animateButton(v);
                navigateToEventManager();
            });
        }

        // Load Data
        loadUserData();
        loadDashboardStats();

        return view;
    }

    private void animateButton(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Fetch User Details from Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("username");
                            String base64Image = document.getString("profileImage");

                            // Set Name
                            if (name != null && !name.isEmpty()) {
                                welcomeText.setText("Welcome Back, " + name + "!");
                            } else {
                                welcomeText.setText("Welcome Back!");
                            }

                            // Set Profile Image
                            if (base64Image != null && !base64Image.isEmpty()) {
                                decodeBase64AndSetImage(base64Image, profileImage);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure (optional)
                    });
        }
    }

    // Helper to decode Base64 string to Bitmap
    private void decodeBase64AndSetImage(String base64, ImageView imageView) {
        try {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.profile); // Fallback if decode fails
        }
    }

    private void loadDashboardStats() {
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();

        // Get all events created by this specific Organizer
        db.collection("events")
                .whereEqualTo("uid", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        // Toast.makeText(getContext(), "Error loading stats", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        int total = snapshots.size();
                        int pending = 0;
                        int approved = 0;
                        int rejected = 0;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String status = doc.getString("status");
                            if (status != null) {
                                switch (status) {
                                    case "Pending":
                                        pending++;
                                        break;
                                    case "Approved":
                                        approved++;
                                        break;
                                    case "Rejected":
                                        rejected++;
                                        break;
                                }
                            }
                        }

                        // Update UI
                        if(tvCountTotal != null) tvCountTotal.setText(String.valueOf(total));
                        if(tvCountPending != null) tvCountPending.setText(String.valueOf(pending));
                        if(tvCountApproved != null) tvCountApproved.setText(String.valueOf(approved));
                        if(tvCountRejected != null) tvCountRejected.setText(String.valueOf(rejected));
                    }
                });
    }

    private void navigateToEventForm() {
        EventForm eventFormFragment = new EventForm();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentView, eventFormFragment);
        transaction.addToBackStack("eventForm");
        transaction.commit();
    }

    private void navigateToEventManager() {
        Event_manager eventManagerFragment = new Event_manager();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentView, eventManagerFragment);
        transaction.addToBackStack("eventManager");
        transaction.commit();
    }
}