package com.example.dietpal.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Supplied recipe facts; nutrition is present only for ingredient-calculated recipes. */
public final class Meal {
  public String id = "";
  public String title = "";
  public String category = "";
  public String area = "";
  public String image = "";
  public String sourceUrl = "";
  public String provider = "starter";
  public List<String> ingredients = new ArrayList<>();
  public List<String> steps = new ArrayList<>();
  public String diet = "", mealType = "";
  public List<String> allergens = new ArrayList<>();
  public List<IngredientAmount> ingredientAmounts = new ArrayList<>();
  public Nutrition nutrition;

  public static final class IngredientAmount {
    public String food;
    public double grams;
  }

  public static final class Nutrition {
    public double kcal, protein, carbs, fat, fiber;

    public double[] vector() {
      return new double[] {kcal, protein, carbs, fat};
    }
  }

  public com.example.dietpal.tracker.Food asFood() {
    if (nutrition == null)
      throw new IllegalStateException("No calculated nutrition for this recipe");
    com.example.dietpal.tracker.Food food = new com.example.dietpal.tracker.Food();
    food.name = title;
    food.unit = "portion";
    food.kcal = nutrition.kcal;
    food.protein = nutrition.protein;
    food.carbs = nutrition.carbs;
    food.fat = nutrition.fat;
    food.fiber = nutrition.fiber;
    food.source = "Recipe ingredient estimate · USDA SR Legacy 2018 · one base serving";
    return food;
  }

  public boolean isComplete() {
    return !ingredients.isEmpty() && !steps.isEmpty();
  }

  public boolean matches(String query, String filter) {
    String q = query.trim().toLowerCase(Locale.ROOT);
    boolean categoryMatches =
        filter.equals("All")
            || category.equalsIgnoreCase(filter)
            || (filter.equals("Vegetarian") && (diet.equals("vegetarian") || diet.equals("vegan")))
            || (filter.equals("Vegan") && diet.equals("vegan"))
            || (filter.equals("Breakfast") && mealType.equals("Breakfast"))
            || (filter.equals("Mains") && mealType.equals("Main"))
            || (filter.equals("Sides") && mealType.equals("Side"))
            || (filter.equals("Protein ≥25 g") && nutrition != null && nutrition.protein >= 25);
    // Online search is by meal name; offline/saved search uses the same meaning.
    return categoryMatches && title.toLowerCase(Locale.ROOT).contains(q);
  }

  public String subtitle() {
    if (area.isEmpty()) return category;
    return category.isEmpty() ? area : category + " · " + area;
  }
}
