package com.example.dietpal.planning;

import com.example.dietpal.recipes.Meal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Bounded numerical fitting of ingredient estimates, with hard food exclusions and visible gaps.
 * The 10% display tolerance and serving bounds are engineering choices, not clinical thresholds.
 */
public final class MealPlanner {
  private MealPlanner() {}

  public static LocalDate endExclusive(LocalDate start, String horizon) {
    if (horizon.equals("Week")) return start.plusWeeks(1);
    if (horizon.equals("Month")) return start.plusMonths(1);
    if (horizon.equals("Year")) return start.plusYears(1);
    throw new IllegalArgumentException("Choose week, month or year.");
  }

  public static PlannedDay generate(
      LocalDate day,
      PlannerProfile profile,
      List<Meal> catalog,
      Map<String, Integer> recentUse,
      int variation) {
    profile.validate();
    List<Meal> allowed = new ArrayList<>(),
        breakfast = new ArrayList<>(),
        mains = new ArrayList<>(),
        sides = new ArrayList<>();
    for (Meal meal : catalog)
      if (profile.allows(meal)) {
        allowed.add(meal);
        if (meal.mealType.equals("Breakfast")) breakfast.add(meal);
        else if (meal.mealType.equals("Main")) mains.add(meal);
        else sides.add(meal);
      }
    if (allowed.size() < 4 || breakfast.isEmpty() || mains.size() < 2 || sides.isEmpty())
      throw new IllegalArgumentException(
          "These exclusions leave too few different meals. Change exclusions or add more"
              + " nutrition-backed recipes.");
    int count = Math.max(4, profile.mealsPerDay);
    double[] targets = profile.targets(), fixed = profile.fixedNutrition();
    Random random =
        new Random(
            day.toEpochDay() * 1009L
                + variation * 7919L
                + profile.diet.hashCode()
                + profile.mealsPerDay);
    Meal[] best = null;
    double[] bestPortions = null;
    double bestScore = Double.POSITIVE_INFINITY;
    for (int attempt = 0; attempt < 240; attempt++) {
      if (Thread.currentThread().isInterrupted())
        throw new java.util.concurrent.CancellationException();
      Meal[] choices = new Meal[count];
      HashSet<String> unique = new HashSet<>();
      for (int i = 0; i < count; i++) {
        List<Meal> pool = i == 0 ? breakfast : i < 3 ? mains : sides;
        List<Meal> unused = new ArrayList<>();
        for (Meal meal : pool) if (!unique.contains(meal.id)) unused.add(meal);
        if (unused.isEmpty()) {
          choices = null;
          break;
        }
        choices[i] = unused.get(random.nextInt(unused.size()));
        unique.add(choices[i].id);
      }
      if (choices == null) continue;
      double[] portions = fit(choices, targets, fixed);
      double[] total = totals(choices, portions);
      for (int n = 0; n < 4; n++) total[n] += fixed[n];
      boolean within = profile.fits(total);
      double score = profile.error(total) + (within ? 0 : 1000);
      for (Meal meal : choices) {
        score += .0007 * recentUse.getOrDefault(meal.id, 0);
        if (profile.favouriteRecipes.contains(meal.id)) score -= .0001;
      }
      if (score < bestScore) {
        bestScore = score;
        best = choices;
        bestPortions = portions;
      }
    }
    if (best == null)
      throw new IllegalArgumentException(
          "No varied combination is available for this meal pattern.");
    PlannedDay out = new PlannedDay();
    out.day = day.toString();
    out.profile = profile;
    String[][] names = {
      {"Meal 1"},
      {"Meal 1", "Meal 2"},
      {"Breakfast", "Lunch", "Dinner"},
      {"Breakfast", "Lunch", "Dinner", "Snack"},
      {"Breakfast", "Lunch", "Dinner", "Snack 1", "Snack 2"},
      {"Breakfast", "Lunch", "Dinner", "Snack 1", "Snack 2", "Snack 3"}
    };
    for (String name : names[profile.mealsPerDay - 1]) {
      PlannedDay.Occasion meal = new PlannedDay.Occasion();
      meal.name = name;
      out.meals.add(meal);
    }
    for (int i = 0; i < best.length; i++) {
      int occasion =
          profile.mealsPerDay == 1
              ? 0
              : profile.mealsPerDay == 2 ? (i < 2 ? 0 : 1) : Math.min(i, profile.mealsPerDay - 1);
      out.meals.get(occasion).dishes.add(new PlannedDay.Dish(best[i], bestPortions[i]));
    }
    return out;
  }

  private static double[] fit(Meal[] meals, double[] target, double[] fixed) {
    double[] portions = new double[meals.length], total = fixed.clone();
    java.util.Arrays.fill(portions, 1);
    double[][] vectors = new double[meals.length][];
    for (int i = 0; i < meals.length; i++) {
      vectors[i] = meals[i].nutrition.vector();
      for (int n = 0; n < 4; n++) total[n] += vectors[i][n];
    }
    for (int pass = 0; pass < 40; pass++)
      for (int i = 0; i < meals.length; i++) {
        double[] v = vectors[i];
        double numerator = 0, denominator = 0;
        for (int n = 0; n < 4; n++) {
          if (target[n] <= 0) continue;
          double a = v[n] / target[n];
          numerator += a * (1 - (total[n] - v[n] * portions[i]) / target[n]);
          denominator += a * a;
        }
        double fitted = Math.max(.5, Math.min(2, numerator / Math.max(denominator, 1e-12)));
        for (int n = 0; n < 4; n++) total[n] += v[n] * (fitted - portions[i]);
        portions[i] = fitted;
      }
    for (int i = 0; i < portions.length; i++) portions[i] = Math.round(portions[i] * 20) / 20.0;
    return portions;
  }

  private static double[] totals(Meal[] meals, double[] portions) {
    double[] out = new double[4];
    for (int i = 0; i < meals.length; i++) {
      double[] v = meals[i].nutrition.vector();
      for (int n = 0; n < 4; n++) out[n] += v[n] * portions[i];
    }
    return out;
  }

  public static double error(double[] actual, double[] target) {
    double error = 0;
    for (int i = 0; i < 4; i++)
      if (target[i] > 0) error += Math.pow((actual[i] - target[i]) / target[i], 2);
    return error;
  }

  public static List<PlannedDay.Dish> replacements(
      PlannedDay day, PlannedDay.Dish old, List<Meal> catalog) {
    double[] current = day.totals(),
        target = day.profile.targets(),
        removed = old.recipe.nutrition.vector();
    for (int n = 0; n < 4; n++) current[n] -= removed[n] * old.servings;
    final double[] base = current;
    List<PlannedDay.Dish> result = new ArrayList<>();
    java.util.Set<String> alreadyUsed = new java.util.HashSet<>();
    for (PlannedDay.Occasion occasion : day.meals)
      for (PlannedDay.Dish dish : occasion.dishes) if (dish != old) alreadyUsed.add(dish.recipe.id);
    for (Meal meal : catalog)
      if (!alreadyUsed.contains(meal.id)
          && day.profile.allows(meal)
          && !meal.id.equals(old.recipe.id)
          && meal.mealType.equals(old.recipe.mealType)) {
        double numerator = 0, denominator = 0;
        double[] values = meal.nutrition.vector();
        for (int n = 0; n < 4; n++) {
          if (target[n] <= 0) continue;
          double a = values[n] / target[n];
          numerator += a * (target[n] - base[n]) / target[n];
          denominator += a * a;
        }
        double serving =
            Math.round(Math.max(.5, Math.min(2, numerator / Math.max(denominator, 1e-12))) * 20)
                / 20.0;
        result.add(new PlannedDay.Dish(meal, serving));
      }
    result.sort(
        java.util.Comparator.comparingDouble(
            d -> {
              double[] total = base.clone(), v = d.recipe.nutrition.vector();
              for (int n = 0; n < 4; n++) total[n] += v[n] * d.servings;
              return day.profile.error(total);
            }));
    return result;
  }
}
