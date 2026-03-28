package com.example.gk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter dùng cho RecyclerView để hiển thị danh sách các khoản chi tiêu (Expense).
 * Mỗi item hiển thị: ghi chú, số tiền, và thời gian/loại khoản chi.
 * Hỗ trợ sự kiện click vào item thông qua interface OnItemClickListener.
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    /**
     * Interface callback để xử lý sự kiện khi người dùng nhấn vào một khoản chi tiêu.
     * Được implement ở nơi sử dụng adapter (ví dụ: DashboardFragment).
     */
    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    private List<Expense> expenseList;       // Danh sách các khoản chi tiêu cần hiển thị
    private OnItemClickListener listener;    // Callback xử lý sự kiện click item

    /**
     * Khởi tạo Adapter với danh sách chi tiêu và listener xử lý click.
     *
     * @param expenseList Danh sách các khoản chi tiêu
     * @param listener    Callback được gọi khi người dùng nhấn vào một item
     */
    public ExpenseAdapter(List<Expense> expenseList, OnItemClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    /**
     * Được gọi khi RecyclerView cần tạo một ViewHolder mới.
     * Inflate layout item_expense.xml để tạo giao diện cho từng dòng trong danh sách.
     */
    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    /**
     * Được gọi để gán dữ liệu vào ViewHolder tại vị trí position.
     * - Hiển thị ghi chú (note) và số tiền đã format.
     * - Nếu loại là "fixed" → hiện "Hằng tháng", ngược lại hiện ngày cụ thể.
     * - Gắn sự kiện click vào item để kích hoạt listener.
     */
    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.textNote.setText(expense.getNote());
        holder.textAmount.setText(String.format("%,.0f đ", expense.getAmount()));

        // Hiển thị thời gian khác nhau tùy theo loại chi tiêu
        switch (expense.getCategory()) {
            case "fixed":
                holder.textTime.setText("Hằng tháng"); // Chi phí cố định: hiện "Hằng tháng"
                break;
            default:
                holder.textTime.setText(expense.getDate()); // Chi tiêu phát sinh: hiện ngày cụ thể
                break;
        }

        // Khi nhấn vào item → gọi callback listener để xử lý (ví dụ: hiện dialog xóa)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(expense);
        });
    }

    /**
     * Trả về tổng số item trong danh sách.
     * RecyclerView dùng giá trị này để biết cần render bao nhiêu dòng.
     */
    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    /**
     * ViewHolder giữ tham chiếu đến các View trong layout item_expense.xml.
     * Tái sử dụng View thay vì tạo mới mỗi lần → tối ưu hiệu năng cho danh sách dài.
     */
    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView textNote;    // Hiển thị ghi chú / mô tả khoản chi
        TextView textAmount;  // Hiển thị số tiền (đã format)
        TextView textTime;    // Hiển thị ngày hoặc "Hằng tháng"

        /**
         * Khởi tạo ViewHolder và bind các TextView từ layout item_expense.xml.
         */
        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            textNote = itemView.findViewById(R.id.textNote);
            textAmount = itemView.findViewById(R.id.textAmount);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}
