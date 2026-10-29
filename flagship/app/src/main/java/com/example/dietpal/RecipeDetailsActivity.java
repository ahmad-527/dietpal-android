package com.example.dietpal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dietpal.recipes.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;
import java.util.List;

public class RecipeDetailsActivity extends AppCompatActivity {
  private RecipeRepository repository;
  private RecipeStore store;
  private LinearLayout body, root;
  private String id;
  private Bundle restored;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    restored = state;
    id = getIntent().getStringExtra("id");
    if (id == null || !id.matches("(?:mealdb-[0-9]+|starter-[a-z-]+|balanced-[a-z-]+)")) {
      finish();
      return;
    }
    store = new RecipeStore(this);
    repository = new RecipeRepository(this);
    root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Recipe");
    body = RecipeUi.scroll(this, root);
    load();
  }

  private void load() {
    body.removeAllViews();
    body.addView(RecipeUi.text(this, "Getting your recipe…", 26, true));
    ProgressBar progress = new ProgressBar(this);
    body.addView(progress);
    repository.details(
        id,
        new RecipeRepository.Listener() {
          public void loaded(List<Meal> meals, String origin) {
            if (!isFinishing() && !isDestroyed()) show(meals.get(0), origin);
          }

          public void failed(String message) {
            if (isFinishing() || isDestroyed()) return;
            body.removeAllViews();
            TextView error = RecipeUi.text(RecipeDetailsActivity.this, message, 22, true);
            error.setId(R.id.recipe_detail_error);
            body.addView(error);
            MaterialButton retry = RecipeUi.button(RecipeDetailsActivity.this, "Retry", true);
            retry.setOnClickListener(v -> load());
            body.addView(retry);
          }
        });
  }

  private void show(Meal meal, String origin) {
    double requested = getIntent().getDoubleExtra("servings", 1);
    final double servings =
        meal.nutrition != null && Double.isFinite(requested) && requested > 0 && requested <= 100
            ? requested
            : 1;
    body.removeAllViews();
    TextView title = RecipeUi.text(this, meal.title, 25, true);
    title.setId(R.id.recipe_detail_title);
    body.addView(title);
    body.addView(RecipeUi.text(this, meal.subtitle(), 14, false));
    RecipeUi.gap(this, body, 14);
    ImageView image = new ImageView(this);
    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    image.setImageResource(
        meal.nutrition == null
            ? R.drawable.recipe_placeholder
            : meal.mealType.equals("Breakfast")
                ? R.drawable.balanced_breakfast
                : meal.mealType.equals("Main")
                    ? R.drawable.balanced_main
                    : R.drawable.balanced_side);
    body.addView(image, new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 176)));
    if (!meal.image.isEmpty())
      Picasso.get()
          .load(meal.image)
          .placeholder(R.drawable.recipe_placeholder)
          .error(R.drawable.recipe_placeholder)
          .fit()
          .centerCrop()
          .into(image);
    TextView source = RecipeUi.text(this, origin, 12, false);
    source.setId(R.id.recipe_detail_source);
    body.addView(source);
    RecipeUi.gap(this, body, 12);
    if (meal.nutrition != null) {
      LinearLayout nutrition = RecipeUi.cardBody(this, body);
      nutrition.addView(
          RecipeUi.text(
              this,
              String.format(
                  java.util.Locale.getDefault(),
                  "Estimated nutrition · %.2f base servings",
                  servings),
              17,
              true));
      RecipeUi.gap(this, nutrition, 8);
      nutrition.addView(
          RecipeUi.text(
              this,
              com.example.dietpal.planning.PlanningUi.amounts(
                  scaled(meal.nutrition.vector(), servings)),
              14,
              false));
      nutrition.addView(
          RecipeUi.text(
              this,
              String.format(
                  java.util.Locale.getDefault(),
                  "Fibre %.1f g · %s",
                  meal.nutrition.fiber,
                  meal.diet),
              13,
              false));
      nutrition.addView(
          RecipeUi.text(
              this,
              "Calculated from USDA ingredient weights. Brands and preparation can change these"
                  + " values. Check ingredient forms and labels.",
              12,
              false));
      MaterialButton log = RecipeUi.button(this, "Log this recipe", true);
      log.setOnClickListener(
          v ->
              startActivity(
                  new Intent(this, com.example.dietpal.tracker.FoodLogActivity.class)
                      .putExtra("day", getIntent().getStringExtra("day"))
                      .putExtra("food", new com.google.gson.Gson().toJson(meal.asFood()))
                      .putExtra("amount", servings)));
      nutrition.addView(log);
      try (com.example.dietpal.planning.PlanningStore preferences =
          new com.example.dietpal.planning.PlanningStore(this)) {
        boolean hidden = preferences.hiddenRecipes().contains(id);
        MaterialButton exclude =
            RecipeUi.button(
                this, hidden ? "Include in suggestions" : "Exclude from suggestions", false);
        exclude.setOnClickListener(
            v -> {
              try (com.example.dietpal.planning.PlanningStore prefs =
                  new com.example.dietpal.planning.PlanningStore(this)) {
                boolean value = !prefs.hiddenRecipes().contains(id);
                prefs.hideRecipe(id, value);
                exclude.setText(value ? "Include in suggestions" : "Exclude from suggestions");
              }
            });
        nutrition.addView(exclude);
      }
    }
    MaterialButton save =
        RecipeUi.button(this, store.isSaved(id) ? "Saved ✓" : "Save recipe", false);
    save.setId(R.id.recipe_save);
    save.setOnClickListener(
        v -> {
          boolean value = !store.isSaved(id);
          store.setSaved(meal, value);
          save.setText(value ? "Saved ✓" : "Save recipe");
        });
    LinearLayout actions = RecipeUi.row(this);
    actions.addView(save, RecipeUi.weighted(this));
    MaterialButton shopping = RecipeUi.button(this, "Add ingredients to list", true);
    shopping.setId(R.id.recipe_add_ingredients);
    shopping.setOnClickListener(
        v -> {
          store.addIngredients(meal, servings);
          Snackbar.make(root, "Ingredients added to your shopping list", Snackbar.LENGTH_LONG)
              .setAction("Open", open -> startActivity(new Intent(this, ShoppingActivity.class)))
              .show();
        });
    MaterialButton plan = RecipeUi.button(this, "Plan this meal", false);
    plan.setId(R.id.recipe_plan);
    plan.setOnClickListener(v -> com.example.dietpal.tracker.PlanActions.choose(this, meal));
    actions.addView(plan, RecipeUi.weighted(this));
    body.addView(actions);
    body.addView(shopping);
    RecipeUi.gap(this, body, 20);
    body.addView(RecipeUi.text(this, "Ingredients", 20, true));
    body.addView(
        RecipeUi.text(
            this,
            meal.nutrition == null
                ? "Amounts are shown as supplied with this recipe."
                : String.format(
                    java.util.Locale.getDefault(),
                    "Ingredient weights for %.2f base servings",
                    servings),
            13,
            false));
    LinearLayout ingredients = RecipeUi.cardBody(this, body);
    ingredients.setId(R.id.recipe_ingredients);
    for (int i = 0; i < meal.ingredients.size(); i++) {
      MaterialCheckBox check = new MaterialCheckBox(this);
      check.setText(
          meal.nutrition != null
              ? meal.ingredients
                  .get(i)
                  .replaceFirst(
                      "^[0-9.]+ g",
                      String.format(
                          java.util.Locale.getDefault(),
                          "%.1f g",
                          meal.ingredientAmounts.get(i).grams * servings))
              : meal.ingredients.get(i));
      check.setTextColor(RecipeUi.INK);
      check.setTextSize(16);
      check.setMinHeight(RecipeUi.dp(this, 48));
      final String key = "ingredient-" + i;
      check.setTag(key);
      if (restored != null) check.setChecked(restored.getBoolean(key));
      ingredients.addView(check);
    }
    RecipeUi.gap(this, body, 12);
    body.addView(RecipeUi.text(this, "Method", 20, true));
    LinearLayout steps = RecipeUi.column(this);
    steps.setId(R.id.recipe_steps);
    body.addView(steps);
    for (int i = 0; i < meal.steps.size(); i++) {
      LinearLayout step = RecipeUi.cardBody(this, steps);
      step.addView(RecipeUi.text(this, "STEP " + (i + 1), 12, false));
      step.addView(RecipeUi.text(this, meal.steps.get(i), 17, false));
    }
    if (meal.provider.equals("themealdb")) {
      MaterialButton link = RecipeUi.button(this, "View on TheMealDB ↗", false);
      link.setOnClickListener(
          v -> {
            try {
              startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(meal.sourceUrl)));
            } catch (android.content.ActivityNotFoundException unavailable) {
              Snackbar.make(root, "No browser is available", Snackbar.LENGTH_SHORT).show();
            }
          });
      body.addView(link);
    }
    if (meal.nutrition == null)
      body.addView(
          RecipeUi.text(
              this,
              "Nutrition values are not supplied with these recipes. Log verified food or label"
                  + " values in your diary.",
              13,
              false));
  }

  private double[] scaled(double[] values, double servings) {
    for (int i = 0; i < values.length; i++) values[i] *= servings;
    return values;
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    LinearLayout list = findViewById(R.id.recipe_ingredients);
    if (list != null)
      for (int i = 0; i < list.getChildCount(); i++) {
        MaterialCheckBox check = (MaterialCheckBox) list.getChildAt(i);
        state.putBoolean((String) check.getTag(), check.isChecked());
      }
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    if (repository != null) repository.cancel();
    super.onDestroy();
  }
}
