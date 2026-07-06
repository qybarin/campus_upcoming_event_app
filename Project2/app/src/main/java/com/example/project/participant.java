package com.example.project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.ImageView;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class participant extends AppCompatActivity {

    private ImageView btnHome, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_participant);

        // Initialize buttons
        btnHome = findViewById(R.id.btnHome);
        btnProfile = findViewById(R.id.btnProfile);

        // Set click listeners
        btnHome.setOnClickListener(v -> {
            loadFragment(new home());  // Changed to Home()
        });

        btnProfile.setOnClickListener(v -> loadFragment(new profile()));  // Changed to Profile()



        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new home());  // Changed to Home()
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.commit();
    }
}