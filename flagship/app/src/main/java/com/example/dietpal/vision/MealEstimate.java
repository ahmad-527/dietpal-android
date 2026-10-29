package com.example.dietpal.vision;

import com.example.dietpal.tracker.Food;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

public final class MealEstimate {
  public final Food food;
  public final double grams;

  private MealEstimate(Food food, double grams) {
    this.food = food;
    this.grams = grams;
  }

  public static List<MealEstimate> parse(String output) {
    String text = output.trim();
    if (text.length() > 30000)
      throw new IllegalArgumentException("The analysis response was too long.");
    if (text.startsWith("```")) {
      int start = text.indexOf('\n');
      int end = text.lastIndexOf("```");
      if (start >= 0 && end > start) text = text.substring(start + 1, end).trim();
    }
    JsonObject root = new JsonParser().parse(text).getAsJsonObject();
    if (!root.has("foods") || !root.get("foods").isJsonArray())
      throw new IllegalArgumentException("The model did not return a usable meal estimate.");
    List<MealEstimate> estimates = new ArrayList<>();
    for (JsonElement value : root.getAsJsonArray("foods")) {
      if (estimates.size() == 6) break;
      try {
        JsonObject item = value.getAsJsonObject();
        Food food = new Food();
        food.name = item.get("name").getAsString().trim();
        if (food.name.length() > 100) continue;
        food.source = "On-device AI estimate · user reviewed";
        food.unit = "g";
        double grams = item.get("grams").getAsDouble();
        if (!Double.isFinite(grams) || grams <= 0 || grams > 10000) continue;
        food.kcal = number(item, "kcalPer100g");
        food.protein = number(item, "proteinPer100g");
        food.carbs = number(item, "carbsPer100g");
        food.fat = number(item, "fatPer100g");
        food.validate(grams);
        estimates.add(new MealEstimate(food, grams));
      } catch (RuntimeException invalid) {
        /* Invalid items are never logged or turned into zeroes. */
      }
    }
    return estimates;
  }

  private static Double number(JsonObject item, String key) {
    JsonElement value = item.get(key);
    if (value == null || value.isJsonNull()) return null;
    double number = value.getAsDouble();
    if (!Double.isFinite(number) || number < 0) throw new IllegalArgumentException();
    return number;
  }
}
