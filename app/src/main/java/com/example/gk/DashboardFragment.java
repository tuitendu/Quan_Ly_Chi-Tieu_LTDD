package com.example.gk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String PREF_NAME = "expense_prefs";
    private static final String KEY_SALARY = "salary";

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private String currentDate;
    private String currentMonth;
    private boolean isShowingFixed = false;
    private int currentSalaryId = -1;
    private Button buttonUpdateSalary;

    private TextView textBalance;
    private EditText editSalary;
    private EditText editAmount;
    private EditText editNote;
    private Spinner spinnerCategory;
    private RecyclerView recyclerExpenses;
    private TextView textTotalToday;
    private TextView textRemaining;
    private TextView tabChiTieu;
    private TextView tabThuNhap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        dbHelper.migrateOldFixedExpenses(currentMonth);

        textBalance = view.findViewById(R.id.textBalance);
        editSalary = view.findViewById(R.id.editSalary);
        editAmount = view.findViewById(R.id.editAmount);
        editNote = view.findViewById(R.id.editNote);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        recyclerExpenses = view.findViewById(R.id.recyclerExpenses);
        textTotalToday = view.findViewById(R.id.textTotalToday);
        textRemaining = view.findViewById(R.id.textRemaining);
        tabChiTieu = view.findViewById(R.id.tabChiTieu);
        tabThuNhap = view.findViewById(R.id.tabThuNhap);
        buttonUpdateSalary = view.findViewById(R.id.buttonUpdateSalary);
        Button buttonAdd = view.findViewById(R.id.buttonAdd);

        recyclerExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Phát sinh", "Cố định"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        checkSalaryThisMonth();

        buttonUpdateSalary.setOnClickListener(v -> updateSalary());
        buttonAdd.setOnClickListener(v -> addExpense());

        tabChiTieu.setOnClickListener(v -> {
            isShowingFixed = false;
            updateTabUI();
            loadData();
        });

        tabThuNhap.setOnClickListener(v -> {
            isShowingFixed = true;
            updateTabUI();
            loadData();
        });

        loadData();
        updateSummary();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummary();
    }

    private void loadData() {
        List<Expense> list;
        if (isShowingFixed) {
            list = dbHelper.getFixedExpensesForMonth(currentMonth);
        } else {
            list = dbHelper.getVariableExpenses(currentDate);
        }

        ExpenseAdapter adapter = new ExpenseAdapter(list, expense -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn muốn xóa khoản \"" + expense.getNote() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        dbHelper.deleteExpense(expense.getId());
                        loadData();
                        updateSummary();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        recyclerExpenses.setAdapter(adapter);
    }

    private void addExpense() {
        String amountStr = editAmount.getText().toString().trim();
        String note = editNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(requireContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (note.isEmpty()) note = "Không có mô tả";

        String category = spinnerCategory.getSelectedItemPosition() == 0 ? "variable" : "fixed";
        String date = category.equals("variable") ? currentDate : currentMonth;

        dbHelper.addExpense(amount, note, category, date);
        editAmount.setText("");
        editNote.setText("");
        loadData();
        updateSummary();
        Toast.makeText(requireContext(), "Đã thêm khoản chi", Toast.LENGTH_SHORT).show();
    }


    private void checkSalaryThisMonth() {
        Expense existing = dbHelper.getSalaryForMonth(currentMonth);
        if (existing != null) {
            currentSalaryId = existing.getId();
            editSalary.setText(String.valueOf((long) existing.getAmount()));
            buttonUpdateSalary.setText("Sửa");
        } else {
            currentSalaryId = -1;
            buttonUpdateSalary.setText("Cập nhật");
        }
    }

    private void updateSalary() {
        String salaryStr = editSalary.getText().toString().trim();
        if (salaryStr.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập lương", Toast.LENGTH_SHORT).show();
            return;
        }
        double salary;
        try {
            salary = Double.parseDouble(salaryStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (salary <= 0) {
            Toast.makeText(requireContext(), "Lương phải lớn hơn 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentSalaryId == -1) {
            long newId = dbHelper.addExpense(salary, "Lương tháng " + currentMonth, "salary", currentMonth);
            currentSalaryId = (int) newId;
            buttonUpdateSalary.setText("Sửa");
            Toast.makeText(requireContext(), "Đã lưu lương tháng này", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.updateExpenseAmount(currentSalaryId, salary);
            Toast.makeText(requireContext(), "Đã cập nhật lương tháng này", Toast.LENGTH_SHORT).show();
        }
        updateSummary();
    }

    private void updateSummary() {
        double balance = dbHelper.getTotalBalanceAllTime();
        double todayVariable = dbHelper.getTotalVariableForDate(currentDate);

        textBalance.setText(formatCurrency(balance));
        textTotalToday.setText("Tổng chi tiêu hôm nay: " + formatCurrency(todayVariable));
        textRemaining.setText("Số dư ròng hiện tại (đã trừ chi tiêu): " + formatCurrency(balance));
    }

    private void updateTabUI() {
        if (isShowingFixed) {
            tabThuNhap.setTextColor(requireContext().getColor(R.color.colorPrimary));
            tabChiTieu.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        } else {
            tabChiTieu.setTextColor(requireContext().getColor(R.color.colorPrimary));
            tabThuNhap.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        }
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }
}
