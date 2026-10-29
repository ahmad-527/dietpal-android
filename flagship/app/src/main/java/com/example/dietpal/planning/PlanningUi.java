package com.example.dietpal.planning;

import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.example.dietpal.RecipeDetailsActivity;
import com.example.dietpal.recipes.RecipeStore;
import com.example.dietpal.recipes.RecipeUi;
import com.example.dietpal.tracker.FoodLogActivity;
import java.util.List;
import java.util.Locale;

public final class PlanningUi {
  private PlanningUi() {}

  public static String amounts(double[] values) {
    return String.format(
        Locale.getDefault(),
        "%.0f kcal · P %.0f g · C %.0f g · F %.0f g",
        values[0],
        values[1],
        values[2],
        values[3]);
  }

  public static void day(
      Activity a, LinearLayout parent, PlannedDay day, boolean saved, Runnable refresh) {
    LinearLayout summary = RecipeUi.cardBody(a, parent);
    summary.addView(
        RecipeUi.text(
            a,
            java.time.LocalDate.parse(day.day)
                .format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "EEE, d MMM", Locale.getDefault())),
            18,
            true));
    RecipeUi.gap(a, summary, 8);
    summary.addView(RecipeUi.text(a, amounts(day.totals()), 15, true));
    RecipeUi.gap(a, summary, 6);
    summary.addView(RecipeUi.text(a, "Targets: " + targetLabels(day.profile), 12, false));
    summary.addView(
        RecipeUi.text(
            a,
            day.withinTolerance()
                ? "Targets matched"
                : "Targets not fully met — review the differences below",
            12,
            false));
    if (!day.withinTolerance()) {
      double[] totals = day.totals();
      Goal[] goals = day.profile.goals();
      String[] names = {"Energy", "Protein", "Carbs", "Fat"};
      for (int i = 0; i < 4; i++)
        if (goals[i].enabled() && !goals[i].accepts(totals[i]))
          summary.addView(
              RecipeUi.text(
                  a,
                  String.format(
                      Locale.getDefault(),
                      "%s %.1f · target %s",
                      names[i],
                      totals[i],
                      goals[i].label()),
                  12,
                  false));
    }
    if (!day.profile.supplements.isEmpty()) {
      RecipeUi.divider(a, summary);
      summary.addView(RecipeUi.text(a, "Supplements you selected", 15, true));
      for (PlannerProfile.Supplement supplement : day.profile.supplements) {
        RecipeUi.setting(
            a,
            summary,
            com.example.dietpal.R.drawable.health_plus,
            supplement.food.name,
            supplement.servings
                + " label servings · "
                + Math.round(supplement.food.forAmount(supplement.food.kcal, supplement.servings))
                + " kcal included",
            () -> {
              if (saved)
                a.startActivity(
                    new Intent(a, FoodLogActivity.class)
                        .putExtra("day", day.day)
                        .putExtra("food", new com.google.gson.Gson().toJson(supplement.food))
                        .putExtra("amount", supplement.servings));
            });
      }
    }
    for (PlannedDay.Occasion occasion : day.meals) {
      RecipeUi.divider(a, summary);
      summary.addView(RecipeUi.text(a, occasion.name, 16, true));
      for (PlannedDay.Dish dish : occasion.dishes) {
        RecipeUi.gap(a, summary, 8);
        LinearLayout dishRow = RecipeUi.row(a);
        dishRow.setMinimumHeight(RecipeUi.dp(a, 72));
        LinearLayout labels = RecipeUi.column(a);
        labels.setPadding(0, RecipeUi.dp(a, 8), RecipeUi.dp(a, 8), RecipeUi.dp(a, 8));
        labels.addView(RecipeUi.text(a, dish.recipe.title, 15, true));
        RecipeUi.gap(a, labels, 5);
        labels.addView(
            RecipeUi.text(
                a,
                String.format(
                    Locale.getDefault(),
                    "%.2f servings · %.0f kcal",
                    dish.servings,
                    dish.recipe.nutrition.kcal * dish.servings),
                12,
                false));
        labels.setOnClickListener(
            v ->
                a.startActivity(
                    new Intent(a, RecipeDetailsActivity.class)
                        .putExtra("id", dish.recipe.id)
                        .putExtra("servings", dish.servings)
                        .putExtra("day", day.day)));
        labels.setFocusable(true);
        dishRow.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        dishRow.addView(
            RecipeUi.iconButton(
                a,
                com.example.dietpal.R.drawable.health_swap,
                "Swap " + dish.recipe.title,
                () -> swap(a, day, dish, saved, refresh)));
        if (saved)
          dishRow.addView(
              RecipeUi.iconButton(
                  a,
                  com.example.dietpal.R.drawable.health_plus,
                  "Review and log " + dish.recipe.title,
                  () ->
                      a.startActivity(
                          new Intent(a, FoodLogActivity.class)
                              .putExtra("day", day.day)
                              .putExtra(
                                  "food", new com.google.gson.Gson().toJson(dish.recipe.asFood()))
                              .putExtra("amount", dish.servings))));
        summary.addView(dishRow);
      }
    }
  }

  public static String targetLabels(PlannerProfile profile) {
    Goal[] goals = profile.goals();
    String[] names = {"kcal", "P", "C", "F"};
    StringBuilder labels = new StringBuilder();
    for (int i = 0; i < 4; i++)
      if (goals[i].enabled()) {
        if (labels.length() > 0) labels.append(" · ");
        labels.append(names[i]).append(" ").append(goals[i].label());
        if (i > 0) labels.append(" g");
      }
    return labels.toString();
  }

  private static void swap(
      Activity a, PlannedDay day, PlannedDay.Dish dish, boolean saved, Runnable refresh) {
    try (PlanningStore preferences = new PlanningStore(a)) {
      for (String id : preferences.hiddenRecipes())
        if (!day.profile.avoidedRecipes.contains(id)) day.profile.avoidedRecipes.add(id);
    }
    List<PlannedDay.Dish> options =
        MealPlanner.replacements(day, dish, new RecipeStore(a).balanced());
    if (options.isEmpty()) {
      new AlertDialog.Builder(a)
          .setTitle("No suitable alternatives")
          .setMessage(
              "No other recipe of this type satisfies these food exclusions. Change your"
                  + " preferences or keep this meal.")
          .setPositiveButton("Close", null)
          .show();
      return;
    }
    String[] labels = new String[Math.min(8, options.size())];
    for (int i = 0; i < labels.length; i++) {
      PlannedDay.Dish d = options.get(i);
      labels[i] =
          String.format(Locale.getDefault(), "%s · %.2f servings", d.recipe.title, d.servings);
    }
    new AlertDialog.Builder(a)
        .setTitle("Alternatives ranked by daily target fit")
        .setItems(
            labels,
            (dialog, which) -> {
              PlannedDay.Dish replacement = options.get(which);
              dish.recipe = replacement.recipe;
              dish.servings = replacement.servings;
              if (saved)
                try (PlanningStore store = new PlanningStore(a)) {
                  store.save(java.util.Collections.singletonList(day));
                }
              refresh.run();
            })
        .setNegativeButton("Cancel", null)
        .show();
  }
}
