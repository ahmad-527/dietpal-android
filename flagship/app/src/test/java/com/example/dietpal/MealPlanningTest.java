package com.example.dietpal;

import static org.junit.Assert.*;

import com.example.dietpal.planning.*;
import com.example.dietpal.recipes.Meal;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import org.junit.Test;

public class MealPlanningTest {
  private List<Meal> catalog() throws Exception {
    return new Gson()
        .fromJson(
            new String(
                Files.readAllBytes(Paths.get("src/main/assets/balanced_recipes.json")),
                StandardCharsets.UTF_8),
            new TypeToken<List<Meal>>() {}.getType());
  }

  private PlannerProfile profile(String diet, int count) {
    PlannerProfile p = new PlannerProfile();
    p.effectiveFrom = "2026-10-29";
    p.diet = diet;
    p.mealsPerDay = count;
    p.kcal = 2000;
    p.protein = 120;
    p.carbs = 230;
    p.fat = 65;
    return p;
  }

  @Test
  public void recipeEstimatesReconcileToEveryDeclaredIngredient() throws Exception {
    JsonObject data =
        JsonParser.parseString(
                new String(
                    Files.readAllBytes(Paths.get("src/main/assets/nutrition_ingredients.json")),
                    StandardCharsets.UTF_8))
            .getAsJsonObject()
            .getAsJsonObject("ingredients");
    List<Meal> catalog = catalog();
    assertEquals(40, catalog.size());
    Set<String> ids = new HashSet<>();
    for (Meal meal : catalog) {
      assertTrue(ids.add(meal.id));
      assertTrue(meal.isComplete());
      assertEquals(meal.ingredients.size(), meal.ingredientAmounts.size());
      double[] calculated = new double[4];
      Set<String> allergens = new HashSet<>();
      for (Meal.IngredientAmount amount : meal.ingredientAmounts) {
        assertTrue(amount.grams > 0);
        JsonObject food = data.getAsJsonObject(amount.food), n = food.getAsJsonObject("per100g");
        String[] keys = {"kcal", "protein", "carbs", "fat"};
        for (int i = 0; i < 4; i++)
          calculated[i] += amount.grams / 100 * n.get(keys[i]).getAsDouble();
        for (com.google.gson.JsonElement a : food.getAsJsonArray("allergens"))
          allergens.add(a.getAsString());
      }
      assertArrayEquals(calculated, meal.nutrition.vector(), .00001);
      assertEquals(allergens, new HashSet<>(meal.allergens));
      assertEquals(
          meal.nutrition.kcal * 1.5, meal.asFood().forAmount(meal.asFood().kcal, 1.5), .00001);
    }
  }

  @Test
  public void calendarPeriodsHandleMonthEndsAndLeapYears() {
    assertEquals(
        LocalDate.of(2026, 2, 28), MealPlanner.endExclusive(LocalDate.of(2026, 1, 31), "Month"));
    assertEquals(
        366,
        java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.of(2023, 3, 1), MealPlanner.endExclusive(LocalDate.of(2023, 3, 1), "Year")));
  }

  @Test
  public void requestedMealCountsAreRealOccasionsWithoutDuplicateDishes() throws Exception {
    for (int count : new int[] {1, 2, 3, 4, 5, 6}) {
      PlannedDay day =
          MealPlanner.generate(
              LocalDate.of(2026, 10, 29), profile("mixed", count), catalog(), new HashMap<>(), 0);
      assertEquals(count, day.meals.size());
      Set<String> used = new HashSet<>();
      for (PlannedDay.Occasion meal : day.meals) {
        assertFalse(meal.dishes.isEmpty());
        for (PlannedDay.Dish dish : meal.dishes) {
          assertTrue(used.add(dish.recipe.id));
          assertTrue(dish.servings >= .5 && dish.servings <= 2);
        }
      }
    }
  }

  @Test
  public void dietsAndExclusionsAreHardConstraints() throws Exception {
    PlannerProfile p = profile("vegan", 3);
    p.excludedFoods.add("avocado");
    p.excludedAllergens.add("tree nuts");
    PlannedDay day =
        MealPlanner.generate(LocalDate.of(2026, 10, 29), p, catalog(), new HashMap<>(), 0);
    for (PlannedDay.Occasion meal : day.meals)
      for (PlannedDay.Dish dish : meal.dishes) {
        assertEquals("vegan", dish.recipe.diet);
        assertFalse(dish.recipe.allergens.contains("tree nuts"));
        for (Meal.IngredientAmount ingredient : dish.recipe.ingredientAmounts)
          assertNotEquals("avocado", ingredient.food);
      }
  }

  @Test
  public void feasibleReferenceWeekFitsEachTargetAndVariesRecipes() throws Exception {
    for (String diet : new String[] {"mixed", "vegetarian", "vegan"}) {
      Map<String, Integer> use = new HashMap<>();
      Set<String> distinct = new HashSet<>();
      for (int i = 0; i < 7; i++) {
        PlannedDay day =
            MealPlanner.generate(
                LocalDate.of(2026, 10, 29).plusDays(i), profile(diet, 3), catalog(), use, 0);
        assertTrue(
            diet + " " + day.day + " " + Arrays.toString(day.totals()), day.withinTolerance());
        for (PlannedDay.Occasion meal : day.meals)
          for (PlannedDay.Dish dish : meal.dishes) {
            use.merge(dish.recipe.id, 1, Integer::sum);
            distinct.add(dish.recipe.id);
          }
      }
      assertTrue(diet + " needs meaningful variation", distinct.size() >= 10);
    }
  }

  @Test
  public void impossibleTargetsRemainExplicitlyUnmet() throws Exception {
    PlannerProfile p = profile("mixed", 3);
    p.protein = 1500;
    PlannedDay day =
        MealPlanner.generate(LocalDate.of(2026, 10, 29), p, catalog(), new HashMap<>(), 0);
    assertFalse(day.withinTolerance());
  }

  @Test
  public void emptyEligiblePoolFailsWithoutInventingMeals() throws Exception {
    PlannerProfile p = profile("vegan", 3);
    p.excludedFoods.addAll(Arrays.asList("oats", "bread", "quinoa"));
    try {
      MealPlanner.generate(LocalDate.now(), p, catalog(), new HashMap<>(), 0);
      fail();
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("too few"));
    }
  }

  @Test
  public void replacementsRespectPreferencesAndDoNotMutateOtherDishes() throws Exception {
    PlannerProfile p = profile("vegan", 2);
    PlannedDay day =
        MealPlanner.generate(LocalDate.of(2026, 10, 29), p, catalog(), new HashMap<>(), 0);
    String before = new Gson().toJson(day);
    PlannedDay.Dish old = day.meals.get(0).dishes.get(0);
    List<PlannedDay.Dish> choices = MealPlanner.replacements(day, old, catalog());
    assertFalse(choices.isEmpty());
    for (PlannedDay.Dish choice : choices) {
      assertTrue(p.allows(choice.recipe));
      assertNotEquals(old.recipe.id, choice.recipe.id);
    }
    assertEquals(before, new Gson().toJson(day));
  }
}
