package com.example.budgetbuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RecentExpenseAdapter extends RecyclerView.Adapter<RecentExpenseAdapter.ViewHolder> {

    private final Context context;
    private final List<Expense> expenseList;
    private final FirebaseFirestore db;
    private final String uid;

    public RecentExpenseAdapter(Context context, List<Expense> expenseList) {
        this.context = context;
        this.expenseList = expenseList;
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public RecentExpenseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentExpenseAdapter.ViewHolder holder, int position) {
        Expense expense = expenseList.get(position);

        holder.titleTextView.setText(expense.getTitle());
        holder.amountTextView.setText(String.format("৳%.2f", expense.getAmount()));
        holder.categoryTextView.setText(expense.getCategory());
        holder.noteTextView.setText(expense.getNote());
        holder.dateTextView.setText(expense.getDate());

        // Handle delete
        holder.deleteIconImageView.setVisibility(View.VISIBLE);
        holder.deleteIconImageView.setOnClickListener(v -> {
            if (uid != null && expense.getId() != null) {
                db.collection("expenses")
                        .document(uid)
                        .collection("userExpenses")
                        .document(expense.getId())
                        .delete()
                        .addOnSuccessListener(unused -> {
                            expenseList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, expenseList.size());
                            Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(context, "Invalid user or expense ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, amountTextView, categoryTextView, noteTextView, dateTextView;
        ImageView deleteIconImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            noteTextView = itemView.findViewById(R.id.noteTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            deleteIconImageView = itemView.findViewById(R.id.deleteIconImageView); // Make sure it exists
        }
    }
}
