package com.example.project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class Organizer extends AppCompatActivity {

    private ImageView btnHome, btnManageEvent, btnCreateEvent, btnInbox, btnSetting;
    private List<ImageView> navButtons = new ArrayList<>();

    // Define Colors
    private final int SELECTED_COLOR = Color.parseColor("#018ABD");
    private final int UNSELECTED_COLOR = Color.parseColor("#B1B1B1");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_organizer);

        // Initialize buttons
        btnHome = findViewById(R.id.btnHome);
        btnManageEvent = findViewById(R.id.btnManageEvent);
        btnCreateEvent = findViewById(R.id.btnCreateEventMain);
        btnInbox = findViewById(R.id.btnInbox);
        btnSetting = findViewById(R.id.btnSetting);

        // Add all buttons to a list for easier management
        navButtons.add(btnHome);
        navButtons.add(btnManageEvent);
        navButtons.add(btnCreateEvent);
        navButtons.add(btnInbox);
        navButtons.add(btnSetting);

        // Set click listeners
        btnHome.setOnClickListener(v -> {
            updateBottomNav(btnHome);
            loadFragment(new home_org(), "home", false);
        });

        btnManageEvent.setOnClickListener(v -> {
            updateBottomNav(btnManageEvent);
            loadFragment(new Event_manager(), "eventManager", true);
        });

        btnCreateEvent.setOnClickListener(v -> {
            updateBottomNav(btnCreateEvent);
            loadFragment(new EventForm(), "eventForm", true);
        });

        btnInbox.setOnClickListener(v -> {
            updateBottomNav(btnInbox);
            loadFragment(new OrgInbox(), "inbox", true);
        });

        btnSetting.setOnClickListener(v -> {
            updateBottomNav(btnSetting);
            loadFragment(new OrgSetting(), "setting", true);
        });

        // Load default fragment and set default UI state
        if (savedInstanceState == null) {
            updateBottomNav(btnHome); // Highlight Home initially
            loadFragment(new home_org(), "home", false);
        }
    }

    /**
     * Updates the UI of the bottom navigation bar.
     * Highlights the selected button and dims the others.
     * Adds a scaling animation.
     */
    private void updateBottomNav(ImageView selectedView) {
        for (ImageView btn : navButtons) {
            if (btn == selectedView) {
                // SELECTED STATE
                btn.setColorFilter(SELECTED_COLOR);

                // Animate Scale Up (Zoom In)
                btn.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(200)
                        .start();
            } else {
                // UNSELECTED STATE
                btn.setColorFilter(UNSELECTED_COLOR);

                // Animate Scale Down (Return to normal)
                btn.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start();
            }
        }
    }

    private void loadFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentView, fragment, tag);

        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }

        transaction.commit();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            // Optional: Logic to reset the selected icon based on which fragment is visible
            // requires more complex fragment management, usually handled by checking current tag
        } else {
            super.onBackPressed();
        }
    }
}