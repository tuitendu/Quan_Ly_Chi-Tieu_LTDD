package com.example.gk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CaiDatFragment extends Fragment {

    private static final String PREF_NAME = "expense_prefs";
    private static final String KEY_SAVING_GOAL = "savingGoal";

    private SharedPreferences sharedPreferences;
    private EditText editSavingGoal;
    private TextView textCurrentGoal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cai_dat, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        editSavingGoal = view.findViewById(R.id.editSavingGoal);
        textCurrentGoal = view.findViewById(R.id.textCurrentGoal);
        Button buttonSaveGoal = view.findViewById(R.id.buttonSaveGoal);

        loadCurrentGoal();

        buttonSaveGoal.setOnClickListener(v -> saveGoal());

        return view;
    }

    private void loadCurrentGoal() {
        String goalStr = sharedPreferences.getString(KEY_SAVING_GOAL, "0");
        double goal = 0;
        try {
            goal = Double.parseDouble(goalStr);
        } catch (NumberFormatException e) {
            goal = 0;
        }
        if (goal > 0) {
            textCurrentGoal.setText(String.format("Mục tiêu hiện tại: %,.0f đ", goal));
            editSavingGoal.setText(goalStr);
        } else {
            textCurrentGoal.setText("Mục tiêu hiện tại: Chưa đặt");
        }
    }

    private void saveGoal() {
        String input = editSavingGoal.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập mục tiêu", Toast.LENGTH_SHORT).show();
            return;
        }
        double goal;
        try {
            goal = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (goal <= 0) {
            Toast.makeText(requireContext(), "Mục tiêu phải lớn hơn 0", Toast.LENGTH_SHORT).show();
            return;
        }
        sharedPreferences.edit().putString(KEY_SAVING_GOAL, String.valueOf(goal)).apply();
        textCurrentGoal.setText(String.format("Mục tiêu hiện tại: %,.0f đ", goal));
        Toast.makeText(requireContext(), "Đã lưu mục tiêu", Toast.LENGTH_SHORT).show();
    }
}
