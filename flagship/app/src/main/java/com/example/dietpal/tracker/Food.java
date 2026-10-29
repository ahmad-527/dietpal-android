package com.example.dietpal.tracker;

/** Label values per 100 g, or per one explicitly declared portion. Null means unknown. */
public final class Food {
  public String name = "";
  public String unit = "g";
  public String source = "Manual entry";
  public String code = "";
  public Double kcal, protein, carbs, fat, fiber, availableCarbs;

  public void validate(double amount) {
    if (name.trim().isEmpty() || name.length() > 200)
      throw new IllegalArgumentException("Enter a food name (up to 200 characters).");
    if (!unit.equals("g") && !unit.equals("ml") && !unit.equals("portion"))
      throw new IllegalArgumentException("Choose grams, millilitres or portions.");
    if (!Double.isFinite(amount) || amount <= 0 || amount > 100000)
      throw new IllegalArgumentException("Enter a positive quantity.");
    if (kcal == null)
      throw new IllegalArgumentException(
          "Enter the calories from the label or your verified recipe values.");
    for (Double value : new Double[] {kcal, protein, carbs, fat, fiber, availableCarbs})
      if (value != null && (!Double.isFinite(value) || value < 0 || value > 20000))
        throw new IllegalArgumentException(
            "Nutrition values must be finite, non-negative numbers.");
    if (unit.equals("g"))
      for (Double value : new Double[] {protein, carbs, fat, fiber, availableCarbs})
        if (value != null && value > 100)
          throw new IllegalArgumentException("A nutrient cannot exceed 100 g per 100 g of food.");
  }

  public Double forAmount(Double value, double amount) {
    return value == null ? null : value * amount / (unit.equals("portion") ? 1.0 : 100.0);
  }
}
