package com.example.gk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseDetailAdapter extends RecyclerView.Adapter<ExpenseDetailAdapter.ViewHolder> {

    private List<Expense> list;
    private Context context;

    public ExpenseDetailAdapter(Context context, List<Expense> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = list.get(position);
        holder.textNote.setText(expense.getNote());
        holder.textAmount.setText(String.format("%,.0f đ", expense.getAmount()));

        switch (expense.getCategory()) {
            case "fixed":
                holder.textCategory.setText(context.getString(R.string.value_co_dinh));
                break;
            case "salary":
                holder.textCategory.setText(context.getString(R.string.value_luong));
                break;
            default:
                holder.textCategory.setText(context.getString(R.string.value_phat_sinh));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textNote, textAmount, textCategory;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textNote = itemView.findViewById(R.id.textDetailNote);
            textAmount = itemView.findViewById(R.id.textDetailAmount);
            textCategory = itemView.findViewById(R.id.textDetailCategory);
        }
    }
}
