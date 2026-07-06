package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class MyEventAdapter extends RecyclerView.Adapter<MyEventAdapter.ViewHolder> {

    private Context context;
    private List<DocumentSnapshot> eventList;
    private OnEventClickListener listener;

    // Updated Interface to handle Edit and Delete actions
    public interface OnEventClickListener {
        void onEventClick(DocumentSnapshot doc);
        void onEditClick(DocumentSnapshot doc);
        void onDeleteClick(DocumentSnapshot doc, int position);
    }

    public MyEventAdapter(Context context, List<DocumentSnapshot> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = eventList.get(position);

        // --- Basic Data Binding ---
        holder.tvTitle.setText(doc.getString("title"));
        holder.tvLocation.setText(doc.getString("location"));
        holder.tvCategory.setText(doc.getString("category"));
        holder.tvDate.setText(doc.getString("startDate"));

        // Quota Logic
        Object quotaObj = doc.get("quota");
        if (quotaObj == null) quotaObj = doc.get("participantCount");

        if (quotaObj != null) {
            String quota = String.valueOf(quotaObj);
            if (quota.equalsIgnoreCase("Open for all")) {
                holder.tvQuota.setText("Open for all");
            } else if (quota.matches("\\d+")) {
                holder.tvQuota.setText(quota + " Participants");
            } else {
                holder.tvQuota.setText(quota);
            }
        } else {
            holder.tvQuota.setText("0 Participants");
        }

        // --- BUTTON VISIBILITY LOGIC (UPDATED) ---
        String status = doc.getString("status");

        // Default Colors
        int colorBlue = Color.parseColor("#006E95");
        int colorGray = Color.parseColor("#BDBDBD");
        int colorRed = Color.parseColor("#D32F2F");
        int colorGreenText = Color.parseColor("#155724");
        int colorYellowText = Color.parseColor("#856404");
        int colorRedText = Color.parseColor("#721C24");

        // Reset Stepper colors
        holder.ivStep1.setColorFilter(colorBlue);
        holder.line1.setBackgroundColor(colorGray);
        holder.ivStep2.setColorFilter(colorGray);
        holder.line2.setBackgroundColor(colorGray);
        holder.ivStep3.setColorFilter(colorGray);
        holder.ivStep3.setImageResource(R.drawable.check);

        if (status != null) {
            holder.tvStatusChip.setText(status);
            String normalizedStatus = status.toLowerCase();

            if (normalizedStatus.contains("approve")) {
                // === APPROVED ===
                // UI: Status
                holder.tvStatusChip.setBackgroundResource(R.drawable.bg_status_approve);
                holder.tvStatusChip.setTextColor(colorGreenText);

                // UI: Stepper (All Blue)
                holder.line1.setBackgroundColor(colorBlue);
                holder.ivStep2.setColorFilter(colorBlue);
                holder.line2.setBackgroundColor(colorBlue);
                holder.ivStep3.setColorFilter(colorBlue);
                holder.tvLabelStep2.setTextColor(colorBlue);
                holder.tvLabelStep3.setText("Approved");
                holder.tvLabelStep3.setTextColor(colorBlue);

                // BUTTON LOGIC: Hide "In Progress", Show "Edit" and "Delete"
                holder.btnInProgress.setVisibility(View.GONE);
                holder.llActionButtons.setVisibility(View.VISIBLE);
                holder.btnEdit.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);

            } else if (normalizedStatus.contains("reject")) {
                // === REJECTED ===
                // UI: Status
                holder.tvStatusChip.setBackgroundResource(R.drawable.bg_status_reject);
                holder.tvStatusChip.setTextColor(colorRedText);

                // UI: Stepper (Red Cross)
                holder.line1.setBackgroundColor(colorBlue);
                holder.ivStep2.setColorFilter(colorBlue);
                holder.line2.setBackgroundColor(colorRed);
                holder.ivStep3.setColorFilter(colorRed);
                holder.ivStep3.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                holder.tvLabelStep2.setTextColor(colorBlue);
                holder.tvLabelStep3.setText("Rejected");
                holder.tvLabelStep3.setTextColor(colorRed);

                // BUTTON LOGIC: Hide "In Progress", Show ONLY "Delete"
                holder.btnInProgress.setVisibility(View.GONE);
                holder.llActionButtons.setVisibility(View.VISIBLE);
                holder.btnEdit.setVisibility(View.GONE); // Hide Edit
                holder.btnDelete.setVisibility(View.VISIBLE);

            } else {
                // === PENDING / IN PROGRESS ===
                holder.tvStatusChip.setBackgroundResource(R.drawable.bg_status_pending);
                holder.tvStatusChip.setTextColor(colorYellowText);
                holder.line1.setBackgroundColor(colorBlue);
                holder.ivStep2.setColorFilter(colorBlue);
                holder.tvLabelStep2.setTextColor(colorBlue);
                holder.tvLabelStep3.setText("Approved");
                holder.tvLabelStep3.setTextColor(colorGray);

                // BUTTON LOGIC: Show "In Progress", Hide Actions
                holder.btnInProgress.setVisibility(View.VISIBLE);
                holder.llActionButtons.setVisibility(View.GONE);
            }
        } else {
            // Default Pending
            holder.tvStatusChip.setText("Pending");
            holder.btnInProgress.setVisibility(View.VISIBLE);
            holder.llActionButtons.setVisibility(View.GONE);
        }

        // --- CLICK LISTENERS ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(doc);
        });

        // Delete Button Click
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(doc, position);
        });

        // Edit Button Click
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(doc);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvCategory, tvDate, tvQuota, tvStatusChip;
        TextView tvLabelStep2, tvLabelStep3;
        ImageView ivStep1, ivStep2, ivStep3;
        View line1, line2;
        Button btnInProgress, btnEdit, btnDelete;
        LinearLayout llActionButtons;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvQuota = itemView.findViewById(R.id.tvQuota);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);

            ivStep1 = itemView.findViewById(R.id.ivStep1);
            ivStep2 = itemView.findViewById(R.id.ivStep2);
            ivStep3 = itemView.findViewById(R.id.ivStep3);
            line1 = itemView.findViewById(R.id.line1);
            line2 = itemView.findViewById(R.id.line2);
            tvLabelStep2 = itemView.findViewById(R.id.tvLabelStep2);
            tvLabelStep3 = itemView.findViewById(R.id.tvLabelStep3);

            btnInProgress = itemView.findViewById(R.id.btnInProgress);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            llActionButtons = itemView.findViewById(R.id.llActionButtons);
        }
    }
}