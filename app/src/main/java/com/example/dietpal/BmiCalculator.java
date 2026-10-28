package com.example.dietpal;

/** Arithmetic only; no diagnosis or weight recommendation. */
final class BmiCalculator {
    private BmiCalculator() {}

    static double calculate(String weightText, String heightText) {
        try {
            double weight = Double.parseDouble(weightText.trim());
            double heightMetres = Double.parseDouble(heightText.trim()) / 100.0;
            if (!Double.isFinite(weight) || !Double.isFinite(heightMetres)
                    || weight <= 0 || heightMetres <= 0) throw new IllegalArgumentException();
            double result = weight / (heightMetres * heightMetres);
            if (!Double.isFinite(result) || result <= 0) throw new IllegalArgumentException();
            return result;
        } catch (NullPointerException | NumberFormatException invalidInput) {
            throw new IllegalArgumentException("Enter positive numeric weight and height", invalidInput);
        }
    }
}
