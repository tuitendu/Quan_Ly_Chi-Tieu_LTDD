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

    // Tên file SharedPreferences và key lưu lương
    private static final String PREF_NAME = "expense_prefs";
    private static final String KEY_SALARY = "salary";

    private DatabaseHelper dbHelper;           // Đối tượng thao tác với cơ sở dữ liệu SQLite
    private SharedPreferences sharedPreferences; // Lưu trữ dữ liệu cục bộ đơn giản
    private String currentDate;                // Ngày hiện tại định dạng "yyyy-MM-dd"
    private String currentMonth;               // Tháng hiện tại định dạng "yyyy-MM"
    private boolean isShowingFixed = false;    // true = đang xem tab Chi phí cố định, false = Chi tiêu hôm nay
    private int currentSalaryId = -1;          // ID bản ghi lương tháng này trong DB (-1 nếu chưa có)
    private Button buttonUpdateSalary;

    private TextView textBalance;
    private EditText editSalary;
    private EditText editAmount;
    private EditText editNote;
    private EditText editIncomeAmount;
    private EditText editIncomeNote;
    private TextView textTotalIncome;
    private RecyclerView recyclerIncomes;
    private Spinner spinnerCategory;
    private RecyclerView recyclerExpenses;
    private TextView textTotalToday;
    private TextView textRemaining;
    private TextView tabChiTieu;
    private TextView tabThuNhap;

    /**
     * Được gọi khi Fragment được tạo lần đầu.
     * Thực hiện: inflate layout, khởi tạo DB, lấy ngày/tháng hiện tại,
     * bind các view, setup Spinner, gắn sự kiện cho các nút và tab,
     * sau đó load dữ liệu và cập nhật tổng kết ban đầu.
     */
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
        // Chuyển đổi các chi phí cố định cũ (nếu có) sang định dạng lưu theo tháng
        dbHelper.migrateOldFixedExpenses(currentMonth);

        textBalance = view.findViewById(R.id.textBalance);
        editSalary = view.findViewById(R.id.editSalary);
        editAmount = view.findViewById(R.id.editAmount);
        editNote = view.findViewById(R.id.editNote);
        editIncomeAmount = view.findViewById(R.id.editIncomeAmount);
        editIncomeNote = view.findViewById(R.id.editIncomeNote);
        textTotalIncome = view.findViewById(R.id.textTotalIncome);
        recyclerIncomes = view.findViewById(R.id.recyclerIncomes);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        recyclerExpenses = view.findViewById(R.id.recyclerExpenses);
        textTotalToday = view.findViewById(R.id.textTotalToday);
        textRemaining = view.findViewById(R.id.textRemaining);
        tabChiTieu = view.findViewById(R.id.tabChiTieu);
        tabThuNhap = view.findViewById(R.id.tabThuNhap);
        buttonUpdateSalary = view.findViewById(R.id.buttonUpdateSalary);
        Button buttonAdd = view.findViewById(R.id.buttonAdd);

        recyclerExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup Spinner chọn loại chi tiêu: Phát sinh (variable) hoặc Cố định (fixed)
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Phát sinh", "Cố định"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        // Kiểm tra xem tháng này đã nhập lương chưa để hiển thị đúng trạng thái nút
        checkSalaryThisMonth();

        recyclerIncomes.setLayoutManager(new LinearLayoutManager(requireContext()));

        buttonUpdateSalary.setOnClickListener(v -> updateSalary());
        buttonAdd.setOnClickListener(v -> addExpense());
        view.findViewById(R.id.buttonAddIncome).setOnClickListener(v -> addIncome());

        // Tab "Chi tiêu hôm nay": hiển thị danh sách chi tiêu phát sinh trong ngày
        tabChiTieu.setOnClickListener(v -> {
            isShowingFixed = false;
            updateTabUI();
            loadData();
        });

        // Tab "Chi phí cố định": hiển thị danh sách chi phí cố định của tháng
        tabThuNhap.setOnClickListener(v -> {
            isShowingFixed = true;
            updateTabUI();
            loadData();
        });

        loadData();
        loadIncomes();
        updateSummary();

        return view;
    }

    /**
     * Được gọi mỗi khi Fragment hiển thị trở lại (ví dụ: sau khi điều hướng từ màn hình khác về).
     * Cập nhật lại tổng kết số dư để đảm bảo dữ liệu luôn mới nhất.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadIncomes();
        updateSummary();
    }

    /**
     * Tải danh sách chi tiêu từ cơ sở dữ liệu và hiển thị lên RecyclerView.
     * - Nếu đang ở tab "Cố định" → lấy chi phí cố định của tháng hiện tại.
     * - Nếu đang ở tab "Phát sinh" → lấy chi tiêu phát sinh của ngày hôm nay.
     * Mỗi item trong danh sách có thể nhấn để xóa (hiện AlertDialog xác nhận trước khi xóa).
     */
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

    private void loadIncomes() {
        List<Expense> incomeList = dbHelper.getIncomeForMonth(currentMonth);
        double totalIncome = 0;
        for (Expense e : incomeList) totalIncome += e.getAmount();
        final double total = totalIncome;
        textTotalIncome.setText("Tổng tháng này: " + formatCurrency(total));

        ExpenseAdapter incomeAdapter = new ExpenseAdapter(incomeList, expense -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xóa thu nhập")
                    .setMessage("Bạn muốn xóa khoản \"" + expense.getNote() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        dbHelper.deleteExpense(expense.getId());
                        loadIncomes();
                        updateSummary();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        recyclerIncomes.setAdapter(incomeAdapter);
    }

    private void addIncome() {
        String amountStr = editIncomeAmount.getText().toString().trim();
        String note = editIncomeNote.getText().toString().trim();
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
        if (note.isEmpty()) note = "Thu nhập khác";
        dbHelper.addExpense(amount, note, "income", currentDate);
        editIncomeAmount.setText("");
        editIncomeNote.setText("");
        loadIncomes();
        updateSummary();
        Toast.makeText(requireContext(), "Đã thêm thu nhập", Toast.LENGTH_SHORT).show();
    }

    /**
     * Xử lý khi người dùng nhấn nút "+ Thêm" để thêm một khoản chi tiêu mới.
     * Validate dữ liệu đầu vào (số tiền không rỗng, hợp lệ, > 0).
     * Xác định loại chi tiêu từ Spinner:
     *   - "Phát sinh" → lưu theo ngày (currentDate)
     *   - "Cố định"   → lưu theo tháng (currentMonth)
     * Sau khi lưu: xóa trắng ô nhập, reload danh sách và cập nhật tổng kết.
     */
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

    /**
     * Kiểm tra xem tháng hiện tại đã có bản ghi lương trong DB chưa.
     * - Nếu đã có: hiện số tiền lương vào ô nhập, đổi nút thành "Sửa", lưu lại ID bản ghi.
     * - Nếu chưa có: reset ID về -1, nút hiện là "Cập nhật".
     */
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

    /**
     * Xử lý khi người dùng nhấn nút "Cập nhật" hoặc "Sửa" để lưu lương tháng này.
     * Validate số tiền lương (không rỗng, hợp lệ, > 0).
     * - Nếu chưa có lương tháng này (currentSalaryId == -1) → thêm bản ghi mới vào DB.
     * - Nếu đã có lương → cập nhật số tiền bản ghi hiện tại.
     * Sau đó cập nhật lại tổng kết số dư.
     */
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
            // Chưa có lương tháng này → thêm mới
            long newId = dbHelper.addExpense(salary, "Lương tháng " + currentMonth, "salary", currentMonth);
            currentSalaryId = (int) newId;
            buttonUpdateSalary.setText("Sửa");
            Toast.makeText(requireContext(), "Đã lưu lương tháng này", Toast.LENGTH_SHORT).show();
        } else {
            // Đã có lương → cập nhật số tiền
            dbHelper.updateExpenseAmount(currentSalaryId, salary);
            Toast.makeText(requireContext(), "Đã cập nhật lương tháng này", Toast.LENGTH_SHORT).show();
        }
        updateSummary();
    }

    /**
     * Cập nhật các TextView hiển thị tổng kết tài chính:
     * - textBalance: số dư tổng (tổng lương - tổng chi tiêu toàn thời gian)
     * - textTotalToday: tổng chi tiêu phát sinh hôm nay
     * - textRemaining: số dư ròng hiện tại (giống textBalance)
     */
    private void updateSummary() {
        double balance = dbHelper.getTotalBalanceAllTime();
        double todayVariable = dbHelper.getTotalVariableForDate(currentDate);

        textBalance.setText(formatCurrency(balance));
        textTotalToday.setText("Tổng chi tiêu hôm nay: " + formatCurrency(todayVariable));
        textRemaining.setText("Số dư ròng hiện tại (đã trừ chi tiêu): " + formatCurrency(balance));
    }

    /**
     * Cập nhật màu sắc của 2 tab để phản ánh tab nào đang được chọn.
     * - Tab đang active → màu colorPrimary (nổi bật)
     * - Tab còn lại → màu colorTextSecondary (mờ)
     */
    private void updateTabUI() {
        if (isShowingFixed) {
            tabThuNhap.setTextColor(requireContext().getColor(R.color.colorPrimary));
            tabChiTieu.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        } else {
            tabChiTieu.setTextColor(requireContext().getColor(R.color.colorPrimary));
            tabThuNhap.setTextColor(requireContext().getColor(R.color.colorTextSecondary));
        }
    }

    /**
     * Định dạng số tiền thành chuỗi có dấu phân cách hàng nghìn và ký hiệu "đ".
     * Ví dụ: 1500000.0 → "1,500,000 đ"
     */
    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }
}
