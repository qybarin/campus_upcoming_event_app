package com.example.project;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EventForm extends Fragment {

    // Views
    private TextInputEditText etEventTitle, etLocation, etEventQuota, etStartDate, etEndDate;
    private TextInputEditText etStartTime, etEndTime, etGoogleFormLink, etDescription;
    private TextInputEditText etStudentName, etOrganizerName, etMatrixNumber, etPhoneNumber;
    private TextInputEditText etEntryFee;

    private AutoCompleteTextView actvCategories;
    private TextInputLayout tilCategories, tilEventQuota;
    private RadioGroup rgQuota;
    private RadioButton rbOpenForAll, rbSpecificAmount;
    private Button btnSubmitEvent, btnCancel;
    private LinearLayout backButton;

    // File Upload Views
    private CardView cvUploadPoster, cvPosterLog;
    private CardView cvUploadApproval, cvApprovalLog;
    private TextView tvPosterFileName, tvApprovalFileName;
    private ImageView btnDeletePoster, btnDeleteApproval;

    // Variables
    private Uri posterUri = null, approvalUri = null;
    private final String[] categoryList = {"Exhibition", "Business", "Educational", "Social", "Sports", "Academic","Entertainment", "Other"};
    private ProgressDialog progressDialog;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_form, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews(view);
        return view;
    }

    private void initializeViews(View view) {
        // Text Inputs
        etEventTitle = view.findViewById(R.id.etEventTitle);
        etLocation = view.findViewById(R.id.etLocation);
        tilEventQuota = view.findViewById(R.id.tilEventQuota);
        etEventQuota = view.findViewById(R.id.etEventQuota);
        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);
        etStartTime = view.findViewById(R.id.etStartTime);
        etEndTime = view.findViewById(R.id.etEndTime);
        etGoogleFormLink = view.findViewById(R.id.etGoogleFormLink);
        etDescription = view.findViewById(R.id.etDescription);
        etEntryFee = view.findViewById(R.id.etEntryFee);

        etStudentName = view.findViewById(R.id.etStudentName);
        etOrganizerName = view.findViewById(R.id.etOrganizerName);
        etMatrixNumber = view.findViewById(R.id.etMatrixNumber);
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);

        // Quota Radio
        rgQuota = view.findViewById(R.id.rgQuota);
        rbOpenForAll = view.findViewById(R.id.rbOpenForAll);
        rbSpecificAmount = view.findViewById(R.id.rbSpecificAmount);

        // Dropdown
        actvCategories = view.findViewById(R.id.actvCategories);
        tilCategories = view.findViewById(R.id.tilCategories);

        // Buttons
        btnSubmitEvent = view.findViewById(R.id.btnSubmitEvent);
        btnCancel = view.findViewById(R.id.btnCancel);
        backButton = view.findViewById(R.id.back_button);

        // File Uploads
        cvUploadPoster = view.findViewById(R.id.cvUploadPoster);
        cvPosterLog = view.findViewById(R.id.cvPosterLog);
        tvPosterFileName = view.findViewById(R.id.tvPosterFileName);
        btnDeletePoster = view.findViewById(R.id.btnDeletePoster);

        cvUploadApproval = view.findViewById(R.id.cvUploadApproval);
        cvApprovalLog = view.findViewById(R.id.cvApprovalLog);
        tvApprovalFileName = view.findViewById(R.id.tvApprovalFileName);
        btnDeleteApproval = view.findViewById(R.id.btnDeleteApproval);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDatePickers();
        setupTimePickers();
        setupCategoryDropdown();
        setupQuotaLogic();
        setupFileUploadListeners();
        setupButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            // Ensure the window knows to resize
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            // Force the root view to request a new layout pass
            if (getView() != null) {
                getView().requestLayout();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    // --- QUOTA LOGIC ---
    private void setupQuotaLogic() {
        rbOpenForAll.setChecked(true);
        rgQuota.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbOpenForAll) {
                tilEventQuota.setVisibility(View.GONE);
                etEventQuota.setText("");
            } else {
                tilEventQuota.setVisibility(View.VISIBLE);
            }
        });
    }

    // --- FILE UPLOAD LISTENERS ---
    private void setupFileUploadListeners() {
        cvUploadPoster.setOnClickListener(v -> openFilePicker(1, "image/*"));
        cvUploadApproval.setOnClickListener(v -> openFilePicker(2, "*/*"));

        btnDeletePoster.setOnClickListener(v -> {
            posterUri = null;
            cvPosterLog.setVisibility(View.GONE);
            cvUploadPoster.setVisibility(View.VISIBLE);
        });

        btnDeleteApproval.setOnClickListener(v -> {
            approvalUri = null;
            cvApprovalLog.setVisibility(View.GONE);
            cvUploadApproval.setVisibility(View.VISIBLE);
        });

        // Click "Log" to Preview
        cvPosterLog.setOnClickListener(v -> showFilePreviewDialog(posterUri, "Poster Preview"));
        cvApprovalLog.setOnClickListener(v -> showFilePreviewDialog(approvalUri, "Approval Preview"));
    }

    // --- NEW: SCROLLABLE PREVIEW DIALOG ---
    private void showFilePreviewDialog(Uri uri, String title) {
        if (uri == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Use the new Layout with ScrollView
        View dialogView = inflater.inflate(R.layout.dialog_file_preview, null);

        // Get the Container (LinearLayout inside ScrollView)
        LinearLayout container = dialogView.findViewById(R.id.llPreviewContainer);
        ImageView btnClose = dialogView.findViewById(R.id.btnClosePreview);
        TextView tvTitle = dialogView.findViewById(R.id.tvPreviewTitle);

        tvTitle.setText(title);

        try {
            String mimeType = getActivity().getContentResolver().getType(uri);

            if (mimeType != null && mimeType.startsWith("image")) {
                // CASE 1: Normal Image (JPG/PNG)
                ImageView imageView = new ImageView(getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                imageView.setAdjustViewBounds(true);
                imageView.setImageURI(uri);
                container.addView(imageView);

            } else if (mimeType != null && mimeType.equals("application/pdf")) {
                // CASE 2: PDF - Render ALL Pages
                renderPdfToLayout(uri, container);
            } else {
                // Fallback
                ImageView errorView = new ImageView(getContext());
                errorView.setImageResource(android.R.drawable.ic_menu_info_details);
                container.addView(errorView);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error loading preview", Toast.LENGTH_SHORT).show();
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Helper: Loop through PDF pages and add to Layout
    private void renderPdfToLayout(Uri uri, LinearLayout container) {
        try {
            ParcelFileDescriptor fileDescriptor = getActivity().getContentResolver().openFileDescriptor(uri, "r");
            if (fileDescriptor != null) {
                PdfRenderer renderer = new PdfRenderer(fileDescriptor);
                int pageCount = renderer.getPageCount();

                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);

                    // 1. Create the empty Bitmap
                    Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);

                    // 2. FORCE WHITE BACKGROUND (Add this line!)
                    // This fills the transparent bitmap with white before drawing the PDF text on top.
                    bitmap.eraseColor(android.graphics.Color.WHITE);

                    // 3. Render the PDF content onto the white bitmap
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    // Create ImageView
                    ImageView pageImage = new ImageView(getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);

                    // Add margin (gap) between pages so the Grey background shows in the gap
                    params.setMargins(0, 0, 0, 30);

                    pageImage.setLayoutParams(params);
                    pageImage.setAdjustViewBounds(true);
                    pageImage.setImageBitmap(bitmap);

                    container.addView(pageImage);

                    page.close();
                }
                renderer.close();
                fileDescriptor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Cannot render PDF", Toast.LENGTH_SHORT).show();
        }
    }
    private void openFilePicker(int reqCode, String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        if(reqCode == 2) intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
        startActivityForResult(Intent.createChooser(intent, "Select File"), reqCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            if (getFileSizeInKB(selectedUri) > 700) {
                Toast.makeText(getContext(), "File too big! Max size is 700KB.", Toast.LENGTH_LONG).show();
                return;
            }
            String fileName = getFileName(selectedUri);
            if (requestCode == 1) { // Poster
                posterUri = selectedUri;
                cvPosterLog.setVisibility(View.VISIBLE);
                tvPosterFileName.setText(fileName);
                cvUploadPoster.setVisibility(View.GONE);
            } else if (requestCode == 2) { // Approval
                approvalUri = selectedUri;
                cvApprovalLog.setVisibility(View.VISIBLE);
                tvApprovalFileName.setText(fileName);
                cvUploadApproval.setVisibility(View.GONE);
            }
        }
    }

    @SuppressLint("Range")
    private long getFileSizeInKB(Uri uri) {
        long size = 0;
        try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex);
            }
        } catch (Exception e) {}
        return size / 1024;
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst())
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } catch (Exception e) {}
        }
        if (result == null) result = uri.getLastPathSegment();
        return result;
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        btnSubmitEvent.setOnClickListener(v -> {
            if (validateFields()) {
                saveEventDirectly();
            }
        });
    }

    private boolean validateFields() {
        if (etOrganizerName.getText().toString().trim().isEmpty()) { etOrganizerName.setError("Required"); return false; }
        if (etStudentName.getText().toString().trim().isEmpty()) { etStudentName.setError("Required"); return false; }
        if (etEventTitle.getText().toString().isEmpty()) { etEventTitle.setError("Required"); return false; }
        if (posterUri == null) { Toast.makeText(getContext(), "Poster is required", Toast.LENGTH_SHORT).show(); return false; }
        if (approvalUri == null) { Toast.makeText(getContext(), "Approval letter is required", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    private void saveEventDirectly() {
        if (auth.getCurrentUser() == null) { Toast.makeText(getContext(), "Not logged in!", Toast.LENGTH_SHORT).show(); return; }
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Processing files...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            String posterString = encodeFile(posterUri, true);
            String approvalString = encodeFile(approvalUri, false);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (posterString == null || approvalString == null) {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error processing files.", Toast.LENGTH_LONG).show();
                } else {
                    uploadToFirestore(posterString, approvalString);
                }
            });
        }).start();
    }

    private void uploadToFirestore(String posterString, String approvalString) {
        String eventId = UUID.randomUUID().toString();

        String link = etGoogleFormLink.getText().toString().trim();
        if (!link.startsWith("http") && !link.isEmpty()) link = "https://" + link;
        String quota = rbOpenForAll.isChecked() ? "Open for all" : etEventQuota.getText().toString();
        String fee = etEntryFee.getText().toString().trim();
        if (fee.isEmpty()) {
            fee = "0"; // Default to 0 if empty
        }


        Map<String, Object> event = new HashMap<>();
        event.put("id", eventId);
        event.put("title", etEventTitle.getText().toString().trim());
        event.put("location", etLocation.getText().toString().trim());
        event.put("quota", quota);
        event.put("startDate", etStartDate.getText().toString());
        event.put("endDate", etEndDate.getText().toString());
        event.put("startTime", etStartTime.getText().toString());
        event.put("endTime", etEndTime.getText().toString());
        event.put("category", actvCategories.getText().toString());
        event.put("link", link);
        event.put("description", etDescription.getText().toString());
        event.put("studentName", etStudentName.getText().toString().trim());
        event.put("organizerName", etOrganizerName.getText().toString().trim());
        event.put("matrixNumber", etMatrixNumber.getText().toString());
        event.put("phone", etPhoneNumber.getText().toString());
        event.put("posterBase64", posterString);
        event.put("approvalBase64", approvalString);
        event.put("status", "Pending");
        event.put("uid", auth.getCurrentUser().getUid());
        event.put("entryFee", fee);

        // --- ADDED THIS LINE ---
        event.put("isHidden", false);

        db.collection("events").document(eventId).set(event)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    navigateToSuccess(eventId);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String encodeFile(Uri uri, boolean isImage) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            byte[] bytes;
            if (isImage || getMimeType(uri).startsWith("image")) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                bytes = stream.toByteArray();
            } else {
                bytes = getBytes(inputStream);
            }
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) { return null; }
    }

    private byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
        return byteBuffer.toByteArray();
    }

    private String getMimeType(Uri uri) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension != null) type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (type == null) type = getActivity().getContentResolver().getType(uri);
        return type != null ? type : "";
    }

    private void navigateToSuccess(String eventId) {
        Success_create_event successFragment = new Success_create_event();
        Bundle bundle = new Bundle();
        bundle.putString("eventId", eventId);
        successFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentView, successFragment)
                .addToBackStack(null).commit();
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
    }
    private void showDatePicker(TextInputEditText et) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            et.setText(String.format(Locale.US, "%02d/%02d/%04d", day, month+1, year));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void setupTimePickers() {
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
    }
    private void showTimePicker(TextInputEditText et) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(getContext(), (view, hour, min) -> {
            et.setText(String.format(Locale.US, "%02d:%02d", hour, min));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }
    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList);
        actvCategories.setAdapter(adapter);
    }
}