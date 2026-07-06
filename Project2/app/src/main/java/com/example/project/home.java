package com.example.project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class home extends Fragment implements HomeEventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private HomeEventAdapter adapter;

    // Data Lists
    private List<DocumentSnapshot> fullEventList;
    private List<DocumentSnapshot> filteredList;
    private Set<String> joinedEventIds;
    private List<String> recentEventIds;

    // UI Elements
    private ProgressBar progressBar;
    private ChipGroup chipGroupCategory, chipGroupType;
    private LinearLayout layoutFilterContainer; // The collapsible part
    private LinearLayout llHeaderRow; // The clickable part
    private ImageView ivFilterChevron; // The arrow icon

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String currentCategory = "All";
    private String currentType = "Discover";
    private boolean isFilterExpanded = false; // Track state

    public home() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Init Views
        recyclerView = view.findViewById(R.id.rvParticipantEvents);
        progressBar = view.findViewById(R.id.progressBarHome);
        chipGroupCategory = view.findViewById(R.id.chipGroupFilter);
        chipGroupType = view.findViewById(R.id.chipGroupType);

        // Filter Expansion Views
        layoutFilterContainer = view.findViewById(R.id.layoutFilterContainer);
        llHeaderRow = view.findViewById(R.id.llHeaderRow);
        ivFilterChevron = view.findViewById(R.id.ivFilterChevron);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fullEventList = new ArrayList<>();
        filteredList = new ArrayList<>();
        joinedEventIds = new HashSet<>();
        recentEventIds = new ArrayList<>();

        adapter = new HomeEventAdapter(getContext(), filteredList, this);
        recyclerView.setAdapter(adapter);

        // Load Recents
        loadRecentIds();

        setupFilterListeners();
        setupExpandCollapseLogic(); // New Logic
        loadAllData();
    }

    // --- NEW: EXPAND/COLLAPSE LOGIC ---
    private void setupExpandCollapseLogic() {
        llHeaderRow.setOnClickListener(v -> {
            toggleFilters();
        });
    }

    private void toggleFilters() {
        if (isFilterExpanded) {
            // Collapse
            layoutFilterContainer.setVisibility(View.GONE);
            ivFilterChevron.animate().rotation(0).setDuration(300).start();
        } else {
            // Expand
            layoutFilterContainer.setVisibility(View.VISIBLE);
            ivFilterChevron.animate().rotation(90).setDuration(300).start();
        }
        isFilterExpanded = !isFilterExpanded;
    }

    private void setupFilterListeners() {
        // 1. Category Listener
        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) return;
            Chip chip = group.findViewById(checkedId);
            if(chip != null) {
                currentCategory = chip.getText().toString();
                applyFilters();
            }
        });

        // 2. Type Listener
        chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) return;
            Chip chip = group.findViewById(checkedId);
            if(chip != null) {
                String text = chip.getText().toString();
                if(text.contains("Discover")) currentType = "Discover";
                else if(text.contains("Joined")) currentType = "Joined";
                else if(text.contains("Recent")) currentType = "Recent";

                applyFilters();
            }
        });
    }

    private void loadAllData() {
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        db.collection("registrations")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    joinedEventIds.clear();
                    for(DocumentSnapshot d : snap){
                        joinedEventIds.add(d.getString("eventId"));
                    }
                    fetchEvents();
                })
                .addOnFailureListener(e -> fetchEvents());
    }

    private void fetchEvents() {
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("events")
                .whereEqualTo("isHidden", false)   // Matches your database requirement
                .whereEqualTo("status", "Approved") // Matches your database requirement
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(progressBar != null) progressBar.setVisibility(View.GONE);

                    // --- CRITICAL MISSING PIECE ---
                    // 1. Clear the old list
                    fullEventList.clear();

                    // 2. Add the results from Firestore to your fullEventList
                    if (!queryDocumentSnapshots.isEmpty()) {
                        fullEventList.addAll(queryDocumentSnapshots.getDocuments());
                    }

                    // 3. Apply your filters (All, Joined, Category, etc.) to show them in the RecyclerView
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    if(progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        filteredList.clear();

        for (DocumentSnapshot doc : fullEventList) {
            String docId = doc.getId();
            String docCat = doc.getString("category");

            boolean typeMatch = false;
            if (currentType.equals("Discover")) {
                typeMatch = true;
            } else if (currentType.equals("Joined")) {
                typeMatch = joinedEventIds.contains(docId);
            } else if (currentType.equals("Recent")) {
                typeMatch = recentEventIds.contains(docId);
            }

            boolean categoryMatch = false;
            if (currentCategory.equalsIgnoreCase("All")) {
                categoryMatch = true;
            } else if (docCat != null && docCat.equalsIgnoreCase(currentCategory)) {
                categoryMatch = true;
            }

            if (typeMatch && categoryMatch) {
                filteredList.add(doc);
            }
        }

        if(currentType.equals("Recent")){
            Collections.reverse(filteredList);
        }

        adapter.notifyDataSetChanged();
    }

    private void loadRecentIds() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyEventsApp", Context.MODE_PRIVATE);
        String idsString = prefs.getString("recent_events", "");
        recentEventIds.clear();
        if(!idsString.isEmpty()){
            String[] split = idsString.split(",");
            for(String s : split){
                if(!s.trim().isEmpty()) recentEventIds.add(s);
            }
        }
    }

    private void saveRecentId(String newId) {
        recentEventIds.remove(newId);
        recentEventIds.add(newId);
        if(recentEventIds.size() > 20) {
            recentEventIds.remove(0);
        }
        StringBuilder sb = new StringBuilder();
        for(String id : recentEventIds){
            sb.append(id).append(",");
        }
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyEventsApp", Context.MODE_PRIVATE);
        prefs.edit().putString("recent_events", sb.toString()).apply();
    }

    @Override
    public void onEventClick(DocumentSnapshot doc) {
        saveRecentId(doc.getId());

        EventDetail nextFragment = new EventDetail();
        Bundle args = new Bundle();

        args.putString("eventId", doc.getId());
        args.putString("title", doc.getString("title"));
        args.putString("category", doc.getString("category"));
        args.putString("organizer", doc.getString("organizerName"));
        args.putString("location", doc.getString("location"));
        args.putString("entryFee", doc.getString("entryFee"));
        args.putString("startDate", doc.getString("startDate"));
        args.putString("startTime", doc.getString("startTime"));
        args.putString("endDate", doc.getString("endDate"));
        args.putString("endTime", doc.getString("endTime"));
        args.putString("quota", doc.getString("quota"));
        args.putString("link", doc.getString("link"));
        args.putString("desc", doc.getString("description"));

        // --- NEW LOGIC ADDED HERE ---
        // Check if the clicked event is in the 'joinedEventIds' list
        if (joinedEventIds.contains(doc.getId())) {
            args.putBoolean("isJoined", true);
        } else {
            args.putBoolean("isJoined", false);
        }
        // ----------------------------

        nextFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, nextFragment)
                .addToBackStack(null)
                .commit();
    }
}