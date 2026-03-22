package com.example.gk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    private List<Expense> expenseList;
    private OnItemClickListener listener;

    public ExpenseAdapter(List<Expense> expenseList, OnItemClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.textNote.setText(expense.getNote());
        holder.textAmount.setText(String.format("%,.0f đ", expense.getAmount()));

        switch (expense.getCategory()) {
            case "fixed":
                holder.textTime.setText("Hằng tháng");
                break;
            case "income":
                holder.textTime.setText(expense.getDate());
                break;
            default:
                holder.textTime.setText(expense.getDate());
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(expense);
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView textNote, textAmount, textTime;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            textNote = itemView.findViewById(R.id.textNote);
            textAmount = itemView.findViewById(R.id.textAmount);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}
