package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class Success_create_event extends Fragment {

    // Views
    private TextView tvSummaryTitle, tvSummaryLocation, tvSummaryDateTime, tvSummaryCapacity, tvCategoryBadge;
    private MaterialButton btnViewDetails, btnBackToDashboard; // XML ID: btnViewDetails

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_succes_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize ALL Views
        tvSummaryTitle = view.findViewById(R.id.tvSummaryTitle);
        tvSummaryLocation = view.findViewById(R.id.tvSummaryLocation);
        tvSummaryDateTime = view.findViewById(R.id.tvSummaryDateTime);
        tvSummaryCapacity = view.findViewById(R.id.tvSummaryCapacity);
        tvCategoryBadge = view.findViewById(R.id.tvCategoryBadge);

        // Corrected IDs based on your XML
        btnViewDetails = view.findViewById(R.id.btnViewDetails);
        btnBackToDashboard = view.findViewById(R.id.btnBackToDashboard);

        // 2. Get the Event ID passed from the previous page
        String currentEventId = null;
        if (getArguments() != null) {
            currentEventId = getArguments().getString("eventId");
            if (currentEventId != null) {
                fetchEventData(currentEventId);
            }
        }

        final String eventIdForNav = currentEventId;

        // 3. Setup Buttons Navigation

        // Button: View Event Details -> Redirects to Event_manager
        btnViewDetails.setOnClickListener(v -> {
            // Create the target fragment
            Fragment eventManagerFragment = new Event_manager();

            // Optional: Pass the event ID to the manager if needed
            if (eventIdForNav != null) {
                Bundle args = new Bundle();
                args.putString("eventId", eventIdForNav);
                eventManagerFragment.setArguments(args);
            }

            replaceFragment(eventManagerFragment);
        });

        // Button: Back to Dashboard -> Redirects to home_org
        btnBackToDashboard.setOnClickListener(v -> {
            replaceFragment(new home_org());
        });
    }

    // Helper method to switch fragments
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // IMPORTANT: Replace R.id.fragment_container with the actual ID of the FrameLayout in your Main Activity
        fragmentTransaction.replace(R.id.fragmentView, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void fetchEventData(String eventId) {
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get().addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // A. Get Data from Firestore
                        String title = document.getString("title");
                        String location = document.getString("location");
                        String startDate = document.getString("startDate");
                        String startTime = document.getString("startTime");
                        String quota = document.getString("quota");
                        String category = document.getString("category");

                        // B. Set Data to Views
                        if (title != null) tvSummaryTitle.setText(title);
                        if (location != null) tvSummaryLocation.setText(location);

                        // Combine Date and Time
                        String dateTime = (startDate != null ? startDate : "") + ", " + (startTime != null ? startTime : "");
                        tvSummaryDateTime.setText(dateTime);

                        if (quota != null) tvSummaryCapacity.setText(quota);
                        if (category != null) tvCategoryBadge.setText(category);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}