package com.example.project;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;

public class HomeEventAdapter extends RecyclerView.Adapter<HomeEventAdapter.EventViewHolder> {

    private Context context;
    private List<DocumentSnapshot> eventList;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(DocumentSnapshot doc);
    }

    public HomeEventAdapter(Context context, List<DocumentSnapshot> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card_home, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        DocumentSnapshot doc = eventList.get(position);

        holder.tvTitle.setText(doc.getString("title"));
        holder.tvOrganizer.setText("By: " + doc.getString("organizerName"));
        holder.tvCategory.setText(doc.getString("category"));
        holder.tvDesc.setText(doc.getString("description"));
        holder.tvLocation.setText(doc.getString("location"));

        String fee = doc.getString("entryFee");
        if (fee == null || fee.trim().isEmpty() || fee.equals("0")) {
            holder.tvPrice.setText("FREE");
        } else {
            holder.tvPrice.setText("MYR " + fee);
        }

        // Handle Image Loading with Glide from Base64
        String base64Image = doc.getString("posterBase64");
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] imageByteArray = Base64.decode(base64Image, Base64.DEFAULT);
                Glide.with(context)
                        .load(imageByteArray)
                        .centerCrop()
                        .placeholder(R.drawable.image) // Ensure you have a placeholder
                        .into(holder.ivPoster);
            } catch (Exception e) {
                holder.ivPoster.setImageResource(R.drawable.image);
            }
        } else {
            holder.ivPoster.setImageResource(R.drawable.image);
        }

        // Click Feedback Animation is handled by the View's background attribute (selectableItemBackground)
        holder.itemView.setOnClickListener(v -> listener.onEventClick(doc));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOrganizer, tvCategory, tvDesc, tvLocation, tvPrice;
        ImageView ivPoster;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvCardTitle);
            tvOrganizer = itemView.findViewById(R.id.tvCardOrganizer);
            tvCategory = itemView.findViewById(R.id.tvCardCategory);
            tvDesc = itemView.findViewById(R.id.tvCardDesc);
            tvLocation = itemView.findViewById(R.id.tvCardLocation);
            tvPrice = itemView.findViewById(R.id.tvCardPrice);
            ivPoster = itemView.findViewById(R.id.ivCardPoster);
        }
    }
}