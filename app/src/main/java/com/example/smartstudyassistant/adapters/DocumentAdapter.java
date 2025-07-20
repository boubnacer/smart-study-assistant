package com.example.smartstudyassistant.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartstudyassistant.activities.DocumentViewActivity;
import com.example.smartstudyassistant.R;
import com.example.smartstudyassistant.database.DatabaseHelper;
import com.example.smartstudyassistant.models.Document;

import java.io.File;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {
    private List<Document> documentList;
    private final Context context;
    private final DatabaseHelper dbHelper;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardDocument;
        ImageView ivIcon;
        TextView tvDocName;
        TextView tvDocInfo;
        ImageButton btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            cardDocument = itemView.findViewById(R.id.cardDocument);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvDocName = itemView.findViewById(R.id.tvDocName);
            tvDocInfo = itemView.findViewById(R.id.tvDocInfo);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    public DocumentAdapter(List<Document> documentList, Context context) {
        this.documentList = documentList;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document doc = documentList.get(position);
        File file = new File(doc.getPath());

        // Set document name
        holder.tvDocName.setText(doc.getName());

        // Handle missing files
        if (!file.exists()) {
            showMissingFileState(holder, doc, position);
            return;
        }

        // Show valid file state
        showValidFileState(holder, doc, file, position); // Pass file object here
    }

    private void showMissingFileState(ViewHolder holder, Document doc, int position) {
        // Visual indicators
        holder.tvDocInfo.setText("File not found");
        holder.tvDocInfo.setTextColor(Color.RED);
        holder.ivIcon.setImageResource(R.drawable.ic_error);
        holder.ivIcon.setColorFilter(Color.RED);
        holder.cardDocument.setAlpha(0.6f);

        // Functionality
        holder.cardDocument.setOnClickListener(null); // Disable click
        holder.btnDelete.setOnClickListener(v -> deleteDocument(position, doc));
    }

    // Added file parameter
    private void showValidFileState(ViewHolder holder, Document doc, File file, int position) {
        // Reset visual properties
        holder.cardDocument.setAlpha(1f);
        holder.tvDocInfo.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        holder.ivIcon.clearColorFilter();

        // Set file info - file is now available
        double sizeMB = file.length() / (1024.0 * 1024.0);
        String extension = getFileExtension(doc.getPath()).toUpperCase();
        holder.tvDocInfo.setText(String.format("%.1f MB • %s", sizeMB, extension));

        // Set appropriate icon
        setFileIcon(holder.ivIcon, extension);

        // Set click listeners
        holder.cardDocument.setOnClickListener(v -> openDocument(doc));
        holder.btnDelete.setOnClickListener(v -> deleteDocument(position, doc));
    }

    private void setFileIcon(ImageView imageView, String extension) {
        switch (extension.toLowerCase()) {
            case "pdf":
                imageView.setImageResource(R.drawable.ic_pdf);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.primary));
                break;
            case "doc":
            case "docx":
                imageView.setImageResource(R.drawable.ic_word);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.secondary));
                break;
            case "txt":
                imageView.setImageResource(R.drawable.ic_text);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
                break;
            default:
                imageView.setImageResource(R.drawable.ic_document);
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
        }
    }

    private void openDocument(Document doc) {
        Intent intent = new Intent(context, DocumentViewActivity.class);
        intent.putExtra("FILE_PATH", doc.getPath());
        context.startActivity(intent);
    }

    private void deleteDocument(int position, Document doc) {
        // Delete from database
        dbHelper.deleteDocument(doc.getId());

        // Delete physical file if exists
        try {
            File file = new File(doc.getPath());
            if (file.exists() && !file.delete()) {
                Log.w("DocumentAdapter", "Failed to delete file: " + doc.getPath());
            }
        } catch (SecurityException e) {
            Log.e("DocumentAdapter", "File delete error", e);
        }

        // Update UI
        documentList.remove(position);
        notifyItemRemoved(position);

        // Show deletion confirmation
        Toast.makeText(context, doc.getName() + " deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    public void updateDocumentList(List<Document> newList) {
        documentList = newList;
        notifyDataSetChanged();
    }

    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return (lastDot != -1 && lastDot < path.length() - 1) ?
                path.substring(lastDot + 1) : "file";
    }
}