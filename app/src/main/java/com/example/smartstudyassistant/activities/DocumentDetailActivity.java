package com.example.smartstudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartstudyassistant.R;
import com.example.smartstudyassistant.adapters.DocumentAdapter;
import com.example.smartstudyassistant.database.DatabaseHelper;
import com.example.smartstudyassistant.models.Document;

import java.util.List;

public class DocumentDetailActivity extends AppCompatActivity {
    private RecyclerView rvDocuments;
    private DocumentAdapter adapter;
    private List<Document> docList;
    private LinearLayout emptyStateLayout;
    private Button btnUpload;
    private Toolbar toolbar;
    private MenuItem deleteAllItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Documents");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        rvDocuments = findViewById(R.id.rvDocuments);
        emptyStateLayout = findViewById(R.id.emptyState);
        btnUpload = findViewById(R.id.btnUpload);

        rvDocuments.setLayoutManager(new LinearLayoutManager(this));

        // Set upload button listener
        btnUpload.setOnClickListener(v -> {
            startActivity(new Intent(this, UploadActivity.class));
        });

        loadDocuments();
    }

    private void loadDocuments() {
        DatabaseHelper db = new DatabaseHelper(this);
        docList = db.getAllDocuments();

        // Initialize or update adapter
        if (adapter == null) {
            adapter = new DocumentAdapter(docList, this);
            rvDocuments.setAdapter(adapter);
        } else {
            adapter.updateDocumentList(docList);
        }

        updateEmptyState();
        invalidateOptionsMenu(); // Refresh menu visibility
    }

    private void updateEmptyState() {
        if (docList == null || docList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            rvDocuments.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            rvDocuments.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document_menu, menu);
        return true;
    }

    //  Dynamically show/hide delete all icon
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        deleteAllItem = menu.findItem(R.id.action_delete_all);
        if (deleteAllItem != null) {
            deleteAllItem.setVisible(docList != null && !docList.isEmpty());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            showDeleteAllConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteAllConfirmation() {
        if (docList.isEmpty()) {
            Toast.makeText(this, "No documents to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Delete All Documents")
                .setMessage("All documents and their content will be permanently deleted. This cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllDocuments())
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void deleteAllDocuments() {
        DatabaseHelper db = new DatabaseHelper(this);
        db.deleteAllDocuments();

        // Clear UI
        docList.clear();
        adapter.updateDocumentList(docList);
        updateEmptyState();
        invalidateOptionsMenu(); // Refresh menu visibility

        Toast.makeText(this, "All documents deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }
}