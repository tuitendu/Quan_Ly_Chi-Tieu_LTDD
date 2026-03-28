package com.example.gk;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ThongKeFragment extends Fragment {

    // UI views
    private BarChart chartDailyExpense;
    private BarChart chartMonthlySavings;
    private Button buttonSelectDate;
    private TextView textSelectedDate;
    private TextView textDailySubtitle;
    private TextView textMonthlySubtitle;
    private TextView textEmptyList;
    private RecyclerView recyclerExpensesByDate;

    // Data
    private DatabaseHelper dbHelper;
    private ExpenseDetailAdapter detailAdapter;
    private List<Expense> expenseList;
    private Calendar selectedCalendar;

    // Formatters
    private final SimpleDateFormat fmtDbDate    = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat fmtDisplay   = new SimpleDateFormat("dd/MM/yyyy",  Locale.getDefault());
    private final SimpleDateFormat fmtDbMonth   = new SimpleDateFormat("yyyy-MM",     Locale.getDefault());
    private final SimpleDateFormat fmtMonthDay  = new SimpleDateFormat("dd/MM",       Locale.getDefault());
    private final SimpleDateFormat fmtMonthShort = new SimpleDateFormat("MM/yyyy",    Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_thong_ke, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        selectedCalendar = Calendar.getInstance();

        // Bind views
        chartDailyExpense    = view.findViewById(R.id.chartDailyExpense);
        chartMonthlySavings  = view.findViewById(R.id.chartMonthlySavings);
        buttonSelectDate     = view.findViewById(R.id.buttonSelectDate);
        textSelectedDate     = view.findViewById(R.id.textSelectedDate);
        textDailySubtitle    = view.findViewById(R.id.textDailySubtitle);
        textMonthlySubtitle  = view.findViewById(R.id.textMonthlySubtitle);
        textEmptyList        = view.findViewById(R.id.textEmptyList);
        recyclerExpensesByDate = view.findViewById(R.id.recyclerExpensesByDate);

        setupRecyclerView();
        buttonSelectDate.setOnClickListener(v -> showDatePicker());

        // Refresh all data
        refreshAll();

        return view;
    }

    private void setupRecyclerView() {
        expenseList = new ArrayList<>();
        detailAdapter = new ExpenseDetailAdapter(requireContext(), expenseList);
        recyclerExpensesByDate.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerExpensesByDate.setAdapter(detailAdapter);
    }

    private void refreshAll() {
        loadDailyChart();
        loadMonthlyChart();
        loadExpensesForSelectedDate();
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    selectedCalendar.set(year, month, day);
                    loadExpensesForSelectedDate();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadExpensesForSelectedDate() {
        String dbDate      = fmtDbDate.format(selectedCalendar.getTime());
        String displayDate = fmtDisplay.format(selectedCalendar.getTime());

        textSelectedDate.setText("Chọn ngày: " + displayDate);

        expenseList.clear();
        expenseList.addAll(dbHelper.getExpensesForDateDetail(dbDate));
        detailAdapter.notifyDataSetChanged();

        // Show/hide empty state
        if (expenseList.isEmpty()) {
            textEmptyList.setVisibility(View.VISIBLE);
            recyclerExpensesByDate.setVisibility(View.GONE);
        } else {
            textEmptyList.setVisibility(View.GONE);
            recyclerExpensesByDate.setVisibility(View.VISIBLE);
        }
    }

    // ========== BIỂU ĐỒ 1: Chi tiêu 7 ngày qua ==========
    private void loadDailyChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels   = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        double todayTotal = 0;
        String todayStr = fmtDbDate.format(Calendar.getInstance().getTime());

        for (int i = 0; i < 7; i++) {
            String qDate = fmtDbDate.format(cal.getTime());
            String label = fmtMonthDay.format(cal.getTime());

            double val = dbHelper.getTotalVariableForDate(qDate);
            entries.add(new BarEntry(i, (float) val));
            labels.add(label);

            if (qDate.equals(todayStr)) todayTotal = val;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Subtitle
        textDailySubtitle.setText("Tổng chi tiêu hôm nay: " + formatCurrency(todayTotal));

        // Dataset - màu primary (Teal)
        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu (VNĐ)");
        dataSet.setColor(Color.parseColor("#00C9A7"));   // colorPrimary
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return formatCurrencyShort(value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chartDailyExpense.setData(barData);
        styleChart(chartDailyExpense, labels, false);
    }

    // ========== BIỂU ĐỒ 2: Tích lũy 6 tháng qua ==========
    private void loadMonthlyChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels   = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -5);

        double totalSavings = 0;

        for (int i = 0; i < 6; i++) {
            String qMonth = fmtDbMonth.format(cal.getTime());
            String label  = "T." + String.valueOf(cal.get(Calendar.MONTH) + 1);

            double val = dbHelper.getMonthlySavings(qMonth);
            if (val < 0) val = 0; // Không hiển thị cột âm để tránh nhầm lẫn
            entries.add(new BarEntry(i, (float) val));
            labels.add(label);
            totalSavings += val;

            cal.add(Calendar.MONTH, 1);
        }

        textMonthlySubtitle.setText("Tổng tiền tích lũy kế: " + formatCurrency(totalSavings));

        // Dataset - màu accent (Tím)
        BarDataSet dataSet = new BarDataSet(entries, "Tích lũy (VNĐ)");
        dataSet.setColor(Color.parseColor("#845EF7"));   // colorButtonAccent
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return formatCurrencyShort(value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        chartMonthlySavings.setData(barData);
        styleChart(chartMonthlySavings, labels, true);
    }

    // ========== STYLING BIỂU ĐỒ ==========
    private void styleChart(BarChart chart, ArrayList<String> labels, boolean isMonthly) {
        int bgColor        = Color.parseColor("#252637"); // colorCard
        int gridColor      = Color.parseColor("#2D3748"); // colorDivider
        int textColor      = Color.parseColor("#8892A4"); // colorTextSecondary

        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setExtraBottomOffset(6f);

        // X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(textColor);
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());

        // Y Axis Left
        chart.getAxisLeft().setTextColor(textColor);
        chart.getAxisLeft().setTextSize(9f);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(gridColor);
        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setAxisMinimum(0f);

        // Y Axis Right (disable)
        chart.getAxisRight().setEnabled(false);

        // Animation
        chart.animateY(800);
        chart.invalidate();
    }

    // ========== TIỆN ÍCH ==========
    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }

    private String formatCurrencyShort(float val) {
        if (val >= 1_000_000) return String.format("%.1fM", val / 1_000_000);
        if (val >= 1_000)     return String.format("%.0fk", val / 1_000);
        return String.format("%.0f", val);
    }
}
