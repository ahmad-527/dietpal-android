package com.example.dietpal.planning;

import android.app.Activity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.dietpal.recipes.RecipeUi;
import com.example.dietpal.tracker.TrackerStore;

/** Dated target editing shared by tracking and planning. */
public final class GoalDialog {
  private GoalDialog() {}

  public static void show(Activity activity, String date, Runnable refreshed) {
    PlannedDay planned;
    PlannerProfile previous;
    try (PlanningStore plans = new PlanningStore(activity)) {
      planned = plans.day(date);
      previous = planned == null ? plans.targetsAt(date) : planned.profile;
    }
    PlannerProfile profile =
        previous == null
            ? new PlannerProfile()
            : new com.google.gson.Gson()
                .fromJson(new com.google.gson.Gson().toJson(previous), PlannerProfile.class);
    try (TrackerStore tracker = new TrackerStore(activity)) {
      if (previous == null) {
        profile.kcal = tracker.goal("kcal");
        profile.protein = tracker.goal("protein");
        profile.carbs = tracker.goal("carbs");
        profile.fat = tracker.goal("fat");
      }
      if (profile.waterGoal == null) profile.waterGoal = tracker.goal("water");
    }
    profile.effectiveFrom = date;
    Goal[] current = profile.goals();
    android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
    LinearLayout form = RecipeUi.column(activity);
    int p = RecipeUi.dp(activity, 20);
    form.setPadding(p, p, p, p);
    scroll.addView(form);
    form.addView(
        RecipeUi.text(
            activity,
            "Enter a value, range or limit: 180, 100-150, >2000 or <=2000. Blank leaves a target"
                + " unset. Changes start on "
                + date
                + (planned == null ? "." : " for this planned day."),
            13,
            false));
    String[] labels = {
      "Energy (kcal)",
      "Protein (g)",
      "Total carbohydrate (g, includes fibre)",
      "Fat (g)",
      "Water (ml)"
    };
    EditText[] fields = new EditText[5];
    for (int i = 0; i < 5; i++) {
      form.addView(RecipeUi.text(activity, labels[i], 13, false));
      fields[i] = new EditText(activity);
      fields[i].setId(
          new int[] {
                com.example.dietpal.R.id.goal_energy,
                com.example.dietpal.R.id.goal_protein,
                com.example.dietpal.R.id.goal_carbs,
                com.example.dietpal.R.id.goal_fat,
                com.example.dietpal.R.id.goal_water
              }
              [i]);
      fields[i].setSingleLine(true);
      fields[i].setMinHeight(RecipeUi.dp(activity, 48));
      fields[i].setText(i < 4 ? current[i].raw() : Double.toString(profile.waterGoal));
      form.addView(fields[i]);
    }
    TextView error = RecipeUi.text(activity, "", 13, false);
    form.addView(error);
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle("Your targets")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        Goal[] goals = new Goal[4];
                        for (int i = 0; i < 4; i++)
                          goals[i] = Goal.parse(fields[i].getText().toString(), true);
                        String water = fields[4].getText().toString().trim().replace(',', '.');
                        double ml = water.isEmpty() ? 0 : Double.parseDouble(water);
                        if (!Double.isFinite(ml) || ml < 0 || ml > 20000)
                          throw new IllegalArgumentException(
                              "Enter a valid water target in ml, or zero to unset.");
                        profile.setGoals(goals);
                        profile.waterGoal = ml;
                        try (PlanningStore store = new PlanningStore(activity)) {
                          if (planned == null) store.saveTargets(profile);
                          else {
                            profile.validate();
                            planned.profile = profile;
                            store.save(java.util.Collections.singletonList(planned));
                          }
                        }
                        dialog.dismiss();
                        refreshed.run();
                      } catch (IllegalArgumentException invalid) {
                        error.setText(
                            invalid.getMessage() == null
                                ? "Check your target values."
                                : invalid.getMessage());
                      }
                    }));
    dialog.show();
  }
}
