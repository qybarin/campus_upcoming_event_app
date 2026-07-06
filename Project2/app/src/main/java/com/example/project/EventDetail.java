package com.example.project;

import android.app.AlertDialog; // Added Import
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetail extends Fragment {

    private Bundle args;
    private TextView tvDays, tvHours, tvMinutes, tvExpiredMessage;
    private Button btnJoin;
    private LinearLayout layoutCountdown, llTimerContainer;
    private ImageView ivPoster;
    private FirebaseFirestore db;

    public EventDetail() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        args = getArguments();

        if (args == null) return;

        // Init Views
        ivPoster = view.findViewById(R.id.ivEventDetailPoster);
        TextView tvCategory = view.findViewById(R.id.tvEventDetailCategory);
        TextView tvTitle = view.findViewById(R.id.tvEventDetailTitle);
        TextView tvOrganizer = view.findViewById(R.id.tvEventDetailOrganizer);
        TextView tvFee = view.findViewById(R.id.tvEventDetailFee);

        TextView tvDate = view.findViewById(R.id.tvDetailDateVal);
        TextView tvTime = view.findViewById(R.id.tvDetailTimeVal);
        TextView tvLocation = view.findViewById(R.id.tvDetailVenueVal);
        TextView tvQuota = view.findViewById(R.id.tvDetailQuotaVal);

        // Countdown Views
        layoutCountdown = view.findViewById(R.id.layoutCountdown);
        llTimerContainer = view.findViewById(R.id.llTimerContainer);
        tvExpiredMessage = view.findViewById(R.id.tvExpiredMessage);

        tvDays = view.findViewById(R.id.tvCountDays);
        tvHours = view.findViewById(R.id.tvCountHours);
        tvMinutes = view.findViewById(R.id.tvCountMinutes);

        btnJoin = view.findViewById(R.id.btnJoinEvent);
        View btnBack = view.findViewById(R.id.btnBackDetail);

        // Populate Text Data
        tvTitle.setText(args.getString("title"));
        tvCategory.setText(args.getString("category"));
        tvOrganizer.setText("By " + args.getString("organizer"));
        tvLocation.setText(args.getString("location"));
        tvQuota.setText(args.getString("quota") != null ? args.getString("quota") + " pax" : "Unlimited");

        String fee = args.getString("entryFee");
        tvFee.setText((fee == null || fee.equals("0")) ? "Free Entry" : "MYR " + fee);

        String sDate = args.getString("startDate");
        String eDate = args.getString("endDate");
        tvDate.setText(sDate + (eDate != null && !eDate.equals(sDate) ? " - " + eDate : ""));

        String sTime = args.getString("startTime");
        tvTime.setText(sTime);

        // Fetch Image
        String eventId = args.getString("eventId");
        if (eventId != null) {
            loadPosterImage(eventId);
        }

        // Countdown Logic
        setupCountdown(sDate, sTime);

        String link = args.getString("link");
// Retrieve the isJoined status passed from home.java
        boolean isJoined = args.getBoolean("isJoined", false);

// 1. Check if User Already Joined
        if (isJoined) {
            btnJoin.setText("Joined");
            btnJoin.setEnabled(false); // Make unclickable
            btnJoin.setBackgroundColor(android.graphics.Color.GRAY); // Optional: Visual indication
        }
// 2. If not joined, check if link exists
        else if (link == null || link.isEmpty()) {
            btnJoin.setVisibility(View.GONE);
        }
// 3. If link exists and not joined, enable the Join action
        else {
            btnJoin.setText("Join Event");
            btnJoin.setEnabled(true);
            // Call the confirmation dialog
            btnJoin.setOnClickListener(v -> showConfirmationDialog(link));
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    // NEW METHOD: Confirmation Dialog
    private void showConfirmationDialog(String link) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Redirect to link")
                .setMessage("Are you sure you want to be redirected to event form link?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    navigateToJoin(link); // Proceed if Yes
                })
                .setNegativeButton("No", null) // Dismiss if No
                .show();
    }

    private void loadPosterImage(String eventId) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String base64 = documentSnapshot.getString("posterBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            try {
                                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                                if (getContext() != null) {
                                    Glide.with(getContext())
                                            .load(bytes)
                                            .placeholder(R.drawable.image)
                                            .into(ivPoster);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private void setupCountdown(String dateStr, String timeStr) {
        try {
            String format = "dd/MM/yyyy HH:mm";
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
            Date eventDate = sdf.parse(dateStr + " " + timeStr);
            long eventMillis = eventDate.getTime();
            long currentMillis = System.currentTimeMillis();
            long diff = eventMillis - currentMillis;

            if (diff > 0) {
                new CountDownTimer(diff, 60000) {
                    public void onTick(long millisUntilFinished) {
                        long days = millisUntilFinished / (24 * 60 * 60 * 1000);
                        long hours = (millisUntilFinished / (60 * 60 * 1000)) % 24;
                        long mins = (millisUntilFinished / (60 * 1000)) % 60;

                        tvDays.setText(String.valueOf(days));
                        tvHours.setText(String.valueOf(hours));
                        tvMinutes.setText(String.valueOf(mins));
                    }
                    public void onFinish() { expireEvent(); }
                }.start();
            } else {
                expireEvent();
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvDays.setText("0");
        }
    }

    private void expireEvent() {
        if(getContext() == null) return;

        // 1. Change layout appearance
        layoutCountdown.setBackgroundColor(Color.LTGRAY); // Turn box gray

        // 2. Hide Timer Numbers
        llTimerContainer.setVisibility(View.GONE);

        // 3. Show "Event Expired" Text inside the box
        tvExpiredMessage.setVisibility(View.VISIBLE);

        // 4. Disable Join Button (but keep its text)
        btnJoin.setEnabled(false);
        btnJoin.setAlpha(0.5f); // Optional: Make button look faded
    }

    private void navigateToJoin(String link) {
        JoinEvent fragment = new JoinEvent();
        Bundle bundle = new Bundle(args);
        bundle.putString("link", link);
        fragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }
}