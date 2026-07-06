package com.example.project;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth; // Added Import
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrgInbox extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private FirebaseFirestore db;

    public OrgInbox() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewInbox);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        // 2. Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // 3. Load Data
        loadNotificationsFromFirestore();
    }

    private void loadNotificationsFromFirestore() {
        // Ensure user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please log in to view notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Filter notifications by the current user's ID
        db.collection("notifications")
                .whereEqualTo("organizerId", currentUserId) // Only show this user's notifications
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sort: Newest first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // A. Get Raw Data
                            String fullMessage = document.getString("message");
                            String title = document.getString("title");

                            // --- Timestamp Handling ---
                            Timestamp firestoreTs = document.getTimestamp("timestamp");
                            String formattedTime = "Just now";

                            if (firestoreTs != null) {
                                Date date = firestoreTs.toDate();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                                formattedTime = sdf.format(date);
                            }

                            // B. Parse Data
                            String adminName = "Admin";
                            String status = "Pending";
                            String eventName = "Unknown Event";
                            String comment = "No comment provided";

                            if (title != null) {
                                if (title.toLowerCase().contains("approved")) {
                                    status = "Approved";
                                } else if (title.toLowerCase().contains("rejected")) {
                                    status = "Rejected";
                                }
                            }

                            if (fullMessage != null) {
                                Pattern pattern = Pattern.compile("'(.*?)'");
                                Matcher matcher = pattern.matcher(fullMessage);
                                if (matcher.find()) {
                                    eventName = matcher.group(1);
                                }

                                if (fullMessage.contains("Comment:")) {
                                    comment = fullMessage.substring(fullMessage.indexOf("Comment:") + 8).trim();
                                }
                            }

                            // C. Add to List
                            notificationList.add(new NotificationModel(
                                    adminName,
                                    eventName,
                                    status,
                                    comment,
                                    formattedTime
                            ));

                        } catch (Exception e) {
                            Log.e("OrgInbox", "Error parsing notification: " + e.getMessage());
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        // NOTE: If this fails with "FAILED_PRECONDITION", check Logcat for the Index creation link.
                        Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("OrgInbox", "Error loading data", e);
                });
    }
}