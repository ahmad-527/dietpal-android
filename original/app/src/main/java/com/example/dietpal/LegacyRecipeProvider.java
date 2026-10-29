package com.example.dietpal;

import com.example.dietpal.Listeners.*;
import com.example.dietpal.Models.*;
import com.example.dietpal.recipes.Meal;
import com.example.dietpal.recipes.MealParser;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

/** Public educational fallback, adapted to the historical recipe UI's existing models. */
final class LegacyRecipeProvider {
  private interface Service {
    @GET("search.php")
    Call<JsonObject> search(@Query("s") String query);

    @GET("filter.php")
    Call<JsonObject> category(@Query("c") String category);

    @GET("lookup.php")
    Call<JsonObject> details(@Query("i") int id);
  }

  private final Service service =
      new Retrofit.Builder()
          .baseUrl("https://www.themealdb.com/api/json/v1/1/")
          .client(
              new OkHttpClient.Builder()
                  .connectTimeout(10, TimeUnit.SECONDS)
                  .readTimeout(15, TimeUnit.SECONDS)
                  .callTimeout(20, TimeUnit.SECONDS)
                  .build())
          .addConverterFactory(GsonConverterFactory.create())
          .build()
          .create(Service.class);
  private final List<Call<?>> calls = new ArrayList<>();
  private final Map<Integer, Meal> cache = new HashMap<>();
  private final Map<Integer, List<Waiting>> awaiting = new HashMap<>();
  private boolean closed;

  void cancel() {
    closed = true;
    for (Call<?> call : new ArrayList<>(calls)) call.cancel();
    calls.clear();
    awaiting.clear();
  }

  private int id(Meal meal) {
    return Integer.parseInt(meal.id.substring(7));
  }

  private void fetch(
      Call<JsonObject> call,
      String category,
      Consumer<List<Meal>> success,
      Consumer<String> error) {
    calls.add(call);
    call.enqueue(
        new Callback<JsonObject>() {
          public void onResponse(Call<JsonObject> c, Response<JsonObject> response) {
            calls.remove(c);
            if (closed || c.isCanceled()) return;
            if (response.isSuccessful())
              try {
                List<Meal> meals = MealParser.parse(response.body(), category);
                for (Meal meal : meals) cache.put(id(meal), meal);
                success.accept(meals);
                return;
              } catch (RuntimeException invalid) {
              }
            error.accept("The recipe service is unavailable. Please retry.");
          }

          public void onFailure(Call<JsonObject> c, Throwable failure) {
            calls.remove(c);
            if (!closed && !c.isCanceled())
              error.accept("Could not reach the recipe service. Check your connection and retry.");
          }
        });
  }

  void getRandomRecipes(RandomRecipeResponseListener listener, List<String> tags) {
    String input = tags.isEmpty() ? "" : tags.get(0).trim();
    String category = "All";
    for (String candidate :
        new String[] {"Vegetarian", "Vegan", "Chicken", "Seafood", "Pasta", "Breakfast", "Dessert"})
      if (candidate.equalsIgnoreCase(input)) category = candidate;
    boolean general = input.isEmpty() || input.equalsIgnoreCase("main course");
    Call<JsonObject> call =
        !category.equals("All") ? service.category(category) : service.search(general ? "" : input);
    fetch(
        call,
        category,
        meals -> {
          RandomRecipeApiResponse response = new RandomRecipeApiResponse();
          response.recipes = new ArrayList<>();
          for (Meal meal : meals) {
            Recipe recipe = new Recipe();
            recipe.id = id(meal);
            recipe.title = meal.title;
            recipe.image = meal.image;
            recipe.sourceName = "TheMealDB";
            response.recipes.add(recipe);
          }
          listener.didFetch(response, "TheMealDB");
        },
        listener::didError);
  }

  private void detail(int id, Consumer<Meal> success, Consumer<String> error) {
    Meal cached = cache.get(id);
    if (cached != null && cached.isComplete()) {
      success.accept(cached);
      return;
    }
    Waiting waiter = new Waiting(success, error);
    if (awaiting.containsKey(id)) {
      awaiting.get(id).add(waiter);
      return;
    }
    List<Waiting> waiting = new ArrayList<>();
    waiting.add(waiter);
    awaiting.put(id, waiting);
    fetch(
        service.details(id),
        "All",
        meals -> {
          List<Waiting> listeners = awaiting.remove(id);
          if (listeners == null) return;
          if (meals.isEmpty() || id(meals.get(0)) != id || !meals.get(0).isComplete()) {
            for (Waiting w : listeners) w.error.accept("Recipe details are unavailable.");
            return;
          }
          for (Waiting w : listeners) w.success.accept(meals.get(0));
        },
        message -> {
          List<Waiting> listeners = awaiting.remove(id);
          if (listeners != null) for (Waiting w : listeners) w.error.accept(message);
        });
  }

  void getRecipeDetails(RecipeDetailsListener listener, int id) {
    detail(
        id,
        meal -> {
          RecipeDetailsResponse response = new RecipeDetailsResponse();
          response.id = id;
          response.title = meal.title;
          response.image = meal.image;
          response.sourceName = "Recipe & image: TheMealDB";
          response.sourceUrl = meal.sourceUrl;
          response.summary = meal.subtitle();
          response.extendedIngredients = new ArrayList<>();
          for (String line : meal.ingredients) {
            ExtendedIngredient ingredient = new ExtendedIngredient();
            ingredient.name = line;
            ingredient.original = line;
            ingredient.image = "";
            response.extendedIngredients.add(ingredient);
          }
          listener.didFetch(response, "TheMealDB");
        },
        listener::didError);
  }

  void getInstructions(InstructionsListener listener, int id) {
    detail(
        id,
        meal -> {
          InstructionsResponse instructions = new InstructionsResponse();
          instructions.name = "Method";
          instructions.steps = new ArrayList<>();
          for (int i = 0; i < meal.steps.size(); i++) {
            Step step = new Step();
            step.number = i + 1;
            step.step = meal.steps.get(i);
            step.ingredients = new ArrayList<>();
            step.equipment = new ArrayList<>();
            instructions.steps.add(step);
          }
          listener.didFetch(java.util.Collections.singletonList(instructions), "TheMealDB");
        },
        listener::didError);
  }

  void getSimilarRecipes(SimilarRecipesListener listener, int id) {
    detail(
        id,
        meal -> {
          if (meal.category.isEmpty()) {
            listener.didFetch(new ArrayList<>(), "TheMealDB");
            return;
          }
          fetch(
              service.category(meal.category),
              meal.category,
              meals -> {
                List<SimilarRecipeResponse> result = new ArrayList<>();
                for (Meal related : meals) {
                  if (id(related) == id) continue;
                  SimilarRecipeResponse response = new SimilarRecipeResponse();
                  response.id = id(related);
                  response.title = related.title;
                  response.image = related.image;
                  result.add(response);
                  if (result.size() == 8) break;
                }
                listener.didFetch(result, "Same category · TheMealDB");
              },
              listener::didError);
        },
        listener::didError);
  }

  private static final class Waiting {
    final Consumer<Meal> success;
    final Consumer<String> error;

    Waiting(Consumer<Meal> success, Consumer<String> error) {
      this.success = success;
      this.error = error;
    }
  }
}
