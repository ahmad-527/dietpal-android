package com.example.dietpal.tracker;

import com.google.gson.JsonElement;
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
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Read-only product lookup. Explicit submission, no search-as-you-type traffic. */
public final class FoodApi {
  private static final String FIELDS =
      "code,product_name,brands,nutriments,nutrition,product_quantity_unit,nutrition_data_per";
  private static long lastSearch, lastBarcode;

  public interface Service {
    @GET("cgi/search.pl")
    Call<JsonObject> search(
        @Query("search_terms") String query,
        @Query("search_simple") int simple,
        @Query("action") String action,
        @Query("json") int json,
        @Query("page_size") int pageSize,
        @Query("fields") String fields);

    @GET("api/v3.6/product/{code}")
    Call<JsonObject> barcode(@Path("code") String code, @Query("fields") String fields);
  }

  public interface Listener {
    void loaded(List<Food> foods);

    void failed(String message);
  }

  private final Service service;
  private Call<JsonObject> active;
  private int generation;

  public FoodApi() {
    this("https://world.openfoodfacts.org/", false);
  }

  public FoodApi(String url, boolean staging) {
    OkHttpClient client =
        new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(
                chain -> {
                  okhttp3.Request.Builder request =
                      chain
                          .request()
                          .newBuilder()
                          .header(
                              "User-Agent",
                              "DietPal/2.0 (https://github.com/ahmad-527/dietpal-android)");
                  if (staging)
                    request.header("Authorization", okhttp3.Credentials.basic("off", "off"));
                  return chain.proceed(request.build());
                })
            .build();
    service =
        new Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Service.class);
  }

  public void cancel() {
    generation++;
    if (active != null) active.cancel();
    active = null;
  }

  public void search(String input, Listener listener) {
    cancel();
    String query = input.trim();
    boolean barcode = query.matches("[0-9]{8,14}");
    if (query.length() < 2 || query.length() > 100) {
      listener.failed("Enter a food name or a barcode (8–14 digits).");
      return;
    }
    synchronized (FoodApi.class) {
      long now = android.os.SystemClock.elapsedRealtime();
      long previous = barcode ? lastBarcode : lastSearch;
      long gap = barcode ? 4100 : 6100;
      if (previous != 0 && now - previous < gap) {
        listener.failed(
            "Please wait a few seconds before the next lookup, or add a food manually.");
        return;
      }
      if (barcode) lastBarcode = now;
      else lastSearch = now;
    }
    final int request = generation;
    active =
        barcode
            ? service.barcode(query, FIELDS)
            : service.search(query, 1, "process", 1, 15, FIELDS);
    active.enqueue(
        new Callback<JsonObject>() {
          public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            if (request != generation || call.isCanceled()) return;
            if (!response.isSuccessful() || response.body() == null) {
              listener.failed(
                  "Food lookup is unavailable. Retry later or enter the label values manually.");
              return;
            }
            try {
              listener.loaded(parse(response.body(), barcode));
            } catch (RuntimeException invalid) {
              listener.failed("Food data could not be read. You can still add a food manually.");
            }
          }

          public void onFailure(Call<JsonObject> call, Throwable error) {
            if (request == generation && !call.isCanceled())
              listener.failed(
                  "Could not reach the food database. Check your connection or add a food"
                      + " manually.");
          }
        });
  }

  public static List<Food> parse(JsonObject object, boolean barcode) {
    List<Food> foods = new ArrayList<>();
    List<JsonElement> products = new ArrayList<>();
    if (barcode) {
      if (object.has("product") && object.get("product").isJsonObject())
        products.add(object.get("product"));
    } else if (object.has("products") && object.get("products").isJsonArray())
      for (JsonElement product : object.getAsJsonArray("products")) products.add(product);
    else throw new IllegalArgumentException("Invalid food response");
    for (JsonElement element : products) {
      if (!element.isJsonObject()) continue;
      JsonObject product = element.getAsJsonObject();
      String name = text(product, "product_name");
      if (name.isEmpty()) continue;
      Food food = new Food();
      String brand = text(product, "brands");
      food.name = (brand.isEmpty() ? name : name + " · " + brand);
      if (food.name.length() > 200) food.name = food.name.substring(0, 200);
      food.code = text(product, "code");
      food.source = "Open Food Facts";
      JsonObject nutrients =
          product.has("nutriments") && product.get("nutriments").isJsonObject()
              ? product.getAsJsonObject("nutriments")
              : new JsonObject();
      JsonObject nutrition = object(product, "nutrition"),
          set = object(nutrition, "aggregated_set"),
          structured = object(set, "nutrients");
      if (!structured.entrySet().isEmpty()) {
        String per = text(set, "per");
        if (per.equals("100ml")) food.unit = "ml";
        else if (per.equals("100g")) food.unit = "g";
        else if (per.equals("serving")) food.unit = "portion";
        else {
          food.source = "Open Food Facts · nutrition basis unavailable";
          foods.add(food);
          continue;
        }
        if (text(set, "preparation").equals("prepared")) {
          food.name += " (prepared)";
          food.source += " · prepared product";
        }
        food.kcal = structured(structured, "energy-kcal", "kcal");
        if (food.kcal == null) {
          Double kj = structured(structured, "energy-kj", "kj");
          if (kj == null) kj = structured(structured, "energy", "kj");
          if (kj != null) food.kcal = kj / 4.184;
        }
        food.protein = structured(structured, "proteins", "g");
        food.fat = structured(structured, "fat", "g");
        food.fiber = structured(structured, "fiber", "g");
        food.carbs = structured(structured, "carbohydrates-total", "g");
        food.availableCarbs = structured(structured, "carbohydrates", "g");
      } else {
        String quantityUnit = text(product, "product_quantity_unit");
        food.unit = quantityUnit.equals("ml") || quantityUnit.equals("l") ? "ml" : "g";
        food.kcal = number(nutrients, "energy-kcal_100g");
        if (food.kcal == null) {
          Double kj = number(nutrients, "energy-kj_100g");
          if (kj == null) kj = number(nutrients, "energy_100g");
          if (kj != null) food.kcal = kj / 4.184;
        }
        food.protein = number(nutrients, "proteins_100g");
        food.fat = number(nutrients, "fat_100g");
        food.fiber = number(nutrients, "fiber_100g");
        food.carbs = number(nutrients, "carbohydrates-total_100g");
        food.availableCarbs = number(nutrients, "carbohydrates_100g");
      }
      if (food.carbs == null && food.availableCarbs != null && food.fiber != null)
        food.carbs = food.availableCarbs + food.fiber;
      foods.add(food);
    }
    return foods;
  }

  private static JsonObject object(JsonObject parent, String name) {
    JsonElement value = parent.get(name);
    return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
  }

  private static Double structured(JsonObject parent, String key, String expected) {
    JsonObject value = object(parent, key);
    String modifier = text(value, "modifier");
    if (modifier.equals("<") || modifier.equals(">")) return null;
    Double number = number(value, "value");
    if (number == null) return null;
    String unit = text(value, "unit").toLowerCase(java.util.Locale.ROOT);
    if (unit.equals(expected)) return number;
    if (expected.equals("g") && unit.equals("mg")) return number / 1000;
    return null;
  }

  private static String text(JsonObject o, String key) {
    JsonElement e = o.get(key);
    return e != null && e.isJsonPrimitive() ? e.getAsString().trim() : "";
  }

  private static Double number(JsonObject o, String key) {
    try {
      JsonElement e = o.get(key);
      if (e == null || e.isJsonNull()) return null;
      double value = e.getAsDouble();
      return Double.isFinite(value) && value >= 0 ? value : null;
    } catch (RuntimeException invalid) {
      return null;
    }
  }
}
