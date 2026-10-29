package com.example.dietpal.planning;

import com.example.dietpal.recipes.Meal;
import java.util.ArrayList;
import java.util.List;

public final class PlannedDay {
  public String day;
  public PlannerProfile profile;
  public List<Occasion> meals = new ArrayList<>();

  public static final class Occasion {
    public String name;
    public List<Dish> dishes = new ArrayList<>();
  }

  public static final class Dish {
    public Meal recipe;
    public double servings;

    public Dish(Meal meal, double servings) {
      this.recipe = meal;
      this.servings = servings;
    }
  }

  public double[] totals() {
    double[] totals = profile.fixedNutrition();
    for (Occasion occasion : meals)
      for (Dish dish : occasion.dishes) {
        double[] values = dish.recipe.nutrition.vector();
        for (int i = 0; i < 4; i++) totals[i] += values[i] * dish.servings;
      }
    return totals;
  }

  public boolean withinTolerance() {
    return profile.fits(totals());
  }
}
