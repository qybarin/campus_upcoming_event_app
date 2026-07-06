package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class Done extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_done, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Populate Reminder Details
        if (getArguments() != null) {
            TextView tvName = view.findViewById(R.id.tvDoneEventName);
            TextView tvDate = view.findViewById(R.id.tvDoneDate);
            TextView tvTime = view.findViewById(R.id.tvDoneTime);
            TextView tvLoc = view.findViewById(R.id.tvDoneLocation);

            tvName.setText(getArguments().getString("title"));
            tvDate.setText(getArguments().getString("startDate"));
            tvTime.setText(getArguments().getString("startTime"));
            tvLoc.setText(getArguments().getString("location"));
        }

        // Buttons
        Button btnHome = view.findViewById(R.id.btnHome);

        btnHome.setOnClickListener(v -> {
            // Clear back stack and go to Home
            getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new home())
                    .commit();

        });
    }
}