package com.example.smartstudyassistant.activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartstudyassistant.R;
import com.example.smartstudyassistant.database.DatabaseHelper;
import com.example.smartstudyassistant.utils.DocumentTextExtractor;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class UploadActivity extends AppCompatActivity {
    private EditText etDocName;
    private Button btnSave;
    private TextView tvFilePath;
    private Toolbar toolbar;
    private Uri selectedFileUri;
    private String originalFileName;

    // File picker launcher
    private final ActivityResultLauncher<String> pickDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    originalFileName = getFileName(uri);
                    tvFilePath.setText(originalFileName);

                    // Set default name (without extension)
                    if (originalFileName != null) {
                        String nameWithoutExt = originalFileName.replaceFirst("[.][^.]+$", "");
                        etDocName.setText(nameWithoutExt);
                    }

                    // Show success feedback
                    Snackbar.make(tvFilePath, "File selected: " + originalFileName,
                            Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(tvFilePath, "No file selected", Snackbar.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize UI components
        etDocName = findViewById(R.id.etDocName);
        btnSave = findViewById(R.id.btnSaveDoc);
        Button btnPickFile = findViewById(R.id.btnPickFile);
        tvFilePath = findViewById(R.id.tvFilePath);

        // Set button click listeners
        btnPickFile.setOnClickListener(v -> pickDocumentLauncher.launch("*/*"));

        btnSave.setOnClickListener(v -> {
            String docName = etDocName.getText().toString().trim();
            if (selectedFileUri == null) {
                Snackbar.make(btnSave, "Please select a document first", Snackbar.LENGTH_LONG).show();
                return;
            }

            if (docName.isEmpty()) {
                etDocName.setError("Document name is required");
                return;
            }

            processDocument();
        });
    }

    private void processDocument() {
        String docName = etDocName.getText().toString().trim();

        // Show processing indicator
        Snackbar processingSnackbar = Snackbar.make(btnSave, "Processing document...", Snackbar.LENGTH_INDEFINITE);
        processingSnackbar.show();

        new Thread(() -> {
            try {
                // Copy file to app storage
                String newFilePath = copyFileToPrivateStorage(selectedFileUri, docName);
                if (newFilePath == null) {
                    runOnUiThread(() -> {
                        processingSnackbar.dismiss();
                        Snackbar.make(btnSave, "Failed to save file", Snackbar.LENGTH_LONG).show();
                    });
                    return;
                }

                // Verify file exists
                File file = new File(newFilePath);
                if (!file.exists()) {
                    runOnUiThread(() -> {
                        processingSnackbar.dismiss();
                        Snackbar.make(btnSave, "File not saved correctly", Snackbar.LENGTH_LONG).show();
                    });
                    return;
                }

                // Add to database
                DatabaseHelper db = new DatabaseHelper(UploadActivity.this);
                long docId = db.addDocument(docName, newFilePath);

                // Extract text
                List<String> chunks = DocumentTextExtractor.extractText(UploadActivity.this, newFilePath);

                runOnUiThread(() -> {
                    processingSnackbar.dismiss();

                    if (chunks == null || chunks.isEmpty()) {
                        Snackbar.make(btnSave, "Failed to extract text from document",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    // Save chunks to database
                    for (String chunk : chunks) {
                        db.addChunk((int) docId, chunk);
                    }

                    // Show success message
                    Snackbar successSnackbar = Snackbar.make(
                            btnSave,
                            "Document processed! " + chunks.size() + " chunks created",
                            Snackbar.LENGTH_LONG
                    );
                    successSnackbar.setAction("OK", v -> finish());
                    successSnackbar.show();
                });
            } catch (Exception e) {
                Log.e("UploadActivity", "Error processing document", e);
                runOnUiThread(() -> {
                    processingSnackbar.dismiss();
                    Snackbar.make(btnSave, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"));
                }
            } catch (Exception e) {
                Log.e("UploadActivity", "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String copyFileToPrivateStorage(Uri uri, String newName) {
        // Create documents directory if needed
        File documentsDir = new File(getFilesDir(), "documents");
        if (!documentsDir.exists() && !documentsDir.mkdirs()) {
            Log.e("UploadActivity", "Failed to create documents directory");
            return null;
        }

        // Preserve original file extension
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        File newFile = new File(documentsDir, newName + extension);

        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return newFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("UploadActivity", "File copy error", e);
            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}