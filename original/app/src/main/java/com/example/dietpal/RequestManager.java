package com.example.dietpal;

import android.content.Context;
import com.example.dietpal.Listeners.InstructionsListener;
import com.example.dietpal.Listeners.RandomRecipeResponseListener;
import com.example.dietpal.Listeners.RecipeDetailsListener;
import com.example.dietpal.Listeners.SimilarRecipesListener;
import com.example.dietpal.Models.InstructionsResponse;
import com.example.dietpal.Models.RandomRecipeApiResponse;
import com.example.dietpal.Models.RecipeDetailsResponse;
import com.example.dietpal.Models.SimilarRecipeResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class RequestManager {
  private final String apiKey;
  private final LegacyRecipeProvider legacy = new LegacyRecipeProvider();

  public String providerName() {
    return apiKey.isEmpty() ? "TheMealDB" : "Spoonacular";
  }

  private final List<Call<?>> pendingCalls = new ArrayList<>();
  Retrofit retrofit =
      new Retrofit.Builder()
          .baseUrl("https://api.spoonacular.com/")
          .addConverterFactory(GsonConverterFactory.create())
          .build();

  public RequestManager(Context context) {
    this(BuildConfig.SPOONACULAR_API_KEY);
  }

  RequestManager(String apiKey) {
    this.apiKey = apiKey == null ? "" : apiKey.trim();
  }

  public void cancelPendingRequests() {
    legacy.cancel();
    for (Call<?> call : new ArrayList<>(pendingCalls)) call.cancel();
    pendingCalls.clear();
  }

  public void getRandomRecipes(RandomRecipeResponseListener listener, List<String> tags) {
    if (apiKey.isEmpty()) {
      legacy.getRandomRecipes(listener, tags);
      return;
    }
    CallRandomRecipes callRandomRecipes = retrofit.create(CallRandomRecipes.class);
    Call<RandomRecipeApiResponse> call = callRandomRecipes.callRandomRecipe(apiKey, "20", tags);
    pendingCalls.add(call);
    call.enqueue(
        new Callback<RandomRecipeApiResponse>() {
          @Override
          public void onResponse(
              Call<RandomRecipeApiResponse> call, Response<RandomRecipeApiResponse> response) {
            pendingCalls.remove(call);

            if (!response.isSuccessful() || response.body() == null) {
              listener.didError(response.message());
              return;
            }
            listener.didFetch(response.body(), response.message());
          }

          @Override
          public void onFailure(Call<RandomRecipeApiResponse> call, Throwable t) {
            pendingCalls.remove(call);
            if (call.isCanceled()) return;
            listener.didError("Could not reach the recipe service. Check your connection.");
          }
        });
  }

  public void getRecipeDetails(RecipeDetailsListener listener, int id) {
    if (apiKey.isEmpty()) {
      legacy.getRecipeDetails(listener, id);
      return;
    }
    CallRecipeDetails callRecipeDetails = retrofit.create(CallRecipeDetails.class);
    Call<RecipeDetailsResponse> call = callRecipeDetails.callRecipeDetails(id, apiKey);
    pendingCalls.add(call);
    call.enqueue(
        new Callback<RecipeDetailsResponse>() {
          @Override
          public void onResponse(
              Call<RecipeDetailsResponse> call, Response<RecipeDetailsResponse> response) {
            pendingCalls.remove(call);
            if (!response.isSuccessful() || response.body() == null) {
              listener.didError(response.message());
              return;
            }
            listener.didFetch(response.body(), response.message());
          }

          @Override
          public void onFailure(Call<RecipeDetailsResponse> call, Throwable t) {
            pendingCalls.remove(call);
            if (call.isCanceled()) return;

            listener.didError("Could not reach the recipe service. Check your connection.");
          }
        });
  }

  private interface CallRandomRecipes {
    @GET("recipes/random")
    Call<RandomRecipeApiResponse> callRandomRecipe(
        @Query("apiKey") String apiKey,
        @Query("number") String number,
        @Query("tags") List<String> tags);
  }

  private interface CallRecipeDetails {
    @GET("recipes/{id}/information")
    Call<RecipeDetailsResponse> callRecipeDetails(
        @Path("id") int id, @Query("apiKey") String apiKey);
  }

  public void getSimilarRecipes(SimilarRecipesListener listener, int id) {
    if (apiKey.isEmpty()) {
      legacy.getSimilarRecipes(listener, id);
      return;
    }
    CallSimilarRecipes callSimilarRecipes = retrofit.create(CallSimilarRecipes.class);
    Call<List<SimilarRecipeResponse>> call = callSimilarRecipes.callSimilarRecipe(id, "8", apiKey);
    pendingCalls.add(call);
    call.enqueue(
        new Callback<List<SimilarRecipeResponse>>() {
          @Override
          public void onResponse(
              Call<List<SimilarRecipeResponse>> call,
              Response<List<SimilarRecipeResponse>> response) {
            pendingCalls.remove(call);
            if (!response.isSuccessful() || response.body() == null) {
              listener.didError(response.message());
              return;
            }
            listener.didFetch(response.body(), response.message());
          }

          @Override
          public void onFailure(Call<List<SimilarRecipeResponse>> call, Throwable t) {
            pendingCalls.remove(call);
            if (call.isCanceled()) return;

            listener.didError("Could not reach the recipe service. Check your connection.");
          }
        });
  }

  public void getInstructions(InstructionsListener listener, int id) {
    if (apiKey.isEmpty()) {
      legacy.getInstructions(listener, id);
      return;
    }

    CallInstructions callInstructions = retrofit.create(CallInstructions.class);
    Call<List<InstructionsResponse>> call = callInstructions.callInstructions(id, apiKey);
    pendingCalls.add(call);
    call.enqueue(
        new Callback<List<InstructionsResponse>>() {
          @Override
          public void onResponse(
              Call<List<InstructionsResponse>> call,
              Response<List<InstructionsResponse>> response) {
            pendingCalls.remove(call);

            if (!response.isSuccessful() || response.body() == null) {
              listener.didError(response.message());
              return;
            }
            listener.didFetch(response.body(), response.message());
          }

          @Override
          public void onFailure(Call<List<InstructionsResponse>> call, Throwable t) {
            pendingCalls.remove(call);
            if (call.isCanceled()) return;

            listener.didError("Could not reach the recipe service. Check your connection.");
          }
        });
  }

  private interface CallSimilarRecipes {
    @GET("recipes/{id}/similar")
    Call<List<SimilarRecipeResponse>> callSimilarRecipe(
        @Path("id") int id, @Query("number") String number, @Query("apiKey") String apiKey);
  }

  private interface CallInstructions {

    @GET("recipes/{id}/analyzedInstructions")
    Call<List<InstructionsResponse>> callInstructions(
        @Path("id") int id, @Query("apiKey") String apiKey);
  }
}
