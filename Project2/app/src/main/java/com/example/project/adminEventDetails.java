package com.example.project;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfRenderer; // Added
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor; // Added
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView; // Added
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class adminEventDetails extends Fragment {

    private static final String ARG_EVENT_ID = "param1";
    private String eventId;
    private TextView tvEntryFee;

    private TextView tvEventName, tvDesc, tvDate, tvVenue, tvStatus;
    private TextView tvOrg, tvCat, tvTime, tvQuota, tvLink;
    private TextView tvDirName, tvDirMatric, tvDirPhone;

    private Button btnViewProposal, btnViewPoster, btnApprove, btnReject;
    private LinearLayout layoutActions;

    private FirebaseFirestore db;
    private DocumentSnapshot currentEvent;

    public adminEventDetails() {
        // Required empty public constructor
    }

    public static adminEventDetails newInstance(String eventId, String param2) {
        adminEventDetails fragment = new adminEventDetails();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_event_details, container, false);

        db = FirebaseFirestore.getInstance();

        // --- Init Views ---
        tvEventName = view.findViewById(R.id.tvDetailName);
        tvDesc = view.findViewById(R.id.tvDescription);
        tvDate = view.findViewById(R.id.tvDetailDate);
        tvVenue = view.findViewById(R.id.tvDetailVenue);
        tvStatus = view.findViewById(R.id.tvStatusValue);
        tvEntryFee = view.findViewById(R.id.tvEntryFee);

        tvOrg = view.findViewById(R.id.tvDetailOrg);
        tvCat = view.findViewById(R.id.tvDetailCat);
        tvTime = view.findViewById(R.id.tvDetailTime);
        tvQuota = view.findViewById(R.id.tvDetailQuota);
        tvLink = view.findViewById(R.id.tvDetailLink);

        tvDirName = view.findViewById(R.id.tvDirName);
        tvDirMatric = view.findViewById(R.id.tvDirMatric);
        tvDirPhone = view.findViewById(R.id.tvDirPhone);

        btnViewProposal = view.findViewById(R.id.btnViewProposal);
        btnViewPoster = view.findViewById(R.id.btnViewPoster);
        btnApprove = view.findViewById(R.id.btnDetailApprove);
        btnReject = view.findViewById(R.id.btnDetailReject);
        layoutActions = view.findViewById(R.id.layoutApprovalActions);

        view.findViewById(R.id.back_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadEventDetails();

        return view;
    }

    private void loadEventDetails() {
        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentEvent = doc;

                tvEventName.setText("Name                : " + doc.getString("title"));
                tvOrg.setText      ("Organized by   : " + doc.getString("organizerName"));
                tvCat.setText      ("Category          : " + doc.getString("category"));
                tvDate.setText     ("Date                  : " + doc.getString("startDate") + " - " + doc.getString("endDate"));
                tvTime.setText     ("Time                 : " + doc.getString("startTime") + " - " + doc.getString("endTime"));
                tvVenue.setText    ("Venue               : " + doc.getString("location"));
                tvQuota.setText    ("Quota               : " + doc.getString("quota"));
                tvLink.setText     ("Form Link        : " + doc.getString("link"));

                tvDesc.setText(doc.getString("description"));

                tvDirName.setText  ("Name             : " + doc.getString("studentName"));
                tvDirMatric.setText("Matric No       : " + doc.getString("matrixNumber"));
                tvDirPhone.setText ("Phone No       : " + doc.getString("phone"));

                String status = doc.getString("status");
                tvStatus.setText(status);

                String fee = doc.getString("entryFee");
                if (fee != null && !fee.isEmpty()) {
                    tvEntryFee.setText("Fee                    : MYR " + fee);
                } else {
                    tvEntryFee.setText("Fee                    : Free");
                }

                if ("Approved".equalsIgnoreCase(status)) {
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_approve);
                    layoutActions.setVisibility(View.GONE);
                } else if ("Rejected".equalsIgnoreCase(status)) {
                    tvStatus.setTextColor(Color.parseColor("#D35F5F"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_reject);
                    layoutActions.setVisibility(View.GONE);
                } else {
                    tvStatus.setTextColor(Color.parseColor("#C7B658"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    layoutActions.setVisibility(View.VISIBLE);
                    setupActionButtons();
                }

                setupFilePreviews();
            }
        });
    }

    private void setupFilePreviews() {
        String pdfBase64 = currentEvent.getString("approvalBase64");
        String imgBase64 = currentEvent.getString("posterBase64");

        btnViewProposal.setOnClickListener(v -> showPreviewDialog(pdfBase64, "PDF"));
        btnViewPoster.setOnClickListener(v -> showPreviewDialog(imgBase64, "Image"));
    }

    private void showPreviewDialog(String base64Data, String type) {
        if (base64Data == null || base64Data.isEmpty()) {
            Toast.makeText(getContext(), "No " + type + " file available", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_preview_file, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView titleText = dialogView.findViewById(R.id.headerTitle);
        ImageView ivPreview = dialogView.findViewById(R.id.ivPreviewImage);
        Button btnAction = dialogView.findViewById(R.id.btnDownloadFile);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClosePreview);

        // Set Title
        if (type.equals("PDF")) {
            titleText.setText("Event Approval (PDF)");

            // --- PDF RENDERING LOGIC ---
            // 1. Hide the default image view
            ivPreview.setVisibility(View.GONE);

            // 2. Get the FrameLayout parent
            ViewGroup parentLayout = (ViewGroup) ivPreview.getParent();

            // 3. Create a ScrollView and Container programmatically
            ScrollView scrollView = new ScrollView(getContext());
            LinearLayout pdfContainer = new LinearLayout(getContext());
            pdfContainer.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(pdfContainer);

            // 4. Add ScrollView to the existing FrameLayout
            parentLayout.addView(scrollView);

            // 5. Render pages
            try {
                File pdfFile = createTempPdfFile(base64Data);
                renderPdfToContainer(pdfFile, pdfContainer);
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error displaying PDF pages", Toast.LENGTH_SHORT).show();
            }

            // 6. Keep the "Open PDF" button functionality unchanged
            btnAction.setText("Open PDF Document");
            btnAction.setOnClickListener(v -> openPdfFile(base64Data));

        } else {
            // --- IMAGE LOGIC (UNCHANGED) ---
            titleText.setText("Event Poster");
            ivPreview.setVisibility(View.VISIBLE);
            btnAction.setText("Download Image");
            try {
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    ivPreview.setImageBitmap(decodedByte);
                } else {
                    ivPreview.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            } catch (Exception e) {
                ivPreview.setImageResource(android.R.drawable.ic_menu_report_image);
            }
            btnAction.setOnClickListener(v -> saveImageToDownloads(base64Data));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Helper: Create temporary file from Base64 for rendering
    private File createTempPdfFile(String base64Data) throws IOException {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        File cacheDir = new File(requireContext().getCacheDir(), "pdf_preview_cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        File tempFile = new File(cacheDir, "preview_render.pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(bytes);
        }
        return tempFile;
    }

    // Helper: Render PDF pages into LinearLayout
    private void renderPdfToContainer(File file, LinearLayout container) {
        try {
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            int pageCount = renderer.getPageCount();

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                // Create Bitmap
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE); // Ensure white background

                // Render content
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                // Create ImageView for the page
                ImageView pageImage = new ImageView(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 30); // Add gap between pages

                pageImage.setLayoutParams(params);
                pageImage.setAdjustViewBounds(true);
                pageImage.setImageBitmap(bitmap);

                container.addView(pageImage);
                page.close();
            }
            renderer.close();
            fileDescriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helper to Open PDF externally (Existing Functionality) ---
    private void openPdfFile(String base64Data) {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "temp_docs");
            if (!cachePath.exists()) cachePath.mkdirs();

            File newFile = new File(cachePath, "proposal_preview.pdf");
            FileOutputStream os = new FileOutputStream(newFile);
            os.write(Base64.decode(base64Data, Base64.DEFAULT));
            os.close();

            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", newFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (IOException e) {
            Toast.makeText(getContext(), "Error saving file", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No PDF Viewer app found", Toast.LENGTH_LONG).show();
        }
    }

    private void saveImageToDownloads(String base64Data) {
        try {
            File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "EventPoster_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream os = new FileOutputStream(file);
            os.write(Base64.decode(base64Data, Base64.DEFAULT));
            os.close();
            Toast.makeText(getContext(), "Saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupActionButtons() {
        btnApprove.setOnClickListener(v -> showActionDialog(true));
        btnReject.setOnClickListener(v -> showActionDialog(false));
    }

    private void showActionDialog(boolean isApprove) {
        if(getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reject_event, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etComment = dialogView.findViewById(R.id.etComment);
        Button btnProceed = dialogView.findViewById(R.id.btnProceed);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        TextView titleText = dialogView.findViewById(R.id.tvDialogTitle);

        if(titleText != null) titleText.setText(isApprove ? "Approve Event" : "Reject Event");
        btnProceed.setText(isApprove ? "Approve" : "Reject");
        btnProceed.setBackgroundColor(isApprove ? Color.parseColor("#4CAF50") : Color.parseColor("#D35F5F"));

        btnProceed.setOnClickListener(v -> {
            String comment = etComment.getText().toString().trim();
            if (comment.isEmpty()) {
                etComment.setError("Comment required");
                return;
            }
            processEventAction(isApprove ? "Approved" : "Rejected", comment);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void processEventAction(String newStatus, String comment) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("status", newStatus);
        updateMap.put("adminComment", comment);

        db.collection("events").document(eventId).update(updateMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event " + newStatus, Toast.LENGTH_SHORT).show();
                    if(currentEvent != null) {
                        createNotification(currentEvent.getString("uid"), currentEvent.getString("title"), newStatus, comment);
                    }
                    getParentFragmentManager().popBackStack();
                });
    }

    private void createNotification(String orgId, String eventName, String status, String comment) {
        if(orgId == null) return;
        Map<String, Object> notif = new HashMap<>();
        notif.put("organizerId", orgId);
        notif.put("title", "Event " + status);
        notif.put("message", "Your event '" + eventName + "' has been " + status + ". Comment: " + comment);
        notif.put("timestamp", com.google.firebase.Timestamp.now());
        notif.put("read", false);

        db.collection("notifications").add(notif);
    }
}