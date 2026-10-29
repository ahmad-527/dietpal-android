package com.example.dietpal.recipes;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Device-local recipe storage; account credentials are never sent to a recipe provider. */
public final class RecipeStore {
  private final SharedPreferences preferences;
  private final Context context;
  private final Gson gson = new Gson();

  public RecipeStore(Context context) {
    this.context = context.getApplicationContext();
    preferences = this.context.getSharedPreferences("dietpal_recipes_v2", Context.MODE_PRIVATE);
  }

  public List<Meal> starters() {
    try (InputStreamReader reader =
        new InputStreamReader(
            context.getAssets().open("starter_recipes.json"), StandardCharsets.UTF_8)) {
      List<Meal> meals = gson.fromJson(reader, new TypeToken<List<Meal>>() {}.getType());
      return meals == null ? new ArrayList<>() : meals;
    } catch (Exception error) {
      throw new IllegalStateException("Starter collection unavailable", error);
    }
  }

  public List<Meal> balanced() {
    try (InputStreamReader reader =
        new InputStreamReader(
            context.getAssets().open("balanced_recipes.json"), StandardCharsets.UTF_8)) {
      List<Meal> meals = gson.fromJson(reader, new TypeToken<List<Meal>>() {}.getType());
      if (meals == null || meals.isEmpty())
        throw new IllegalStateException("Empty meal collection");
      return meals;
    } catch (Exception error) {
      throw new IllegalStateException("Balanced collection unavailable", error);
    }
  }

  public void remember(Meal meal) {
    Map<String, Meal> details = readMeals("details");
    details.remove(meal.id);
    details.put(meal.id, meal);
    while (details.size() > 100) details.remove(details.keySet().iterator().next());
    preferences.edit().putString("details", gson.toJson(details)).apply();
  }

  public Meal find(String id) {
    Meal saved = readMeals("saved").get(id);
    if (saved != null) return saved;
    Meal cached = readMeals("details").get(id);
    if (cached != null) return cached;
    for (Meal planned : readMeals("plans").values()) if (planned.id.equals(id)) return planned;
    for (Meal meal : starters()) if (meal.id.equals(id)) return meal;
    for (Meal meal : balanced()) if (meal.id.equals(id)) return meal;
    return null;
  }

  public List<Meal> saved() {
    return new ArrayList<>(readMeals("saved").values());
  }

  public boolean isSaved(String id) {
    return readMeals("saved").containsKey(id);
  }

  public void setSaved(Meal meal, boolean saved) {
    if (!meal.isComplete())
      throw new IllegalArgumentException("Only complete recipes can be saved");
    Map<String, Meal> values = readMeals("saved");
    if (saved) values.put(meal.id, meal);
    else values.remove(meal.id);
    preferences.edit().putString("saved", gson.toJson(values)).apply();
  }

  public void cacheSearch(String query, String category, List<Meal> meals) {
    Map<String, List<Meal>> values = searches();
    String key = category + ":" + query;
    values.remove(key);
    values.put(key, meals);
    while (values.size() > 12) values.remove(values.keySet().iterator().next());
    preferences.edit().putString("searches", gson.toJson(values)).apply();
  }

  public List<Meal> cachedSearch(String query, String category) {
    return searches().get(category + ":" + query);
  }

  public Map<String, ShoppingItem> shopping() {
    try {
      Map<String, ShoppingItem> values =
          gson.fromJson(
              preferences.getString("shopping", "{}"),
              new TypeToken<LinkedHashMap<String, ShoppingItem>>() {}.getType());
      return values == null ? new LinkedHashMap<>() : values;
    } catch (RuntimeException malformed) {
      return new LinkedHashMap<>();
    }
  }

  public void addIngredients(Meal meal) {
    addIngredients(meal, 1);
  }

  public void addIngredients(Meal meal, double servings) {
    if (!Double.isFinite(servings) || servings <= 0 || servings > 100)
      throw new IllegalArgumentException("Invalid recipe servings");
    Map<String, ShoppingItem> values = shopping();
    for (int i = 0; i < meal.ingredients.size(); i++) {
      String key = meal.id + (meal.nutrition == null ? "" : ":servings:" + servings) + ":" + i;
      if (!values.containsKey(key))
        values.put(
            key,
            new ShoppingItem(
                meal.title
                    + (meal.nutrition == null
                        ? ""
                        : String.format(
                            java.util.Locale.getDefault(), " · %.2f base servings", servings)),
                meal.nutrition == null
                    ? meal.ingredients.get(i)
                    : meal.ingredients
                        .get(i)
                        .replaceFirst(
                            "^[0-9.]+ g",
                            String.format(
                                java.util.Locale.getDefault(),
                                "%.1f g",
                                meal.ingredientAmounts.get(i).grams * servings))));
    }
    writeShopping(values);
  }

  public void checkItem(String key, boolean checked) {
    Map<String, ShoppingItem> values = shopping();
    if (values.containsKey(key)) values.get(key).checked = checked;
    writeShopping(values);
  }

  public void clearChecked() {
    Map<String, ShoppingItem> values = shopping();
    values.entrySet().removeIf(entry -> entry.getValue().checked);
    writeShopping(values);
  }

  public void plan(String day, String slot, Meal meal) {
    java.time.LocalDate.parse(day);
    if (!java.util.Arrays.asList(com.example.dietpal.tracker.TrackerStore.SLOTS).contains(slot))
      throw new IllegalArgumentException("Invalid meal group");
    if (!meal.isComplete())
      throw new IllegalArgumentException("Load recipe details before planning");
    Map<String, Meal> values = readMeals("plans");
    values.put(day + ":" + slot, meal);
    preferences.edit().putString("plans", gson.toJson(values)).apply();
  }

  public Map<String, Meal> plans() {
    return readMeals("plans");
  }

  public void removePlan(String key) {
    Map<String, Meal> values = readMeals("plans");
    values.remove(key);
    preferences.edit().putString("plans", gson.toJson(values)).apply();
  }

  public Map<String, Object> export() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("savedRecipes", saved());
    data.put("mealPlans", plans());
    data.put("shoppingList", shopping());
    return data;
  }

  public void clearUserData() {
    preferences.edit().clear().apply();
  }

  private void writeShopping(Map<String, ShoppingItem> values) {
    preferences.edit().putString("shopping", gson.toJson(values)).apply();
  }

  private Map<String, Meal> readMeals(String key) {
    try {
      Map<String, Meal> values =
          gson.fromJson(
              preferences.getString(key, "{}"),
              new TypeToken<LinkedHashMap<String, Meal>>() {}.getType());
      return values == null ? new LinkedHashMap<>() : values;
    } catch (RuntimeException malformed) {
      return new LinkedHashMap<>();
    }
  }

  private Map<String, List<Meal>> searches() {
    try {
      Map<String, List<Meal>> values =
          gson.fromJson(
              preferences.getString("searches", "{}"),
              new TypeToken<LinkedHashMap<String, List<Meal>>>() {}.getType());
      return values == null ? new LinkedHashMap<>() : values;
    } catch (RuntimeException malformed) {
      return new LinkedHashMap<>();
    }
  }

  public static final class ShoppingItem {
    public String recipe;
    public String ingredient;
    public boolean checked;

    ShoppingItem(String recipe, String ingredient) {
      this.recipe = recipe;
      this.ingredient = ingredient;
    }
  }
}
