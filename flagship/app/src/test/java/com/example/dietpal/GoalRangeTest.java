package com.example.dietpal;

import static org.junit.Assert.*;

import com.example.dietpal.planning.*;
import com.example.dietpal.recipes.Meal;
import com.example.dietpal.tracker.Food;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import org.junit.Test;

public class GoalRangeTest {
  private List<Meal> catalog() throws Exception {
    return new Gson()
        .fromJson(
            new String(
                Files.readAllBytes(Paths.get("src/main/assets/balanced_recipes.json")),
                StandardCharsets.UTF_8),
            new TypeToken<List<Meal>>() {}.getType());
  }

  private PlannerProfile profile(String energy) {
    PlannerProfile p = new PlannerProfile();
    p.effectiveFrom = "2026-10-29";
    p.setGoals(
        new Goal[] {
          Goal.parse(energy, true),
          Goal.parse("180", true),
          Goal.parse("100-150", true),
          Goal.parse("60", true)
        });
    return p;
  }

  @Test
  public void rangesAndStrictComparisonsKeepTheirMeaning() {
    Goal range = Goal.parse("100-150", false);
    assertTrue(range.accepts(100));
    assertTrue(range.accepts(150));
    assertFalse(range.accepts(99.99));
    assertFalse(range.accepts(150.01));
    Goal above = Goal.parse(">2000", false);
    assertFalse(above.accepts(2000));
    assertTrue(above.accepts(2000.1));
    assertEquals(">2000", above.raw());
    assertEquals("100.25-150.75", Goal.parse("100.25-150.75", false).raw());
    assertTrue(Goal.parse("<=2000", false).accepts(2000));
    assertFalse(Goal.parse("<=2000", false).accepts(2000.1));
  }

  @Test
  public void invalidGoalTextDoesNotBecomeAnUnsetGoal() {
    for (String input : new String[] {"150-100", "-30", "NaN", "1-2-3", "hello", "Infinity", "0"})
      try {
        Goal.parse(input, false);
        fail(input);
      } catch (IllegalArgumentException expected) {
      }
    assertFalse(Goal.parse("", true).enabled());
  }

  @Test
  public void userExampleFitsWithAnOptionalCalorieCeiling() throws Exception {
    Map<String, Integer> use = new HashMap<>();
    for (int i = 0; i < 7; i++) {
      PlannerProfile p = profile("<=2000");
      PlannedDay day =
          MealPlanner.generate(LocalDate.of(2026, 10, 29).plusDays(i), p, catalog(), use, 0);
      assertTrue(day.day + " " + Arrays.toString(day.totals()), day.withinTolerance());
      assertTrue(day.totals()[0] <= 2000);
      assertTrue(day.totals()[2] >= 100 && day.totals()[2] <= 150);
      for (PlannedDay.Occasion meal : day.meals)
        for (PlannedDay.Dish dish : meal.dishes) use.merge(dish.recipe.id, 1, Integer::sum);
    }
  }

  @Test
  public void macroOnlyPlanningDoesNotInventAnEnergyTarget() throws Exception {
    PlannerProfile p = profile("");
    PlannedDay day =
        MealPlanner.generate(LocalDate.of(2026, 10, 29), p, catalog(), new HashMap<>(), 0);
    assertFalse(p.goals()[0].enabled());
    assertTrue(Arrays.toString(day.totals()), day.withinTolerance());
  }

  @Test
  public void chosenSupplementsCountTheirActualCaloriesAndMacros() throws Exception {
    PlannerProfile p = profile("<=2000");
    PlannerProfile.Supplement label = new PlannerProfile.Supplement();
    label.food = new Food();
    label.food.name = "User label whey";
    label.food.unit = "portion";
    label.food.kcal = 120.0;
    label.food.protein = 24.0;
    label.food.carbs = 3.0;
    label.food.fat = 2.0;
    label.servings = 2;
    p.supplements.add(label);
    assertArrayEquals(new double[] {240, 48, 6, 4}, p.fixedNutrition(), 0);
    PlannedDay day =
        MealPlanner.generate(LocalDate.of(2026, 10, 29), p, catalog(), new HashMap<>(), 0);
    double[] fromFood = new double[4];
    for (PlannedDay.Occasion meal : day.meals)
      for (PlannedDay.Dish dish : meal.dishes) {
        double[] n = dish.recipe.nutrition.vector();
        for (int i = 0; i < 4; i++) fromFood[i] += n[i] * dish.servings;
      }
    double[] fixed = p.fixedNutrition();
    for (int i = 0; i < 4; i++) fromFood[i] += fixed[i];
    assertArrayEquals(fromFood, day.totals(), .000001);
    assertEquals(2, p.supplements.get(0).servings, 0);
    assertTrue(Arrays.toString(day.totals()), day.withinTolerance());
  }

  @Test
  public void incompatibleCalorieRequirementIsNotSilentlyChanged() throws Exception {
    PlannerProfile p = profile(">3000");
    PlannedDay day =
        MealPlanner.generate(LocalDate.of(2026, 10, 29), p, catalog(), new HashMap<>(), 0);
    assertFalse(day.withinTolerance());
    assertEquals(">3000", day.profile.goals()[0].raw());
  }

  @Test
  public void milkSupplementCannotBypassVeganOrAllergenChoices() {
    PlannerProfile p = profile("<=2000");
    PlannerProfile.Supplement s = new PlannerProfile.Supplement();
    s.food = new Food();
    s.food.name = "Whey label";
    s.food.unit = "portion";
    s.food.kcal = 120.0;
    s.food.protein = 24.0;
    s.food.carbs = 3.0;
    s.food.fat = 2.0;
    s.servings = 1;
    s.allergens.add("milk");
    p.supplements.add(s);
    p.diet = "vegan";
    try {
      p.validate();
      fail();
    } catch (IllegalArgumentException expected) {
    }
    p.diet = "mixed";
    p.excludedAllergens.add("milk");
    try {
      p.validate();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
