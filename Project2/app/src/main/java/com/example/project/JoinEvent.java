package com.example.project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JoinEvent extends Fragment {

    private String link, eventId, title, location, startDate, startTime;
    private Button btnDone;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_join_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            link = getArguments().getString("link");
            eventId = getArguments().getString("eventId");
            title = getArguments().getString("title");
            location = getArguments().getString("location");
            startDate = getArguments().getString("startDate");
            startTime = getArguments().getString("startTime");
        }

        TextView tvLink = view.findViewById(R.id.tvJoinLink);
        tvLink.setText(link);

        // --- NEW: Make the link clickable manually ---
        tvLink.setOnClickListener(v -> {
            if (link != null && !link.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            }
        });
        // ---------------------------------------------

        btnDone = view.findViewById(R.id.btnDone);
        View btnBack = view.findViewById(R.id.back_button);

        // Back Button Logic: returns to previous fragment (Event Details)
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // 1. Auto Redirect Logic
        new Handler().postDelayed(() -> {
            if (getContext() != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            }
        }, 2000); // 2 seconds delay

        // 2. Done Button Logic
        btnDone.setOnClickListener(v -> {
            saveToHistoryAndCalendar();
        });
    }

    private void saveToHistoryAndCalendar() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";

        Map<String, Object> joinData = new HashMap<>();
        joinData.put("userId", uid);
        joinData.put("eventId", eventId);
        joinData.put("title", title);
        joinData.put("timestamp", FieldValue.serverTimestamp());
        joinData.put("status", "Joined");

        // Save to History (Registrations collection)
        db.collection("registrations").add(joinData)
                .addOnSuccessListener(docRef -> {
                    addToCalendar(); // Open Calendar
                    navigateToDone(); // Go to Done Page
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to join", Toast.LENGTH_SHORT).show());
    }

    private void addToCalendar() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(startDate + " " + startTime);
            long startMillis = date.getTime();
            long endMillis = startMillis + (60 * 60 * 1000); // Default 1 hour duration

            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.Events.TITLE, title);
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location);
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToDone() {
        Done doneFragment = new Done();
        doneFragment.setArguments(getArguments()); // Pass event details to Done page

        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, doneFragment)
                // Do NOT add to backstack so they can't go back to Join page
                .commit();
    }
}