package com.example.project;

import static com.example.project.R.id.tvStatusChip;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event_manager extends Fragment implements MyEventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private MyEventAdapter adapter;
    private List<DocumentSnapshot> eventList;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // For Edit Poster Logic
    private ActivityResultLauncher<String> pickImageLauncher;
    private String tempNewPosterBase64 = null; // Stores new image data if user changes it
    private ImageView ivEditPosterPreviewRef; // Reference to update dialog UI
    private TextView tvEditPosterNameRef;

    public Event_manager() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.rvMyEvents);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventList = new ArrayList<>();
        adapter = new MyEventAdapter(getContext(), eventList, this);
        recyclerView.setAdapter(adapter);

        // Initialize Image Picker for Editing
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && ivEditPosterPreviewRef != null) {
                processSelectedImage(uri);
            }
        });

        loadMyEvents();
    }

    private void loadMyEvents() {
        if (auth.getCurrentUser() == null) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("events")
                .whereEqualTo("uid", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    eventList.clear();

                    // Filter out "hidden" events manually (Soft Delete logic)
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isHidden = doc.getBoolean("isHidden");
                        if (isHidden == null || !isHidden) {
                            eventList.add(doc);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (getContext() != null) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- INTERFACE METHODS ---

    @Override
    public void onEventClick(DocumentSnapshot doc) {
        showEventDetailsDialog(doc);
    }

    @Override
    public void onEditClick(DocumentSnapshot doc) {
        showEditEventDialog(doc);
    }

    @Override
    public void onDeleteClick(DocumentSnapshot doc, int position) {
        // Soft Delete: Mark as hidden in Firestore, remove from UI
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("events").document(doc.getId())
                .update("isHidden", true)
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    // Remove from list locally
                    eventList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, eventList.size());

                    Toast.makeText(getContext(), "Event removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- EDIT EVENT DIALOG ---
    private void showEditEventDialog(DocumentSnapshot doc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // Inflate the Edit Dialog Layout (User needs to create dialog_edit_event.xml similar to fragment_event_form.xml)
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_event, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Transparent background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 1. Initialize Inputs (Using IDs from fragment_event_form.xml structure)
        TextInputEditText etTitle = view.findViewById(R.id.etEventTitle);
        TextInputEditText etLocation = view.findViewById(R.id.etLocation);
        TextInputEditText etStartDate = view.findViewById(R.id.etStartDate);
        TextInputEditText etEndDate = view.findViewById(R.id.etEndDate);
        TextInputEditText etStartTime = view.findViewById(R.id.etStartTime);
        TextInputEditText etEndTime = view.findViewById(R.id.etEndTime);
        TextInputEditText etGoogleForm = view.findViewById(R.id.etGoogleFormLink);
        TextInputEditText etDesc = view.findViewById(R.id.etDescription);
        TextInputEditText etOrgName = view.findViewById(R.id.etOrganizerName);
        TextInputEditText etStuName = view.findViewById(R.id.etStudentName);
        TextInputEditText etMatrix = view.findViewById(R.id.etMatrixNumber);
        TextInputEditText etPhone = view.findViewById(R.id.etPhoneNumber);
        TextInputEditText etQuota = view.findViewById(R.id.etEventQuota);
        TextInputEditText etFee = view.findViewById(R.id.etEntryFee);

        RadioGroup rgQuota = view.findViewById(R.id.rgQuota);
        RadioButton rbOpen = view.findViewById(R.id.rbOpenForAll);
        RadioButton rbSpecific = view.findViewById(R.id.rbSpecificAmount);
        TextInputLayout tilQuota = view.findViewById(R.id.tilEventQuota);

        AutoCompleteTextView actvCategory = view.findViewById(R.id.actvCategories);

        // Image Views
        View cvUploadPoster = view.findViewById(R.id.cvUploadPoster); // Clickable card
        ivEditPosterPreviewRef = view.findViewById(R.id.ivPosterPreview); // You might need to add this ID to your layout inside the card or use existing
        if (ivEditPosterPreviewRef == null) {
            // Fallback if user uses the layout exactly: find the image inside the "Logged" card or similar
            // For simplicity, assuming the layout has an ImageView for preview or we use the icon
            // Let's assume the user copies the form structure.
            // We will check specifically for cvPosterLog visibility logic
        }

        // Buttons
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnUpdate = view.findViewById(R.id.btnSubmitEvent); // Reused ID, but text should be "Update"
        if(btnUpdate != null) btnUpdate.setText("Update Event");

        // 2. Pre-fill Data
        if(etTitle != null) etTitle.setText(doc.getString("title"));
        if(etLocation != null) etLocation.setText(doc.getString("location"));
        if(etStartDate != null) etStartDate.setText(doc.getString("startDate"));
        if(etEndDate != null) etEndDate.setText(doc.getString("endDate"));
        if(etStartTime != null) etStartTime.setText(doc.getString("startTime"));
        if(etEndTime != null) etEndTime.setText(doc.getString("endTime"));
        if(etGoogleForm != null) etGoogleForm.setText(doc.getString("link"));
        if(etDesc != null) etDesc.setText(doc.getString("description"));
        if(etOrgName != null) etOrgName.setText(doc.getString("organizerName"));
        if(etStuName != null) etStuName.setText(doc.getString("studentName"));
        if(etMatrix != null) etMatrix.setText(doc.getString("matrixNumber"));
        if(etPhone != null) etPhone.setText(doc.getString("phone"));
        if(etFee != null) etFee.setText(doc.getString("entryFee"));

        // Category Setup
        String currentCategory = doc.getString("category");
        if(actvCategory != null) {
            actvCategory.setText(currentCategory);
            // Re-populate adapter logic if needed, or just leave as text
            String[] categories = {"Academic", "Sports", "Arts", "Technology", "Social", "Others"};
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, categories);
            actvCategory.setAdapter(catAdapter);
        }

        // Quota Logic
        String quota = doc.getString("quota");
        if (quota != null && !quota.equals("Open for all")) {
            if(rbSpecific != null) rbSpecific.setChecked(true);
            if(tilQuota != null) tilQuota.setVisibility(View.VISIBLE);
            if(etQuota != null) etQuota.setText(quota);
        } else {
            if(rbOpen != null) rbOpen.setChecked(true);
            if(tilQuota != null) tilQuota.setVisibility(View.GONE);
        }

        if (rgQuota != null) {
            rgQuota.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbSpecificAmount) {
                    if(tilQuota != null) tilQuota.setVisibility(View.VISIBLE);
                } else {
                    if(tilQuota != null) tilQuota.setVisibility(View.GONE);
                }
            });
        }

        // 3. Date & Time Pickers
        setupDatePickers(etStartDate, etEndDate);
        setupTimePickers(etStartTime, etEndTime);

        // 4. Poster Edit Logic
        // Reset temp variable
        tempNewPosterBase64 = null;

        if (cvUploadPoster != null) {
            cvUploadPoster.setOnClickListener(v -> {
                // Launch Image Picker
                pickImageLauncher.launch("image/*");
            });
        }

        // Note: Approval Letter UI is SKIPPED/HIDDEN as per requirements

        // 5. Update Action
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                // Collect Data
                Map<String, Object> updates = new HashMap<>();
                if(etTitle != null) updates.put("title", etTitle.getText().toString().trim());
                if(etLocation != null) updates.put("location", etLocation.getText().toString().trim());
                if(etStartDate != null) updates.put("startDate", etStartDate.getText().toString().trim());
                if(etEndDate != null) updates.put("endDate", etEndDate.getText().toString().trim());
                if(etStartTime != null) updates.put("startTime", etStartTime.getText().toString().trim());
                if(etEndTime != null) updates.put("endTime", etEndTime.getText().toString().trim());
                if(etGoogleForm != null) updates.put("link", etGoogleForm.getText().toString().trim());
                if(etDesc != null) updates.put("description", etDesc.getText().toString().trim());
                if(etOrgName != null) updates.put("organizerName", etOrgName.getText().toString().trim());
                if(etStuName != null) updates.put("studentName", etStuName.getText().toString().trim());
                if(etMatrix != null) updates.put("matrixNumber", etMatrix.getText().toString().trim());
                if(etPhone != null) updates.put("phone", etPhone.getText().toString().trim());
                if(etFee != null) updates.put("entryFee", etFee.getText().toString().trim());
                if(actvCategory != null) updates.put("category", actvCategory.getText().toString());

                // Quota
                if (rbSpecific != null && rbSpecific.isChecked() && etQuota != null) {
                    updates.put("quota", etQuota.getText().toString().trim());
                } else {
                    updates.put("quota", "Open for all");
                }

                // Poster (Only update if changed)
                if (tempNewPosterBase64 != null) {
                    updates.put("posterBase64", tempNewPosterBase64);
                }

                // Perform Update
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                db.collection("events").document(doc.getId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Event Updated Successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadMyEvents(); // Refresh list
                        })
                        .addOnFailureListener(e -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // --- HELPER: Process Image Selection ---
    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize image to avoid Firestore limit (approx < 1MB)
            Bitmap resizedBitmap = getResizedBitmap(bitmap, 600);

            // Convert to Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            tempNewPosterBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Toast.makeText(getContext(), "New poster selected", Toast.LENGTH_SHORT).show();
            // Optional: Update a preview ImageView in the dialog if you added one

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    // --- HELPER: Date/Time Pickers ---
    private void setupDatePickers(TextInputEditText etStart, TextInputEditText etEnd) {
        View.OnClickListener dateListener = v -> {
            TextInputEditText target = (TextInputEditText) v;
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                String date = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                target.setText(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        };
        if(etStart != null) etStart.setOnClickListener(dateListener);
        if(etEnd != null) etEnd.setOnClickListener(dateListener);
    }

    private void setupTimePickers(TextInputEditText etStart, TextInputEditText etEnd) {
        View.OnClickListener timeListener = v -> {
            TextInputEditText target = (TextInputEditText) v;
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
                String time = String.format("%02d:%02d", hourOfDay, minute);
                target.setText(time);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        };
        if(etStart != null) etStart.setOnClickListener(timeListener);
        if(etEnd != null) etEnd.setOnClickListener(timeListener);
    }

    // --- DIALOG LOGIC (VIEW DETAILS - Existing) ---
    private void showEventDetailsDialog(DocumentSnapshot doc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_event_details, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 1. Initialize ALL Views
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDetails);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView tvStatusChip = dialogView.findViewById(R.id.tvStatusChip);

        TextView tvTitle = dialogView.findViewById(R.id.tvDetailTitle);
        TextView tvLocation = dialogView.findViewById(R.id.tvDetailLocation);
        TextView tvStart = dialogView.findViewById(R.id.tvDetailStartDate);
        TextView tvEnd = dialogView.findViewById(R.id.tvDetailEndDate);
        TextView tvTime = dialogView.findViewById(R.id.tvDetailTime);
        TextView tvCategory = dialogView.findViewById(R.id.tvDetailCategory);
        TextView tvLink = dialogView.findViewById(R.id.tvDetailLink);
        TextView tvDesc = dialogView.findViewById(R.id.tvDetailDesc);
        TextView tvEntryFee = dialogView.findViewById(R.id.tvDetailEntryFee);

        TextView tvOrganizer = dialogView.findViewById(R.id.tvDetailOrganizerName);
        TextView tvStudent = dialogView.findViewById(R.id.tvDetailStudentName);
        TextView tvMatrix = dialogView.findViewById(R.id.tvDetailMatrix);
        TextView tvPhone = dialogView.findViewById(R.id.tvDetailPhone);

        ImageView ivPoster = dialogView.findViewById(R.id.ivDetailPoster);
        LinearLayout pdfContainer = dialogView.findViewById(R.id.llDetailPdfContainer);

        // 2. Populate Data
        if(tvTitle != null) tvTitle.setText(doc.getString("title"));
        if(tvLocation != null) tvLocation.setText(doc.getString("location"));
        if(tvStart != null) tvStart.setText(doc.getString("startDate"));
        if(tvEnd != null) tvEnd.setText(doc.getString("endDate"));

        String time = (doc.getString("startTime") != null ? doc.getString("startTime") : "") + " - " +
                (doc.getString("endTime") != null ? doc.getString("endTime") : "");
        if(tvTime != null) tvTime.setText(time);

        if(tvCategory != null) tvCategory.setText(doc.getString("category"));
        if(tvLink != null) tvLink.setText(doc.getString("link"));
        if(tvDesc != null) tvDesc.setText(doc.getString("description"));

        if(tvOrganizer != null) tvOrganizer.setText(doc.getString("organizerName"));
        if(tvStudent != null) tvStudent.setText(doc.getString("studentName"));
        if(tvMatrix != null) tvMatrix.setText(doc.getString("matrixNumber"));
        if(tvPhone != null) tvPhone.setText(doc.getString("phone"));

        if (tvEntryFee != null) {
            String fee = doc.getString("entryFee");
            if (fee != null && !fee.isEmpty()) {
                tvEntryFee.setText("MYR " + fee);
            } else {
                tvEntryFee.setText("Free");
            }
        }

        // 3. Status Chip Logic
        String status = doc.getString("status");
        if (tvStatusChip != null) {
            if (status != null) {
                tvStatusChip.setText(status);
                String normalizedStatus = status.toLowerCase();

                if (normalizedStatus.contains("approve")) {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_status_approve);
                    tvStatusChip.setTextColor(Color.parseColor("#155724"));
                } else if (normalizedStatus.contains("reject")) {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_status_reject);
                    tvStatusChip.setTextColor(Color.parseColor("#721C24"));
                } else {
                    tvStatusChip.setBackgroundResource(R.drawable.bg_status_pending);
                    tvStatusChip.setTextColor(Color.parseColor("#856404"));
                }
            } else {
                tvStatusChip.setText("Pending");
                tvStatusChip.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatusChip.setTextColor(Color.parseColor("#856404"));
            }
        }

        // 4. Render Poster
        String posterBase64 = doc.getString("posterBase64");
        if (ivPoster != null) {
            if (posterBase64 != null && !posterBase64.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(posterBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivPoster.setImageBitmap(decodedByte);
                    ivPoster.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    ivPoster.setVisibility(View.GONE);
                }
            } else {
                ivPoster.setVisibility(View.GONE);
            }
        }

        // 5. Render PDF
        String pdfBase64 = doc.getString("approvalBase64");
        if (pdfContainer != null) {
            if (pdfBase64 != null && !pdfBase64.isEmpty()) {
                renderPdfFromBase64(pdfBase64, pdfContainer);
            } else {
                TextView noPdf = new TextView(getContext());
                noPdf.setText("No PDF attached");
                pdfContainer.addView(noPdf);
            }
        }

        if(btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- PDF RENDERING HELPER ---
    private void renderPdfFromBase64(String base64Pdf, LinearLayout container) {
        try {
            byte[] pdfBytes = Base64.decode(base64Pdf, Base64.DEFAULT);
            File tempFile = File.createTempFile("approval_preview", ".pdf", getContext().getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(pdfBytes);
            fos.close();

            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            int pageCount = renderer.getPageCount();

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                ImageView pageImage = new ImageView(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 16);
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
            TextView errorText = new TextView(getContext());
            errorText.setText("Error loading PDF preview: " + e.getMessage());
            container.addView(errorText);
        }
    }
}