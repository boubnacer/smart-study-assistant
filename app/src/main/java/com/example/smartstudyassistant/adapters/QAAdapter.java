package com.example.smartstudyassistant.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartstudyassistant.activities.HistoryActivity;
import com.example.smartstudyassistant.R;
import com.example.smartstudyassistant.database.DatabaseHelper;
import com.example.smartstudyassistant.models.QAItem;

import java.util.List;

public class QAAdapter extends RecyclerView.Adapter<QAAdapter.ViewHolder> {
    private List<QAItem> qaList;
    private final Context context;
    private final DatabaseHelper dbHelper;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion;
        TextView tvAnswer;
        ImageButton btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    public QAAdapter(List<QAItem> qaList, Context context) {
        this.qaList = qaList;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qa, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QAItem qa = qaList.get(position);
        holder.tvQuestion.setText(qa.getQuestion());
        holder.tvAnswer.setText(qa.getAnswer());

        // NEW: Delete button listener
        holder.btnDelete.setOnClickListener(v -> {
            dbHelper.deleteQA(qa.getId());
            qaList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, qaList.size());
            Toast.makeText(context, "History deleted", Toast.LENGTH_SHORT).show();

            // Notify activity to update empty state
            if (context instanceof HistoryActivity) {
                ((HistoryActivity) context).updateEmptyState();
            }
        });
    }

    @Override
    public int getItemCount() {
        return qaList.size();
    }

    // Update list method
    public void updateList(List<QAItem> newList) {
        qaList = newList;
        notifyDataSetChanged();
    }
}