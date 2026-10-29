package com.example.dietpal.planning;

import com.example.dietpal.recipes.Meal;
import java.util.ArrayList;
import java.util.List;

/** User preferences and targets, effective from a specific date, never a medical prescription. */
public final class PlannerProfile {
  public String effectiveFrom, diet = "mixed";
  public int mealsPerDay = 3;
  public double kcal, protein, carbs, fat;
  public Goal energyGoal, proteinGoal, carbsGoal, fatGoal;
  public Double waterGoal;
  public List<Supplement> supplements = new ArrayList<>();

  public static final class Supplement {
    public com.example.dietpal.tracker.Food food;
    public double servings;
    public String diet = "vegetarian";
    public List<String> allergens = new ArrayList<>();
  }

  public Goal[] goals() {
    return new Goal[] {
      energyGoal == null ? Goal.point(kcal) : energyGoal,
      proteinGoal == null ? Goal.point(protein) : proteinGoal,
      carbsGoal == null ? Goal.point(carbs) : carbsGoal,
      fatGoal == null ? Goal.point(fat) : fatGoal
    };
  }

  public void setGoals(Goal[] values) {
    energyGoal = values[0];
    proteinGoal = values[1];
    carbsGoal = values[2];
    fatGoal = values[3];
    kcal = values[0].aim();
    protein = values[1].aim();
    carbs = values[2].aim();
    fat = values[3].aim();
  }

  public double[] fixedNutrition() {
    double[] sum = new double[4];
    for (Supplement supplement : supplements) {
      supplement.food.validate(supplement.servings);
      Double[] values = {
        supplement.food.kcal, supplement.food.protein, supplement.food.carbs, supplement.food.fat
      };
      for (int n = 0; n < 4; n++) {
        if (values[n] == null)
          throw new IllegalArgumentException(
              "Enter all nutrition label values for planned supplements.");
        sum[n] += supplement.food.forAmount(values[n], supplement.servings);
      }
    }
    return sum;
  }

  public boolean fits(double[] actual) {
    Goal[] goals = goals();
    for (int i = 0; i < 4; i++) if (!goals[i].accepts(actual[i])) return false;
    return true;
  }

  public double error(double[] actual) {
    double error = 0;
    Goal[] goals = goals();
    for (int i = 0; i < 4; i++)
      if (goals[i].enabled()) {
        error += Math.pow(goals[i].distance(actual[i]), 2);
        error += .00005 * Math.pow((actual[i] - goals[i].aim()) / Math.max(1, goals[i].aim()), 2);
      }
    return error;
  }

  public List<String> excludedFoods = new ArrayList<>();
  public List<String> excludedAllergens = new ArrayList<>();
  public List<String> avoidedRecipes = new ArrayList<>();
  public List<String> favouriteRecipes = new ArrayList<>();

  public double[] targets() {
    Goal[] values = goals();
    return new double[] {values[0].aim(), values[1].aim(), values[2].aim(), values[3].aim()};
  }

  public void validate() {
    java.time.LocalDate.parse(effectiveFrom);
    if (!java.util.Arrays.asList("mixed", "vegetarian", "vegan").contains(diet)
        || mealsPerDay < 1
        || mealsPerDay > 6)
      throw new IllegalArgumentException("Choose a diet and one to six meals per day.");
    Goal[] goals = goals();
    boolean any = false;
    for (int i = 0; i < 4; i++) {
      goals[i].validate(true);
      any |= goals[i].enabled();
    }
    if (!any) throw new IllegalArgumentException("Enter at least one calorie or macro goal.");
    fixedNutrition();
    for (Supplement supplement : supplements) {
      if (diet.equals("vegan") && !supplement.diet.equals("vegan"))
        throw new IllegalArgumentException(
            "A chosen supplement is not marked plant-based. Change it or your diet preference.");
      for (String allergen : supplement.allergens)
        if (excludedAllergens.contains(allergen))
          throw new IllegalArgumentException(
              "A chosen supplement conflicts with the " + allergen + " exclusion.");
    }
  }

  public boolean allows(Meal meal) {
    if (meal.nutrition == null || avoidedRecipes.contains(meal.id)) return false;
    if (diet.equals("vegan") && !meal.diet.equals("vegan")) return false;
    if (diet.equals("vegetarian") && meal.diet.equals("mixed")) return false;
    for (String allergen : meal.allergens) if (excludedAllergens.contains(allergen)) return false;
    for (Meal.IngredientAmount ingredient : meal.ingredientAmounts)
      if (excludedFoods.contains(ingredient.food)) return false;
    return true;
  }
}
