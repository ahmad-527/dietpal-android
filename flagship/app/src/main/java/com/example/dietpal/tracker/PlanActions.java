package com.example.dietpal.tracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import androidx.appcompat.app.AlertDialog;
import com.example.dietpal.recipes.Meal;
import com.example.dietpal.recipes.RecipeStore;
import java.time.LocalDate;

public final class PlanActions {
  private PlanActions() {}

  public static void choose(Activity activity, Meal meal) {
    LocalDate now = LocalDate.now();
    new DatePickerDialog(
            activity,
            (picker, y, m, d) -> {
              String day = LocalDate.of(y, m + 1, d).toString();
              com.example.dietpal.planning.PlannedDay existing;
              try (com.example.dietpal.planning.PlanningStore plans =
                  new com.example.dietpal.planning.PlanningStore(activity)) {
                existing = plans.day(day);
              }
              if (existing != null) {
                if (meal.nutrition == null) {
                  new AlertDialog.Builder(activity)
                      .setTitle("Nutrition needed for this plan")
                      .setMessage(
                          "This date has a macro-based plan. This recipe has no calculated"
                              + " nutrition. Choose a balanced recipe or another date.")
                      .setPositiveButton("Close", null)
                      .show();
                  return;
                }
                for (com.example.dietpal.planning.PlannedDay.Occasion occasion : existing.meals)
                  for (com.example.dietpal.planning.PlannedDay.Dish dish : occasion.dishes)
                    if (dish.recipe.id.equals(meal.id)) {
                      android.widget.Toast.makeText(
                              activity,
                              "This recipe is already in that plan. Use its swap control to change"
                                  + " it.",
                              android.widget.Toast.LENGTH_LONG)
                          .show();
                      return;
                    }
                String[] names = new String[existing.meals.size()];
                for (int i = 0; i < names.length; i++) names[i] = existing.meals.get(i).name;
                new AlertDialog.Builder(activity)
                    .setTitle("Add to " + day)
                    .setItems(
                        names,
                        (dialog, which) -> {
                          existing
                              .meals
                              .get(which)
                              .dishes
                              .add(new com.example.dietpal.planning.PlannedDay.Dish(meal, 1));
                          try (com.example.dietpal.planning.PlanningStore plans =
                              new com.example.dietpal.planning.PlanningStore(activity)) {
                            plans.save(java.util.Collections.singletonList(existing));
                          }
                          android.widget.Toast.makeText(
                                  activity,
                                  "Added. Review the updated daily totals in Plan.",
                                  android.widget.Toast.LENGTH_LONG)
                              .show();
                        })
                    .setNegativeButton("Cancel", null)
                    .show();
                return;
              }
              new AlertDialog.Builder(activity)
                  .setTitle("Plan for " + day)
                  .setItems(
                      TrackerStore.SLOTS,
                      (dialog, which) -> {
                        new RecipeStore(activity).plan(day, TrackerStore.SLOTS[which], meal);
                        android.widget.Toast.makeText(
                                activity,
                                "Meal added to your plan",
                                android.widget.Toast.LENGTH_SHORT)
                            .show();
                      })
                  .show();
            },
            now.getYear(),
            now.getMonthValue() - 1,
            now.getDayOfMonth())
        .show();
  }
}
