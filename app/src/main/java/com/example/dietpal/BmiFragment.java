package com.example.dietpal;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dietpal.R;

public class BmiFragment extends Fragment {

    private EditText weightEditText;
    private EditText heightEditText;
    private TextView resultTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bmi, container, false);

        weightEditText = view.findViewById(R.id.editTextWeight);
        heightEditText = view.findViewById(R.id.editTextHeight);
        Button calculateButton = view.findViewById(R.id.btnCalculate);
        resultTextView = view.findViewById(R.id.textViewResult);

        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBmi();
            }
        });

        return view;
    }

    private void calculateBmi() {
        // Get weight and height from EditText fields
        String weightStr = weightEditText.getText().toString();
        String heightStr = heightEditText.getText().toString();

        // Check if weight and height are not empty
        if (weightStr.isEmpty() || heightStr.isEmpty()) {
            resultTextView.setText(getString(R.string.bmi_error));
            return;
        }

        try {
            double bmi = BmiCalculator.calculate(weightStr, heightStr);
            resultTextView.setText(getString(R.string.bmi_result, bmi));
        } catch (IllegalArgumentException invalidInput) {
            resultTextView.setText("Enter positive numeric weight (kg) and height (cm).");
        }
    }
}
