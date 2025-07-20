package com.example.smartstudyassistant.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartstudyassistant.R;
import com.example.smartstudyassistant.adapters.QAAdapter;
import com.example.smartstudyassistant.database.DatabaseHelper;
import com.example.smartstudyassistant.models.QAItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView rvHistory;
    private QAAdapter adapter;
    private List<QAItem> allQAItems = new ArrayList<>();
    private EditText etSearch;
    private LinearLayout emptyStateLayout;
    private Toolbar toolbar;
    private MenuItem deleteAllItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("History");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvHistory);
        etSearch = findViewById(R.id.etSearch);
        emptyStateLayout = findViewById(R.id.emptyState);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterHistory(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadHistory();
    }

    private void loadHistory() {
        DatabaseHelper db = new DatabaseHelper(this);
        allQAItems = db.getAllQA();

        if (adapter == null) {
            adapter = new QAAdapter(allQAItems, this);
            rvHistory.setAdapter(adapter);
        } else {
            adapter.updateList(allQAItems);
        }

        updateEmptyState();
        invalidateOptionsMenu(); // Update menu visibility
    }

    public void updateEmptyState() {
        if (allQAItems.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }

    private void filterHistory(String query) {
        List<QAItem> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (QAItem item : allQAItems) {
            if (item.getQuestion().toLowerCase().contains(lowerQuery) ||
                    item.getAnswer().toLowerCase().contains(lowerQuery)) {
                filteredList.add(item);
            }
        }

        adapter.updateList(filteredList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    // Dynamic menu visibility
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        deleteAllItem = menu.findItem(R.id.action_delete_all_history);
        if (deleteAllItem != null) {
            deleteAllItem.setVisible(!allQAItems.isEmpty());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all_history) {
            showDeleteAllConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteAllConfirmation() {
        if (allQAItems.isEmpty()) return;

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Delete All History")
                .setMessage("All Q/A history will be permanently deleted. This cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllHistory())
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void deleteAllHistory() {
        DatabaseHelper db = new DatabaseHelper(this);
        db.deleteAllQA();

        allQAItems.clear();
        adapter.updateList(allQAItems);
        updateEmptyState();
        invalidateOptionsMenu();

        Toast.makeText(this, "All history deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }
}