package com.example.dietpal.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class MealParser {
    private MealParser() { }

    public static List<Meal> parse(JsonObject response, String category) {
        if (response == null || !response.has("meals")) throw new IllegalArgumentException("Invalid meal response");
        List<Meal> meals = new ArrayList<>();
        if (response.get("meals").isJsonNull()) return meals;
        JsonArray array = response.getAsJsonArray("meals");
        for (JsonElement value : array) {
            if (!value.isJsonObject()) continue;
            JsonObject object = value.getAsJsonObject();
            String id = text(object, "idMeal"), title = text(object, "strMeal");
            if (!id.matches("[0-9]+") || title.isEmpty()) continue;
            Meal meal = new Meal();
            meal.id = "mealdb-" + id;
            meal.title = title;
            meal.category = text(object, "strCategory");
            if (meal.category.isEmpty() && !category.equals("All")) meal.category = category;
            meal.area = text(object, "strArea");
            if (meal.area.isEmpty()) meal.area = text(object, "strCountry");
            meal.image = safeImage(text(object, "strMealThumb"));
            meal.provider = "themealdb";
            // Provider page remains available even when the contributor's source URL is absent.
            meal.sourceUrl = "https://www.themealdb.com/meal/" + id;
            for (int i = 1; i <= 20; i++) {
                String ingredient = text(object, "strIngredient" + i);
                if (!ingredient.isEmpty()) {
                    String measure = text(object, "strMeasure" + i);
                    meal.ingredients.add(measure.isEmpty() ? ingredient : measure + " " + ingredient);
                }
            }
            for (String step : text(object, "strInstructions").split("[\\r\\n]+")) {
                String clean = step.trim();
                if (!clean.isEmpty()) meal.steps.add(clean);
            }
            meals.add(meal);
        }
        return meals;
    }

    private static String text(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
    }

    public static String safeImage(String value) {
        try {
            URI uri = URI.create(value);
            if ("https".equals(uri.getScheme()) && "www.themealdb.com".equals(uri.getHost())
                    && uri.getPath().startsWith("/images/media/meals/")) return value;
        } catch (IllegalArgumentException ignored) { }
        return "";
    }
}
