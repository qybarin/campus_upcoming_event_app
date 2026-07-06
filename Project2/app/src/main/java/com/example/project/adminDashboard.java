package com.example.project;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class adminDashboard extends Fragment {

    private RecyclerView recyclerView;
    private AdminEventAdapter adapter;
    private List<DocumentSnapshot> allEvents = new ArrayList<>();
    private List<DocumentSnapshot> filteredEvents = new ArrayList<>();
    private FirebaseFirestore db;

    private TextView tvCountPending, tvCountApproved;
    private Button filterAll, filterPending, filterApproved, filterRejected;

    private String currentFilter = "All";

    private ListenerRegistration eventListener;

    public adminDashboard() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        tvCountPending = view.findViewById(R.id.tvCountPending);
        tvCountApproved = view.findViewById(R.id.tvCountApproved);

        filterAll = view.findViewById(R.id.filterAll);
        filterPending = view.findViewById(R.id.filterPending);
        filterApproved = view.findViewById(R.id.filterApproved);
        filterRejected = view.findViewById(R.id.filterRejected);

        adapter = new AdminEventAdapter(filteredEvents);
        recyclerView.setAdapter(adapter);

        filterAll.setOnClickListener(v -> applyFilter("All"));
        filterPending.setOnClickListener(v -> applyFilter("Pending"));
        filterApproved.setOnClickListener(v -> applyFilter("Approved"));
        filterRejected.setOnClickListener(v -> applyFilter("Rejected"));

        loadEvents();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    private void loadEvents() {
        eventListener = db.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        allEvents.clear();

                        // Counters for stats
                        int pendingCount = 0;
                        int approvedCount = 0;

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            // --- STATS LOGIC ---
                            // Check status and increment counters regardless of 'isHidden'
                            String status = doc.getString("status");
                            if (status != null) {
                                if ("Pending".equalsIgnoreCase(status)) {
                                    pendingCount++;
                                } else if ("Approved".equalsIgnoreCase(status)) {
                                    approvedCount++;
                                }
                            }

                            // --- LIST DISPLAY LOGIC ---
                            // Only add to the list if 'isHidden' is false or null
                            Boolean isHidden = doc.getBoolean("isHidden");
                            if (isHidden == null || !isHidden) {
                                allEvents.add(doc);
                            }
                        }

                        // Update Text Views directly with the counts calculated above
                        tvCountPending.setText(String.valueOf(pendingCount));
                        tvCountApproved.setText(String.valueOf(approvedCount));

                        // Apply filter to the visible list
                        applyFilter(currentFilter);
                    }
                });
    }

    // updateStats() method removed as logic is now inside loadEvents to handle the separation of concerns

    private void applyFilter(String status) {
        currentFilter = status;
        updateFilterButtons(status);

        List<DocumentSnapshot> tempList = new ArrayList<>();

        if (status.equals("All")) {
            tempList.addAll(allEvents);
        } else {
            for (DocumentSnapshot doc : allEvents) {
                String docStatus = doc.getString("status");
                if (docStatus != null && status.equalsIgnoreCase(docStatus)) {
                    tempList.add(doc);
                }
            }
        }

        filteredEvents.clear();
        filteredEvents.addAll(tempList);
        adapter.notifyDataSetChanged();
    }

    private void updateFilterButtons(String selected) {
        int selectedColor = Color.parseColor("#004A77");
        int defaultColor = Color.parseColor("#9E9E9E");

        filterAll.setBackgroundColor(selected.equals("All") ? selectedColor : defaultColor);
        filterPending.setBackgroundColor(selected.equals("Pending") ? selectedColor : defaultColor);
        filterApproved.setBackgroundColor(selected.equals("Approved") ? selectedColor : defaultColor);
        filterRejected.setBackgroundColor(selected.equals("Rejected") ? selectedColor : defaultColor);
    }

    private void showActionDialog(DocumentSnapshot eventDoc, boolean isApprove) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reject_event, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etComment = dialogView.findViewById(R.id.etComment);
        Button btnProceed = dialogView.findViewById(R.id.btnProceed);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        if (tvTitle != null) {
            tvTitle.setText(isApprove ? "Approve Event" : "Reject Event");
        }
        if (btnProceed != null) {
            btnProceed.setText(isApprove ? "Approve" : "Reject");
            btnProceed.setBackgroundColor(isApprove ? Color.parseColor("#4CAF50") : Color.parseColor("#D35F5F"));

            btnProceed.setOnClickListener(v -> {
                String comment = etComment.getText().toString().trim();
                if (comment.isEmpty()) {
                    etComment.setError("Comment required");
                    return;
                }
                processEventAction(eventDoc, isApprove ? "Approved" : "Rejected", comment);
                dialog.dismiss();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void processEventAction(DocumentSnapshot doc, String newStatus, String comment) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("status", newStatus);
        updateMap.put("adminComment", comment);

        db.collection("events").document(doc.getId()).update(updateMap)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Event " + newStatus, Toast.LENGTH_SHORT).show();
                    }
                    createNotification(doc.getString("uid"), doc.getString("title"), newStatus, comment);
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNotification(String orgId, String eventName, String status, String comment) {
        if (orgId == null) return;
        Map<String, Object> notif = new HashMap<>();
        notif.put("organizerId", orgId);
        notif.put("title", "Event " + status);
        notif.put("message", "Your event '" + eventName + "' has been " + status + ". Comment: " + comment);
        notif.put("timestamp", com.google.firebase.Timestamp.now());
        notif.put("read", false);

        db.collection("notifications").add(notif);
    }

    private void deleteEvent(String eventId) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to remove this event?")
                .setPositiveButton("Delete", (d, w) -> {
                    // Soft delete: set isHidden to true
                    db.collection("events").document(eventId).update("isHidden", true)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Event Removed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error removing event", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.ViewHolder> {
        List<DocumentSnapshot> list;

        public AdminEventAdapter(List<DocumentSnapshot> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_event, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position >= list.size()) return;

            DocumentSnapshot doc = list.get(position);
            String status = doc.getString("status");

            holder.tvTitle.setText(doc.getString("title"));
            if (doc.getString("organizerName") != null) {
                holder.tvOrg.setText("Organized by: " + doc.getString("organizerName"));
            }
            holder.tvDate.setText(doc.getString("startDate") + " | " + doc.getString("location"));

            // Status styling
            if ("Pending".equalsIgnoreCase(status)) {
                holder.statusStrip.setBackgroundColor(Color.parseColor("#FFC107"));
                holder.layoutPending.setVisibility(View.VISIBLE);
                holder.btnDetails.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
            } else if ("Approved".equalsIgnoreCase(status)) {
                holder.statusStrip.setBackgroundColor(Color.parseColor("#4CAF50"));
                holder.layoutPending.setVisibility(View.GONE);
                holder.btnDetails.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
            } else if ("Rejected".equalsIgnoreCase(status)) {
                holder.statusStrip.setBackgroundColor(Color.parseColor("#D35F5F"));
                holder.layoutPending.setVisibility(View.GONE);
                holder.btnDetails.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
            } else {
                holder.statusStrip.setBackgroundColor(Color.parseColor("#9E9E9E"));
                holder.layoutPending.setVisibility(View.GONE);
                holder.btnDetails.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(v -> openDetails(doc.getId()));
            holder.btnDetails.setOnClickListener(v -> openDetails(doc.getId()));

            holder.btnApprove.setOnClickListener(v -> showActionDialog(doc, true));
            holder.btnReject.setOnClickListener(v -> showActionDialog(doc, false));
            holder.btnDelete.setOnClickListener(v -> deleteEvent(doc.getId()));
        }

        private void openDetails(String eventId) {
            adminEventDetails fragment = adminEventDetails.newInstance(eventId, "");
            View parent = (View) getView().getParent();
            int containerId = parent != null ? parent.getId() : R.id.adminFragmentView;

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(containerId, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvOrg, tvDate;
            View statusStrip;
            LinearLayout layoutPending;
            Button btnApprove, btnReject, btnDetails;
            ImageView btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvEventTitle);
                tvOrg = itemView.findViewById(R.id.tvOrganizerName);
                tvDate = itemView.findViewById(R.id.tvEventDateVenue);
                statusStrip = itemView.findViewById(R.id.statusStrip);
                layoutPending = itemView.findViewById(R.id.layoutPendingActions);
                btnApprove = itemView.findViewById(R.id.btnQuickApprove);
                btnReject = itemView.findViewById(R.id.btnQuickReject);
                btnDetails = itemView.findViewById(R.id.btnDetails);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}