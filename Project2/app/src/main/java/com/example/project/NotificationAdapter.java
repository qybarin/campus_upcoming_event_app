package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> notificationList;

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inbox_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel item = notificationList.get(position);

        // Bind basic data
        holder.tvName.setText(item.getAdminName());
        holder.tvTimestamp.setText(item.getTimestamp());

        // Construct the details text
        String details = "Event Name : " + item.getEventName() + "\n" +
                "Approval : " + item.getStatus() + "\n" +
                "Comment : " + item.getComment();
        holder.tvEventDetails.setText(details);

        // Logic for the bottom message based on status
        if ("Approved".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatusMessage.setText("Congratulations 🎉 It is now ready to be viewed by UTHM students.");
        } else if ("Rejected".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatusMessage.setText("We appreciate your submission. However, your event was not approved. Kindly revise the details and resubmit for further review.");
        } else {
            holder.tvStatusMessage.setText("Status pending...");
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTimestamp, tvEventDetails, tvStatusMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvEventDetails = itemView.findViewById(R.id.tv_event_details);
            tvStatusMessage = itemView.findViewById(R.id.tv_status_message);
        }
    }
}