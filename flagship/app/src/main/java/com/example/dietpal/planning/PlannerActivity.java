package com.example.dietpal.planning;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.dietpal.R;
import com.example.dietpal.recipes.Meal;
import com.example.dietpal.recipes.RecipeStore;
import com.example.dietpal.recipes.RecipeUi;
import com.example.dietpal.tracker.TrackerStore;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Preview, revise and explicitly save a dated plan; generation never silently changes records. */
public final class PlannerActivity extends AppCompatActivity {
  private PlannerViewModel model;
  private LinearLayout root, body;
  private List<Meal> catalog;
  private EditText[] targets;
  private Spinner diet, meals, horizon;
  private TextView status;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    model = new ViewModelProvider(this).get(PlannerViewModel.class);
    catalog = new RecipeStore(this).balanced();
    if (model.start == null) {
      String day = getIntent().getStringExtra("day");
      try {
        LocalDate.parse(day);
      } catch (RuntimeException invalid) {
        day = LocalDate.now().toString();
      }
      model.start = day;
      try (PlanningStore store = new PlanningStore(this)) {
        PlannedDay existing = store.day(day);
        model.profile = existing != null ? existing.profile : store.targetsAt(day);
        if (model.profile == null) {
          PlannerProfile last = store.lastProfile();
          if (last != null && last.effectiveFrom != null && last.effectiveFrom.compareTo(day) <= 0)
            model.profile = last;
        }
      }
      if (model.profile == null) {
        model.profile = new PlannerProfile();
        try (TrackerStore tracker = new TrackerStore(this)) {
          model.profile.kcal = tracker.goal("kcal");
          model.profile.protein = tracker.goal("protein");
          model.profile.carbs = tracker.goal("carbs");
          model.profile.fat = tracker.goal("fat");
        }
      }
    }
    render();
    model.progress.observe(
        this,
        value -> {
          if (model.busy) {
            if (status != null) status.setText("Building your plan · " + Math.max(0, value) + "%");
          } else if (model.draft != null) review();
          else if (model.error != null) {
            form();
            status.setText(model.error);
          }
        });
  }

  private void render() {
    if (model.draft != null) review();
    else form();
  }

  private void setup(String title) {
    root = RecipeUi.root(this);
    RecipeUi.header(this, root, title);
    body = RecipeUi.scroll(this, root);
  }

  private Spinner choice(LinearLayout parent, String label, String[] values, int selected) {
    parent.addView(RecipeUi.text(this, label, 13, false));
    Spinner spinner = new Spinner(this);
    spinner.setMinimumHeight(RecipeUi.dp(this, 48));
    spinner.setAdapter(
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
    spinner.setSelection(selected);
    parent.addView(spinner);
    return spinner;
  }

  private void form() {
    setup("Build a meal plan");
    body.addView(RecipeUi.text(this, "Your preferences, your schedule", 20, true));
    RecipeUi.gap(this, body, 8);
    body.addView(
        RecipeUi.text(
            this,
            "Choose meals you want to eat. Nutrition is estimated from weighed ingredients; every"
                + " draft shows the fit to your targets.",
            14,
            false));
    RecipeUi.gap(this, body, 12);
    LinearLayout schedule = RecipeUi.cardBody(this, body);
    MaterialButton date = RecipeUi.button(this, "Starting " + model.start, false);
    date.setId(R.id.plan_start);
    date.setOnClickListener(
        v -> {
          if (!captureSafe()) return;
          LocalDate start = LocalDate.parse(model.start);
          new DatePickerDialog(
                  this,
                  (picker, y, m, d) -> {
                    model.start = LocalDate.of(y, m + 1, d).toString();
                    form();
                  },
                  start.getYear(),
                  start.getMonthValue() - 1,
                  start.getDayOfMonth())
              .show();
        });
    schedule.addView(date);
    MaterialButton next = RecipeUi.button(this, "Move start forward one week", false);
    next.setOnClickListener(
        v -> {
          if (!captureSafe()) return;
          model.start = LocalDate.parse(model.start).plusWeeks(1).toString();
          form();
        });
    schedule.addView(next);
    horizon =
        choice(
            schedule,
            "Planning period",
            new String[] {"Week", "Month", "Year"},
            java.util.Arrays.asList("Week", "Month", "Year").indexOf(model.horizon));
    horizon.setId(R.id.plan_horizon);
    diet =
        choice(
            schedule,
            "Diet preference",
            new String[] {"Mixed", "Vegetarian", "Vegan"},
            java.util.Arrays.asList("mixed", "vegetarian", "vegan").indexOf(model.profile.diet));
    diet.setId(R.id.plan_diet);
    meals =
        choice(
            schedule,
            "Meals per day",
            new String[] {"1", "2", "3", "4", "5", "6"},
            model.profile.mealsPerDay - 1);
    meals.setId(R.id.plan_meal_count);
    schedule.addView(
        RecipeUi.text(
            this,
            "Meal count means eating occasions. One occasion can contain a main dish and sides."
                + " Choose the pattern that suits your routine.",
            12,
            false));
    RecipeUi.section(this, body, "Daily targets");
    LinearLayout goals = RecipeUi.cardBody(this, body);
    targets = new EditText[4];
    String[] labels = {
      "Energy (kcal)", "Protein (g)", "Total carbohydrate (g, includes fibre)", "Fat (g)"
    };
    int[] ids = {R.id.plan_kcal, R.id.plan_protein, R.id.plan_carbs, R.id.plan_fat};
    Goal[] values = model.profile.goals();
    for (int i = 0; i < 4; i++) {
      goals.addView(RecipeUi.text(this, labels[i], 13, false));
      targets[i] = new EditText(this);
      targets[i].setId(ids[i]);
      targets[i].setSingleLine(true);
      targets[i].setMinHeight(RecipeUi.dp(this, 48));
      targets[i].setInputType(InputType.TYPE_CLASS_TEXT);
      targets[i].setHint(i == 0 ? "Optional: 2000, >2000 or <=2000" : "180 or 100-150");
      targets[i].setText(values[i].raw());
      goals.addView(targets[i]);
    }
    goals.addView(
        RecipeUi.text(
            this,
            "Use a number, range or limit. Examples: 180, 100-150, >2000 or <=2000. Blank leaves"
                + " that target unset.",
            12,
            false));
    RecipeUi.section(this, body, "Food choices");
    LinearLayout preferences = RecipeUi.cardBody(this, body);
    MaterialButton allergens = RecipeUi.button(this, "Ingredient and allergen exclusions", false);
    allergens.setId(R.id.plan_exclusions);
    allergens.setOnClickListener(
        v -> {
          if (!captureSafe()) return;
          exclusions();
        });
    preferences.addView(allergens);
    preferences.addView(
        RecipeUi.text(
            this,
            model.profile.excludedFoods.size()
                + " ingredients and "
                + model.profile.excludedAllergens.size()
                + " allergen categories excluded. Check product labels and preparation for"
                + " cross-contact.",
            12,
            false));
    preferences.addView(
        RecipeUi.text(
            this,
            "Saved balanced recipes receive a small preference bonus. Recipes you hide are"
                + " excluded. Different combinations and serving sizes provide variation; no recipe"
                + " repeats within one day.",
            12,
            false));
    RecipeUi.section(this, body, "Supplements you choose");
    LinearLayout supplements = RecipeUi.cardBody(this, body);
    supplements.addView(
        RecipeUi.text(
            this,
            "Optional. Include your own label values and the daily amount you already intend to"
                + " use. They count toward calories and macros.",
            13,
            false));
    for (PlannerProfile.Supplement supplement : new ArrayList<>(model.profile.supplements)) {
      RecipeUi.setting(
          this,
          supplements,
          R.drawable.health_recipes,
          supplement.food.name,
          supplement.servings + " label servings daily",
          () ->
              new AlertDialog.Builder(this)
                  .setTitle("Remove from this draft?")
                  .setPositiveButton(
                      "Remove",
                      (d, w) -> {
                        model.profile.supplements.remove(supplement);
                        form();
                      })
                  .setNegativeButton("Cancel", null)
                  .show());
    }
    MaterialButton addSupplement = RecipeUi.button(this, "Add a supplement label", false);
    addSupplement.setId(R.id.plan_supplement);
    addSupplement.setOnClickListener(
        v -> {
          if (captureSafe()) supplement();
        });
    supplements.addView(addSupplement);
    status = RecipeUi.text(this, model.busy ? "Building your plan…" : "", 14, false);
    status.setId(R.id.plan_status);
    status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
    body.addView(status);
    MaterialButton generate = RecipeUi.button(this, "Generate draft", true);
    generate.setId(R.id.plan_generate);
    generate.setEnabled(!model.busy);
    generate.setOnClickListener(
        v -> {
          try {
            capture();
            model.profile.validate();
            try (PlanningStore settingsStore = new PlanningStore(this)) {
              model.profile.avoidedRecipes = new ArrayList<>(settingsStore.hiddenRecipes());
            }
            model.profile.favouriteRecipes.clear();
            for (Meal meal : new RecipeStore(this).saved())
              if (meal.nutrition != null) model.profile.favouriteRecipes.add(meal.id);
            model.generate(catalog);
            generate.setEnabled(false);
          } catch (IllegalArgumentException invalid) {
            status.setText(
                invalid instanceof NumberFormatException
                    ? "Enter all four targets as valid numbers."
                    : invalid.getMessage());
          }
        });
    body.addView(generate);
  }

  private EditText supplementField(LinearLayout parent, String label, int id, boolean numeric) {
    parent.addView(RecipeUi.text(this, label, 13, false));
    EditText field = new EditText(this);
    field.setId(id);
    field.setSingleLine(true);
    field.setMinHeight(RecipeUi.dp(this, 48));
    field.setHint(label);
    if (numeric)
      field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    parent.addView(field);
    return field;
  }

  private void supplement() {
    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    LinearLayout form = RecipeUi.column(this);
    int padding = RecipeUi.dp(this, 20);
    form.setPadding(padding, padding, padding, padding);
    scroll.addView(form);
    form.addView(
        RecipeUi.text(
            this,
            "Enter values for ONE serving printed on your product label. No assumed whey values or"
                + " recommended dose is supplied.",
            13,
            false));
    EditText
        name =
            supplementField(
                form,
                "Product and label serving, e.g. whey / 30 g scoop",
                R.id.supplement_name,
                false),
        amount =
            supplementField(
                form, "Label servings you choose per day", R.id.supplement_amount, true);
    amount.setText("1");
    Spinner source =
        choice(
            form,
            "Source stated on the label",
            new String[] {
              "Whey/casein (milk)",
              "Soy protein",
              "Other plant protein",
              "Other — check ingredients"
            },
            0);
    EditText
        kcal = supplementField(form, "Energy per label serving (kcal)", R.id.supplement_kcal, true),
        protein =
            supplementField(form, "Protein per label serving (g)", R.id.supplement_protein, true),
        carbs =
            supplementField(
                form,
                "Total carbs per label serving (g, includes fibre)",
                R.id.supplement_carbs,
                true),
        fat = supplementField(form, "Fat per label serving (g)", R.id.supplement_fat, true);
    form.addView(RecipeUi.text(this, "Other allergens printed on the label", 13, false));
    String[] allergens = {"milk", "egg", "fish", "soy", "peanut", "tree nuts", "wheat", "gluten"};
    java.util.List<String> extra = new ArrayList<>();
    for (String allergen : allergens) {
      com.google.android.material.checkbox.MaterialCheckBox check =
          new com.google.android.material.checkbox.MaterialCheckBox(this);
      check.setText(allergen);
      check.setOnCheckedChangeListener((button, checked) -> toggle(extra, allergen, checked));
      form.addView(check);
    }
    Spinner carbohydrateBasis =
        choice(
            form,
            "Carbohydrate label basis",
            new String[] {
              "Total carbohydrate (includes fibre)", "Available/net carbohydrate + fibre"
            },
            0);
    EditText fibre =
        supplementField(
            form,
            "Fibre per label serving (g); needed for available carbs",
            R.id.supplement_fiber,
            true);
    TextView error = RecipeUi.text(this, "", 13, false);
    form.addView(error);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Supplement label")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Include in draft", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        PlannerProfile.Supplement supplement = new PlannerProfile.Supplement();
                        com.example.dietpal.tracker.Food food =
                            new com.example.dietpal.tracker.Food();
                        food.name = name.getText().toString().trim();
                        food.unit = "portion";
                        food.source = "User-entered supplement label";
                        food.kcal = Double.parseDouble(kcal.getText().toString().replace(',', '.'));
                        food.protein =
                            Double.parseDouble(protein.getText().toString().replace(',', '.'));
                        food.carbs =
                            Double.parseDouble(carbs.getText().toString().replace(',', '.'));
                        food.fat = Double.parseDouble(fat.getText().toString().replace(',', '.'));
                        String fiberText = fibre.getText().toString().trim().replace(',', '.');
                        food.fiber = fiberText.isEmpty() ? null : Double.parseDouble(fiberText);
                        if (carbohydrateBasis.getSelectedItemPosition() == 1) {
                          if (food.fiber == null)
                            throw new IllegalArgumentException(
                                "Fibre is needed to convert available carbs to total carbs.");
                          food.availableCarbs = food.carbs;
                          food.carbs += food.fiber;
                        }
                        supplement.servings =
                            Double.parseDouble(amount.getText().toString().replace(',', '.'));
                        food.validate(supplement.servings);
                        supplement.food = food;
                        supplement.diet =
                            source.getSelectedItemPosition() == 0
                                ? "vegetarian"
                                : source.getSelectedItemPosition() < 3 ? "vegan" : "mixed";
                        supplement.allergens.addAll(extra);
                        if (source.getSelectedItemPosition() == 0
                            && !supplement.allergens.contains("milk"))
                          supplement.allergens.add("milk");
                        if (source.getSelectedItemPosition() == 1
                            && !supplement.allergens.contains("soy"))
                          supplement.allergens.add("soy");
                        model.profile.supplements.add(supplement);
                        dialog.dismiss();
                        form();
                      } catch (IllegalArgumentException invalid) {
                        error.setText(
                            "Enter the name, daily amount and all four non-negative label values."
                                + " Use zero only when the label says zero.");
                      }
                    }));
    dialog.show();
  }

  private boolean captureSafe() {
    try {
      capture();
      return true;
    } catch (IllegalArgumentException invalid) {
      status.setText("Enter valid target numbers, or leave a field blank while editing.");
      return false;
    }
  }

  private void capture() {
    if (targets == null) return;
    Goal[] values = new Goal[4];
    for (int i = 0; i < 4; i++) values[i] = Goal.parse(targets[i].getText().toString(), true);
    model.profile.setGoals(values);
    model.profile.diet =
        new String[] {"mixed", "vegetarian", "vegan"}[diet.getSelectedItemPosition()];
    model.profile.mealsPerDay = meals.getSelectedItemPosition() + 1;
    model.horizon = (String) horizon.getSelectedItem();
    model.profile.effectiveFrom = model.start;
  }

  private void exclusions() {
    String[] allergens = {"milk", "egg", "fish", "soy", "peanut", "tree nuts", "wheat", "gluten"};
    new AlertDialog.Builder(this)
        .setTitle("Exclude allergen categories")
        .setMultiChoiceItems(
            allergens,
            checked(allergens, model.profile.excludedAllergens),
            (dialog, which, checked) ->
                toggle(model.profile.excludedAllergens, allergens[which], checked))
        .setNeutralButton("Choose ingredients", (d, w) -> ingredientExclusions())
        .setPositiveButton("Done", (d, w) -> form())
        .show();
  }

  private boolean[] checked(String[] values, List<String> selected) {
    boolean[] checks = new boolean[values.length];
    for (int i = 0; i < values.length; i++) checks[i] = selected.contains(values[i]);
    return checks;
  }

  private void toggle(List<String> values, String key, boolean checked) {
    values.remove(key);
    if (checked) values.add(key);
  }

  private void ingredientExclusions() {
    try (InputStreamReader reader =
        new InputStreamReader(
            getAssets().open("nutrition_ingredients.json"), StandardCharsets.UTF_8)) {
      JsonObject ingredients =
          JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("ingredients");
      String[] keys = ingredients.keySet().toArray(new String[0]), labels = new String[keys.length];
      for (int i = 0; i < keys.length; i++)
        labels[i] = ingredients.getAsJsonObject(keys[i]).get("name").getAsString();
      new AlertDialog.Builder(this)
          .setTitle("Foods to leave out")
          .setMultiChoiceItems(
              labels,
              checked(keys, model.profile.excludedFoods),
              (dialog, which, checked) -> toggle(model.profile.excludedFoods, keys[which], checked))
          .setPositiveButton("Done", (d, w) -> form())
          .show();
    } catch (java.io.IOException invalid) {
      status.setText("Ingredient list could not be loaded.");
    }
  }

  private void proteinOptions() {
    List<Meal> options = new ArrayList<>();
    for (Meal meal : catalog) if (model.profile.allows(meal)) options.add(meal);
    options.sort(
        java.util.Comparator.comparingDouble(
            meal -> -meal.nutrition.protein / Math.max(1, meal.nutrition.kcal)));
    String[] labels = new String[Math.min(6, options.size())];
    for (int i = 0; i < labels.length; i++) {
      Meal meal = options.get(i);
      labels[i] =
          String.format(
              java.util.Locale.getDefault(),
              "%s · %.0f g protein / %.0f kcal",
              meal.title,
              meal.nutrition.protein,
              meal.nutrition.kcal);
    }
    new AlertDialog.Builder(this)
        .setTitle("Protein-rich recipe options")
        .setItems(
            labels,
            (dialog, which) ->
                startActivity(
                    new android.content.Intent(
                            this, com.example.dietpal.RecipeDetailsActivity.class)
                        .putExtra("id", options.get(which).id)))
        .setNeutralButton(
            "Add a label I use",
            (dialog, which) -> {
              model.draft = null;
              form();
              supplement();
            })
        .setNegativeButton("Close", null)
        .show();
  }

  private void review() {
    setup(model.saved ? "Your meal plan" : "Review meal plan");
    targets = null;
    int matched = 0;
    java.util.Set<String> unique = new java.util.HashSet<>();
    for (PlannedDay day : model.draft) {
      if (day.withinTolerance()) matched++;
      for (PlannedDay.Occasion meal : day.meals)
        for (PlannedDay.Dish dish : meal.dishes) unique.add(dish.recipe.id);
    }
    java.time.format.DateTimeFormatter label =
        java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.getDefault());
    body.addView(
        RecipeUi.text(
            this,
            LocalDate.parse(model.draft.get(0).day).format(label)
                + " — "
                + LocalDate.parse(model.draft.get(model.draft.size() - 1).day).format(label),
            18,
            true));
    RecipeUi.gap(this, body, 8);
    body.addView(
        RecipeUi.text(
            this,
            model.profile.diet
                + " · "
                + model.profile.mealsPerDay
                + " meal"
                + (model.profile.mealsPerDay == 1 ? "" : "s")
                + " per day",
            13,
            false));
    LinearLayout overview = RecipeUi.cardBody(this, body), metrics = RecipeUi.row(this);
    String[]
        numbers = {"" + model.draft.size(), "" + unique.size(), matched + "/" + model.draft.size()},
        labels = {"Days", "Recipes", "Target fit"};
    for (int i = 0; i < 3; i++) {
      LinearLayout metric = RecipeUi.column(this);
      metric.addView(RecipeUi.value(this, numbers[i], 24, RecipeUi.BLUE));
      RecipeUi.gap(this, metric, 6);
      metric.addView(RecipeUi.text(this, labels[i], 12, false));
      metrics.addView(metric, RecipeUi.weighted(this));
    }
    overview.addView(metrics);
    TextView summary =
        RecipeUi.text(
            this,
            model.draft.size()
                + " days · "
                + matched
                + " days meeting target ranges / point tolerance",
            12,
            false);
    summary.setId(R.id.plan_summary);
    RecipeUi.gap(this, overview, 12);
    overview.addView(summary);
    MaterialButton info = RecipeUi.button(this, "About nutrition estimates", false);
    info.setOnClickListener(
        v ->
            new AlertDialog.Builder(this)
                .setTitle("Targets and estimates")
                .setMessage(
                    "Ranges and calorie limits are strict. Exact point targets use a shown 10%"
                        + " comparison tolerance. This is a planning comparison, not a clinical"
                        + " threshold.\n\n"
                        + "Values are estimated from weighed USDA ingredients. A macro-matched plan"
                        + " does not establish micronutrient adequacy or a complete diet. You can"
                        + " change any week as your needs and routine change.")
                .setPositiveButton("Close", null)
                .show());
    overview.addView(info);
    boolean proteinGap = false;
    Goal proteinTarget = model.profile.goals()[1];
    double proteinMinimum =
        proteinTarget.kind.equals("point")
            ? proteinTarget.lower * .9
            : proteinTarget.kind.equals("max") || proteinTarget.kind.equals("off")
                ? 0
                : proteinTarget.lower;
    for (PlannedDay planned : model.draft)
      if (planned.totals()[1] < proteinMinimum) proteinGap = true;
    if (proteinGap) {
      MaterialButton options = RecipeUi.button(this, "Review protein options", false);
      options.setOnClickListener(v -> proteinOptions());
      body.addView(options);
    }
    if (!model.saved) {
      MaterialButton save = RecipeUi.button(this, "Save these dates", true);
      save.setId(R.id.plan_save);
      save.setOnClickListener(
          v ->
              new AlertDialog.Builder(this)
                  .setTitle("Save this dated plan?")
                  .setMessage(
                      "Plans for "
                          + model.draft.get(0).day
                          + " through "
                          + model.draft.get(model.draft.size() - 1).day
                          + " will be replaced. All other dates and diary records stay as they"
                          + " are.")
                  .setNegativeButton("Cancel", null)
                  .setPositiveButton(
                      "Save",
                      (d, w) -> {
                        save.setEnabled(false);
                        model.worker.execute(
                            () -> {
                              try (PlanningStore store =
                                  new PlanningStore(getApplicationContext())) {
                                store.save(model.draft);
                                model.saved = true;
                                runOnUiThread(
                                    () -> {
                                      if (!isDestroyed()) review();
                                    });
                              } catch (Exception failure) {
                                runOnUiThread(
                                    () -> {
                                      if (!isDestroyed()) {
                                        save.setEnabled(true);
                                        android.widget.Toast.makeText(
                                                this,
                                                "Could not save. Your previous records remain"
                                                    + " available.",
                                                android.widget.Toast.LENGTH_LONG)
                                            .show();
                                      }
                                    });
                              }
                            });
                      })
                  .show());
      body.addView(save);
    }
    MaterialButton settings = RecipeUi.button(this, "Change preferences or dates", false);
    settings.setOnClickListener(
        v -> {
          model.draft = null;
          model.error = null;
          form();
        });
    body.addView(settings);
    LinearLayout pages = RecipeUi.row(this);
    MaterialButton previous = RecipeUi.button(this, "Previous week", false);
    previous.setEnabled(model.page > 0);
    previous.setOnClickListener(
        v -> {
          model.page -= 7;
          review();
        });
    pages.addView(previous, RecipeUi.weighted(this));
    MaterialButton next = RecipeUi.button(this, "Next week", false);
    next.setEnabled(model.page + 7 < model.draft.size());
    next.setOnClickListener(
        v -> {
          model.page += 7;
          review();
        });
    pages.addView(next, RecipeUi.weighted(this));
    body.addView(pages);
    for (int i = model.page; i < Math.min(model.page + 7, model.draft.size()); i++)
      PlanningUi.day(this, body, model.draft.get(i), model.saved, this::review);
  }
}
