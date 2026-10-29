package com.example.dietpal.recipes;

import android.content.Context;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public final class RecipeRepository {
  public interface Listener {
    void loaded(List<Meal> meals, String origin);

    void failed(String message);
  }

  public interface Service {
    @GET("search.php")
    Call<JsonObject> search(@Query("s") String query);

    @GET("filter.php")
    Call<JsonObject> category(@Query("c") String category);

    @GET("lookup.php")
    Call<JsonObject> details(@Query("i") String id);
  }

  private final RecipeStore store;
  private final Service service;
  private Call<JsonObject> active;
  private int generation;

  public RecipeRepository(Context context) {
    this(
        new RecipeStore(context),
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
            .create(Service.class));
  }

  public RecipeRepository(RecipeStore store, Service service) {
    this.store = store;
    this.service = service;
  }

  public void cancel() {
    generation++;
    if (active != null) active.cancel();
    active = null;
  }

  public List<Meal> offline(String query, String category) {
    List<Meal> cached = store.cachedSearch(query, category);
    return cached != null ? cached : matching(store.starters(), query, category);
  }

  public void browse(String query, String category, Listener listener) {
    cancel();
    int request = generation;
    String search = query.trim();
    active =
        search.isEmpty() && !category.equals("All")
            ? service.category(category)
            : service.search(search);
    active.enqueue(
        new Callback<JsonObject>() {
          public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            if (request != generation || call.isCanceled()) return;
            if (!response.isSuccessful()) {
              fallback(search, category, listener);
              return;
            }
            try {
              List<Meal> meals =
                  matching(
                      MealParser.parse(response.body(), search.isEmpty() ? category : "All"),
                      search,
                      category);
              store.cacheSearch(search, category, meals);
              for (Meal meal : meals) if (meal.isComplete()) store.remember(meal);
              listener.loaded(meals, "Live recipes · TheMealDB");
            } catch (RuntimeException invalid) {
              fallback(search, category, listener);
            }
          }

          public void onFailure(Call<JsonObject> call, Throwable error) {
            if (request == generation && !call.isCanceled()) fallback(search, category, listener);
          }
        });
  }

  private void fallback(String query, String category, Listener listener) {
    List<Meal> cached = store.cachedSearch(query, category);
    if (cached != null) listener.loaded(cached, "Offline · previously loaded recipes");
    else
      listener.loaded(
          matching(store.starters(), query, category), "Offline · DietPal starter collection");
  }

  public void details(String id, Listener listener) {
    cancel();
    Meal cached = store.find(id);
    if (cached != null && cached.isComplete()) {
      listener.loaded(
          java.util.Collections.singletonList(cached),
          cached.provider.equals("balanced")
              ? "DietPal recipe · USDA ingredient estimates"
              : cached.provider.equals("starter")
                  ? "DietPal starter collection"
                  : "TheMealDB · saved on this device");
      return;
    }
    if (id == null || !id.matches("mealdb-[0-9]+")) {
      listener.failed("This recipe is unavailable. Return to recipes and choose another.");
      return;
    }
    int request = generation;
    active = service.details(id.substring(7));
    active.enqueue(
        new Callback<JsonObject>() {
          public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            if (request != generation || call.isCanceled()) return;
            if (response.isSuccessful()) {
              try {
                List<Meal> meals = MealParser.parse(response.body(), "All");
                if (!meals.isEmpty() && meals.get(0).id.equals(id) && meals.get(0).isComplete()) {
                  store.remember(meals.get(0));
                  listener.loaded(meals, "Recipe & image · TheMealDB");
                  return;
                }
              } catch (RuntimeException ignored) {
              }
            }
            listener.failed("Recipe details are unavailable. Please try again.");
          }

          public void onFailure(Call<JsonObject> call, Throwable error) {
            if (request == generation && !call.isCanceled())
              listener.failed("Could not load this recipe. Check your connection and retry.");
          }
        });
  }

  public static List<Meal> matching(List<Meal> meals, String query, String category) {
    List<Meal> result = new ArrayList<>();
    for (Meal meal : meals) if (meal.matches(query, category)) result.add(meal);
    return result;
  }
}
