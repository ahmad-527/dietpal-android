package com.example.dietpal;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dietpal.Models.RecipeActivity;
import com.example.dietpal.recipes.*;
import com.example.dietpal.tracker.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.GsonBuilder;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FlagshipActivity extends AppCompatActivity {
  private TrackerStore store;
  private RecipeStore recipes;
  private LocalDate day = LocalDate.now();
  private int tab = 1;
  private LinearLayout root, body;
  private final ExecutorService io = Executors.newSingleThreadExecutor();

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    store = new TrackerStore(this);
    recipes = new RecipeStore(this);
    tab = getIntent().getIntExtra("tab", 1);
    if (state != null) {
      tab = state.getInt("tab", 1);
      day = LocalDate.parse(state.getString("day", day.toString()));
    }
    render();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    tab = intent.getIntExtra("tab", 1);
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (store != null) {
      new ReminderScheduler(this).refresh();
      render();
    }
  }

  private com.example.dietpal.planning.PlannerProfile targetProfile() {
    try (com.example.dietpal.planning.PlanningStore plans =
        new com.example.dietpal.planning.PlanningStore(this)) {
      com.example.dietpal.planning.PlannedDay planned = plans.day(day.toString());
      return planned != null ? planned.profile : plans.targetsAt(day.toString());
    }
  }

  private com.example.dietpal.planning.Goal goalRange(String key) {
    com.example.dietpal.planning.PlannerProfile profile = targetProfile();
    String[] keys = {"kcal", "protein", "carbs", "fat"};
    for (int i = 0; i < keys.length; i++)
      if (keys[i].equals(key))
        return profile == null
            ? com.example.dietpal.planning.Goal.point(store.goal(key))
            : profile.goals()[i];
    return com.example.dietpal.planning.Goal.point(store.goal(key));
  }

  private double goal(String key) {
    if (key.equals("water")) {
      com.example.dietpal.planning.PlannerProfile profile = targetProfile();
      return profile != null && profile.waterGoal != null ? profile.waterGoal : store.goal("water");
    }
    return goalRange(key).aim();
  }

  private String number(double value) {
    return String.format(Locale.getDefault(), "%.0f", value);
  }

  private String decimal(double value) {
    return String.format(Locale.getDefault(), "%.1f", value);
  }

  private void render() {
    root = RecipeUi.root(this);
    LinearLayout header = RecipeUi.row(this);
    int p = RecipeUi.dp(this, 16);
    header.setPadding(p, RecipeUi.dp(this, 8), p, 0);
    LinearLayout labels = RecipeUi.column(this);
    TextView brand = RecipeUi.text(this, "DIETPAL", 11, true);
    brand.setLetterSpacing(.12f);
    brand.setTextColor(RecipeUi.BLUE);
    labels.addView(brand);
    RecipeUi.gap(this, labels, 4);
    labels.addView(
        RecipeUi.text(
            this,
            tab == 1 ? "Today" : tab == 3 ? "Meal plan" : tab == 4 ? "Progress" : "Profile",
            28,
            true));
    header.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    header.addView(
        RecipeUi.iconButton(
            this,
            R.drawable.health_bell,
            "Reminder settings",
            () -> startActivity(new Intent(this, ReminderSettingsActivity.class))));
    root.addView(header);
    body = RecipeUi.scroll(this, root);
    if (tab == 1) today();
    else if (tab == 3) plan();
    else if (tab == 4) progress();
    else profile();
    root.addView(
        HealthNavigation.create(
            this,
            tab,
            destination -> {
              if (destination == 2) {
                startActivity(new Intent(this, RecipeActivity.class));
              } else {
                tab = destination;
                render();
              }
            }));
  }

  private void dates() {
    LinearLayout row = RecipeUi.row(this);
    row.setPadding(0, RecipeUi.dp(this, 2), 0, RecipeUi.dp(this, 6));
    row.addView(
        RecipeUi.iconButton(
            this,
            R.drawable.health_back,
            "Previous day",
            () -> {
              day = day.minusDays(1);
              render();
            }));
    MaterialButton date =
        RecipeUi.button(
            this,
            (day.equals(LocalDate.now()) ? "Today \u00b7 " : "")
                + day.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
            false);
    date.setId(R.id.tracker_date);
    date.setBackgroundTintList(android.content.res.ColorStateList.valueOf(RecipeUi.PAPER));
    date.setTextColor(RecipeUi.INK);
    date.setContentDescription("Choose date: " + day);
    date.setOnClickListener(
        v ->
            new DatePickerDialog(
                    this,
                    (picker, y, m, d) -> {
                      day = LocalDate.of(y, m + 1, d);
                      render();
                    },
                    day.getYear(),
                    day.getMonthValue() - 1,
                    day.getDayOfMonth())
                .show());
    row.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
    row.addView(
        RecipeUi.iconButton(
            this,
            R.drawable.health_next,
            "Next day",
            () -> {
              day = day.plusDays(1);
              render();
            }));
    body.addView(row);
  }

  private void openFood(String slot) {
    Intent intent = new Intent(this, FoodLogActivity.class).putExtra("day", day.toString());
    if (slot != null) intent.putExtra("slot", slot);
    startActivity(intent);
  }

  private void openWorkouts() {
    startActivity(new Intent(this, WorkoutsActivity.class).putExtra("day", day.toString()));
  }

  private void today() {
    dates();
    List<TrackerStore.Entry> entries = store.entries(day.toString());
    TrackerStore.Totals totals = new TrackerStore.Totals(entries);
    double goal = goal("kcal");
    LinearLayout card = RecipeUi.cardBody(this, body);
    LinearLayout title = RecipeUi.row(this);
    title.addView(
        RecipeUi.text(this, "Daily nutrition", 17, true), new LinearLayout.LayoutParams(0, -2, 1));
    title.addView(
        RecipeUi.iconButton(this, R.drawable.health_goal, "Edit daily goals", this::goals));
    card.addView(title);
    LinearLayout energy = RecipeUi.row(this);
    if (RecipeUi.compact(this)) energy.setOrientation(LinearLayout.VERTICAL);
    android.widget.FrameLayout ring = new android.widget.FrameLayout(this);
    ring.addView(
        new NutritionRing(this, goal > 0 ? totals.kcal / goal : 0),
        new android.widget.FrameLayout.LayoutParams(-1, -1));
    LinearLayout centre = RecipeUi.column(this);
    centre.setGravity(android.view.Gravity.CENTER);
    TextView total = RecipeUi.value(this, number(totals.kcal), 32, RecipeUi.INK);
    total.setId(R.id.diary_total);
    total.setGravity(android.view.Gravity.CENTER);
    centre.addView(total);
    RecipeUi.gap(this, centre, 4);
    TextView ringLabel = RecipeUi.text(this, "kcal logged", 12, false);
    ringLabel.setGravity(android.view.Gravity.CENTER);
    centre.addView(ringLabel);
    ring.addView(centre, new android.widget.FrameLayout.LayoutParams(-1, -1));
    energy.addView(
        ring, new LinearLayout.LayoutParams(RecipeUi.dp(this, 144), RecipeUi.dp(this, 144)));
    LinearLayout summary = RecipeUi.column(this);
    summary.setPadding(RecipeUi.dp(this, 16), RecipeUi.dp(this, 8), 0, RecipeUi.dp(this, 8));
    if (goal > 0) {
      com.example.dietpal.planning.Goal energyGoal = goalRange("kcal");
      double difference = Math.abs(goal - totals.kcal);
      String direction = totals.kcal <= goal ? "kcal remaining" : "kcal above goal";
      String remaining = number(difference);
      if (energyGoal.kind.equals("range")) {
        remaining =
            energyGoal.accepts(totals.kcal)
                ? "On target"
                : number(
                    totals.kcal < energyGoal.lower
                        ? energyGoal.lower - totals.kcal
                        : totals.kcal - energyGoal.upper);
        direction =
            energyGoal.accepts(totals.kcal)
                ? "Within calorie range"
                : totals.kcal < energyGoal.lower ? "kcal to range" : "kcal above range";
      } else if (energyGoal.kind.equals("min")) {
        remaining =
            energyGoal.accepts(totals.kcal)
                ? "Reached"
                : number(Math.max(0, energyGoal.lower - totals.kcal));
        direction = energyGoal.accepts(totals.kcal) ? "Calorie minimum" : "kcal to minimum";
      } else if (energyGoal.kind.equals("max")) {
        direction = totals.kcal <= energyGoal.upper ? "kcal within limit" : "kcal above limit";
      }
      summary.addView(RecipeUi.value(this, remaining, 28, RecipeUi.BLUE));
      RecipeUi.gap(this, summary, 4);
      summary.addView(RecipeUi.text(this, direction, 14, false));
      RecipeUi.gap(this, summary, 12);
      summary.addView(
          RecipeUi.text(this, "Daily goal  " + goalRange("kcal").label() + " kcal", 12, false));
    } else {
      summary.addView(RecipeUi.text(this, "Your daily overview", 18, true));
      RecipeUi.gap(this, summary, 8);
      summary.addView(RecipeUi.text(this, "Set a goal to track your balance.", 13, false));
      MaterialButton goals = RecipeUi.button(this, "Set goals", false);
      goals.setOnClickListener(v -> goals());
      summary.addView(goals);
    }
    energy.addView(
        summary,
        new LinearLayout.LayoutParams(
            RecipeUi.compact(this) ? -1 : 0, -2, RecipeUi.compact(this) ? 0 : 1));
    card.addView(energy);
    RecipeUi.gap(this, card, 12);
    LinearLayout macros = RecipeUi.row(this);
    if (RecipeUi.compact(this)) macros.setOrientation(LinearLayout.VERTICAL);
    macro(macros, "Protein", totals.protein, totals.missingProtein, RecipeUi.PROTEIN);
    macro(macros, "Carbs", totals.carbs, totals.missingCarbs, RecipeUi.CARBS);
    macro(macros, "Fat", totals.fat, totals.missingFat, RecipeUi.FAT);
    card.addView(macros);
    if (totals.missingProtein || totals.missingCarbs || totals.missingFat) {
      RecipeUi.gap(this, card, 8);
      card.addView(
          RecipeUi.text(
              this, "\u2265 is a known subtotal; some macro values are missing.", 12, false));
    }
    boolean estimated = false;
    for (TrackerStore.Entry e : entries)
      if (e.food.source != null && e.food.source.contains("AI estimate")) estimated = true;
    if (estimated) {
      RecipeUi.gap(this, card, 8);
      card.addView(RecipeUi.text(this, "Includes photo estimates you reviewed.", 12, false));
    }
    LinearLayout quick = RecipeUi.row(this);
    if (RecipeUi.compact(this)) quick.setOrientation(LinearLayout.VERTICAL);
    quickAction(
        quick, R.drawable.health_plus, "Log food", R.id.diary_add_food, () -> openFood(null));
    quickAction(
        quick,
        R.drawable.health_camera,
        "Meal photo",
        0,
        () ->
            startActivity(
                new Intent(this, com.example.dietpal.vision.PhotoActivity.class)
                    .putExtra("day", day.toString())));
    quickAction(
        quick,
        R.drawable.health_recipes,
        "Recipes",
        0,
        () -> startActivity(new Intent(this, RecipeActivity.class)));
    body.addView(quick);
    RecipeUi.gap(this, body, 12);
    LinearLayout metrics = RecipeUi.row(this);
    metrics.setGravity(android.view.Gravity.TOP);
    if (RecipeUi.compact(this)) metrics.setOrientation(LinearLayout.VERTICAL);
    LinearLayout water = metric(metrics, R.drawable.health_water, "Water", RecipeUi.BLUE);
    int ml = store.water(day.toString());
    double waterGoal = goal("water");
    water.addView(RecipeUi.value(this, ml + " ml", 24, RecipeUi.INK));
    RecipeUi.gap(this, water, 6);
    water.addView(
        RecipeUi.text(
            this, waterGoal > 0 ? "Goal " + number(waterGoal) + " ml" : "No goal set", 12, false));
    RecipeUi.gap(this, water, 6);
    LinearLayout waterActions = RecipeUi.row(this);
    android.widget.ImageButton less =
        RecipeUi.iconButton(
            this,
            R.drawable.health_minus,
            "Remove 250 ml of water",
            () -> {
              store.changeWater(day.toString(), -250);
              render();
            });
    less.setId(R.id.water_remove);
    less.setEnabled(ml > 0);
    waterActions.addView(less);
    MaterialButton more = RecipeUi.button(this, "+ 250 ml", false);
    more.setId(R.id.water_add);
    more.setOnClickListener(
        v -> {
          store.changeWater(day.toString(), 250);
          render();
        });
    waterActions.addView(more, new LinearLayout.LayoutParams(0, -2, 1));
    water.addView(waterActions);
    LinearLayout movement = metric(metrics, R.drawable.health_workout, "Movement", RecipeUi.FAT);
    int seconds = 0, count = 0;
    try (WorkoutStore workouts = new WorkoutStore(this)) {
      for (WorkoutStore.Entry e : workouts.entries(day.toString())) {
        seconds += e.seconds;
        count++;
      }
    }
    movement.addView(
        RecipeUi.value(
            this,
            seconds < 60
                ? seconds + " sec"
                : String.format(Locale.getDefault(), "%.1f min", seconds / 60.0),
            24,
            RecipeUi.INK));
    RecipeUi.gap(this, movement, 6);
    movement.addView(
        RecipeUi.text(this, count + " workout" + (count == 1 ? "" : "s") + " logged", 12, false));
    RecipeUi.gap(this, movement, 6);
    MaterialButton log = RecipeUi.button(this, "View workouts", false);
    log.setOnClickListener(v -> openWorkouts());
    movement.addView(log);
    body.addView(metrics);
    RecipeUi.section(this, body, "Food diary");
    for (String slot : TrackerStore.SLOTS) {
      LinearLayout meal = RecipeUi.cardBody(this, body);
      LinearLayout head = RecipeUi.row(this);
      LinearLayout labels = RecipeUi.column(this);
      labels.addView(RecipeUi.text(this, slot, 17, true));
      double mealKcal = 0;
      int items = 0;
      for (TrackerStore.Entry e : entries)
        if (e.slot.equals(slot)) {
          mealKcal += e.calories();
          items++;
        }
      RecipeUi.gap(this, labels, 4);
      labels.addView(
          RecipeUi.text(
              this,
              items == 0
                  ? "No food logged"
                  : number(mealKcal) + " kcal \u00b7 " + items + " item" + (items == 1 ? "" : "s"),
              12,
              false));
      head.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
      head.addView(
          RecipeUi.iconButton(
              this, R.drawable.health_plus, "Add food to " + slot, () -> openFood(slot)));
      meal.addView(head);
      for (TrackerStore.Entry entry : entries)
        if (entry.slot.equals(slot)) {
          RecipeUi.divider(this, meal);
          RecipeUi.setting(
              this,
              meal,
              R.drawable.health_recipes,
              entry.food.name,
              decimal(entry.amount)
                  + " "
                  + entry.food.unit
                  + " \u00b7 "
                  + number(entry.calories())
                  + " kcal",
              () -> entryActions(entry));
        }
    }
  }

  private void quickAction(LinearLayout parent, int icon, String name, int id, Runnable action) {
    LinearLayout item = RecipeUi.column(this);
    item.setGravity(android.view.Gravity.CENTER);
    item.setPadding(
        RecipeUi.dp(this, 6), RecipeUi.dp(this, 12), RecipeUi.dp(this, 6), RecipeUi.dp(this, 12));
    item.setBackground(RecipeUi.surface(this, RecipeUi.WHITE, 18));
    android.widget.ImageView image = new android.widget.ImageView(this);
    image.setImageResource(icon);
    image.setImageTintList(android.content.res.ColorStateList.valueOf(RecipeUi.BLUE));
    image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    item.addView(
        image, new LinearLayout.LayoutParams(RecipeUi.dp(this, 24), RecipeUi.dp(this, 24)));
    RecipeUi.gap(this, item, 8);
    TextView label = RecipeUi.text(this, name, 13, true);
    label.setGravity(android.view.Gravity.CENTER);
    item.addView(label);
    if (id != 0) item.setId(id);
    item.setFocusable(true);
    item.setContentDescription(name);
    item.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams p =
        new LinearLayout.LayoutParams(
            RecipeUi.compact(this) ? -1 : 0, -2, RecipeUi.compact(this) ? 0 : 1);
    p.setMargins(
        RecipeUi.dp(this, 3), RecipeUi.dp(this, 3), RecipeUi.dp(this, 3), RecipeUi.dp(this, 3));
    parent.addView(item, p);
  }

  private LinearLayout metric(LinearLayout parent, int icon, String title, int color) {
    LinearLayout container = RecipeUi.column(this);
    LinearLayout.LayoutParams p =
        new LinearLayout.LayoutParams(
            RecipeUi.compact(this) ? -1 : 0, -2, RecipeUi.compact(this) ? 0 : 1);
    p.setMargins(0, 0, RecipeUi.dp(this, 6), 0);
    parent.addView(container, p);
    LinearLayout card = RecipeUi.cardBody(this, container);
    LinearLayout heading = RecipeUi.row(this);
    android.widget.ImageView image = new android.widget.ImageView(this);
    image.setImageResource(icon);
    image.setImageTintList(android.content.res.ColorStateList.valueOf(color));
    image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    heading.addView(
        image, new LinearLayout.LayoutParams(RecipeUi.dp(this, 20), RecipeUi.dp(this, 20)));
    TextView label = RecipeUi.text(this, title, 14, true);
    label.setPadding(RecipeUi.dp(this, 6), 0, 0, 0);
    heading.addView(label);
    card.addView(heading);
    RecipeUi.gap(this, card, 14);
    return card;
  }

  private void macro(LinearLayout row, String name, double value, boolean missing, int color) {
    LinearLayout col = RecipeUi.column(this);
    col.setPadding(
        RecipeUi.dp(this, 4), RecipeUi.dp(this, 6), RecipeUi.dp(this, 8), RecipeUi.dp(this, 6));
    col.addView(RecipeUi.text(this, name, 12, false));
    RecipeUi.gap(this, col, 6);
    col.addView(
        RecipeUi.value(this, (missing ? "\u2265 " : "") + decimal(value) + " g", 19, color));
    double goal = goal(name.toLowerCase(Locale.ROOT));
    RecipeUi.gap(this, col, 8);
    if (goal > 0) {
      ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
      progress.setMax(1000);
      progress.setProgress((int) Math.min(1000, value / goal * 1000));
      progress.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
      progress.setProgressBackgroundTintList(
          android.content.res.ColorStateList.valueOf(RecipeUi.LINE));
      progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      col.addView(progress, new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 5)));
      RecipeUi.gap(this, col, 6);
      col.addView(
          RecipeUi.text(
              this, "Goal " + goalRange(name.toLowerCase(Locale.ROOT)).label() + " g", 11, false));
    } else col.addView(RecipeUi.text(this, "No goal", 11, false));
    row.addView(
        col,
        new LinearLayout.LayoutParams(
            RecipeUi.compact(this) ? -1 : 0, -2, RecipeUi.compact(this) ? 0 : 1));
  }

  private void entryActions(TrackerStore.Entry entry) {
    new AlertDialog.Builder(this)
        .setTitle(entry.food.name)
        .setMessage(entry.food.source)
        .setPositiveButton(
            "Change portion",
            (dialog, which) -> {
              EditText input = new EditText(this);
              input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
              input.setText(Double.toString(entry.amount));
              AlertDialog edit =
                  new AlertDialog.Builder(this)
                      .setTitle("Amount eaten (" + entry.food.unit + ")")
                      .setView(input)
                      .setNegativeButton("Cancel", null)
                      .setPositiveButton("Save", null)
                      .create();
              edit.setOnShowListener(
                  ignored ->
                      edit.getButton(AlertDialog.BUTTON_POSITIVE)
                          .setOnClickListener(
                              v -> {
                                try {
                                  store.amount(
                                      entry,
                                      Double.parseDouble(
                                          input.getText().toString().replace(',', '.')));
                                  edit.dismiss();
                                  render();
                                } catch (IllegalArgumentException invalid) {
                                  input.setError("Enter a positive valid quantity.");
                                }
                              }));
              edit.show();
            })
        .setNeutralButton(
            "Remove",
            (dialog, which) -> {
              store.remove(entry.id);
              render();
              Snackbar.make(root, "Entry removed", Snackbar.LENGTH_LONG)
                  .setAction(
                      "Undo",
                      v -> {
                        store.restore(entry);
                        render();
                      })
                  .show();
            })
        .setNegativeButton("Close", null)
        .show();
  }

  private void plan() {
    body.addView(RecipeUi.text(this, "Your next 7 days. Add meals from any recipe.", 14, false));
    dates();
    MaterialButton generate = RecipeUi.button(this, "Build or change a dated plan", true);
    generate.setId(R.id.plan_generate);
    generate.setOnClickListener(
        v ->
            startActivity(
                new Intent(this, com.example.dietpal.planning.PlannerActivity.class)
                    .putExtra("day", day.toString())));
    body.addView(generate);
    Map<String, Meal> planned = recipes.plans();
    for (int i = 0; i < 7; i++) {
      LocalDate date = day.plusDays(i);
      com.example.dietpal.planning.PlannedDay generated;
      try (com.example.dietpal.planning.PlanningStore plans =
          new com.example.dietpal.planning.PlanningStore(this)) {
        generated = plans.day(date.toString());
      }
      if (generated != null) {
        com.example.dietpal.planning.PlanningUi.day(this, body, generated, true, this::render);
        continue;
      }
      LinearLayout card = RecipeUi.cardBody(this, body);
      card.addView(
          RecipeUi.text(
              this,
              date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
              17,
              true));
      boolean any = false;
      for (String slot : TrackerStore.SLOTS) {
        String key = date + ":" + slot;
        Meal meal = planned.get(key);
        if (meal == null) continue;
        any = true;
        card.addView(RecipeUi.text(this, slot, 12, false));
        MaterialButton open = RecipeUi.button(this, meal.title, false);
        open.setOnClickListener(
            v ->
                startActivity(
                    new Intent(this, RecipeDetailsActivity.class).putExtra("id", meal.id)));
        card.addView(open);
        LinearLayout actions = RecipeUi.row(this);
        MaterialButton shop = RecipeUi.button(this, "Ingredients", false);
        shop.setOnClickListener(
            v -> {
              recipes.addIngredients(meal);
              Snackbar.make(root, "Ingredients added", Snackbar.LENGTH_SHORT).show();
            });
        actions.addView(shop, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialButton remove = RecipeUi.button(this, "Remove", false);
        remove.setOnClickListener(
            v -> {
              recipes.removePlan(key);
              render();
            });
        actions.addView(remove, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(actions);
      }
      if (!any) card.addView(RecipeUi.text(this, "No meals planned", 14, false));
    }
    MaterialButton discover = RecipeUi.button(this, "Find a recipe", true);
    discover.setOnClickListener(v -> startActivity(new Intent(this, RecipeActivity.class)));
    body.addView(discover);
    MaterialButton list = RecipeUi.button(this, "Open shopping list", false);
    list.setOnClickListener(v -> startActivity(new Intent(this, ShoppingActivity.class)));
    body.addView(list);
  }

  private void progress() {
    dates();
    LocalDate start = day.minusDays(6);
    double[] values = new double[7];
    double sum = 0;
    for (int i = 0; i < 7; i++) {
      values[i] = new TrackerStore.Totals(store.entries(start.plusDays(i).toString())).kcal;
      sum += values[i];
    }
    LinearLayout chart = RecipeUi.cardBody(this, body);
    chart.addView(RecipeUi.text(this, "Energy \u00b7 last 7 days", 17, true));
    chart.addView(
        new WeeklyChart(this, start, values),
        new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 170)));
    chart.addView(RecipeUi.text(this, number(sum) + " kcal logged across these days", 15, false));
    for (int i = 0; i < 7; i++)
      chart.addView(
          RecipeUi.text(
              this,
              start
                      .plusDays(i)
                      .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
                  + "  \u00b7  "
                  + number(values[i])
                  + " kcal",
              13,
              false));
    chart.addView(
        RecipeUi.text(
            this,
            "These are diary totals; an empty day means no food was logged, not zero food"
                + " consumed.",
            12,
            false));
    LinearLayout weights = RecipeUi.cardBody(this, body);
    weights.addView(RecipeUi.text(this, "Weight", 19, true));
    MaterialButton add = RecipeUi.button(this, "Log weight for " + day, true);
    add.setId(R.id.weight_add);
    add.setOnClickListener(v -> weight());
    weights.addView(add);
    List<Map.Entry<String, Double>> entries = new ArrayList<>(store.weights().entrySet());
    if (entries.isEmpty())
      weights.addView(
          RecipeUi.text(this, "No measurements yet. Your records stay on this device.", 14, false));
    for (int i = entries.size() - 1; i >= Math.max(0, entries.size() - 12); i--) {
      Map.Entry<String, Double> entry = entries.get(i);
      weights.addView(
          RecipeUi.text(
              this, entry.getKey() + "  \u00b7  " + decimal(entry.getValue()) + " kg", 16, false));
    }
    if (entries.size() >= 2) {
      double change = entries.get(entries.size() - 1).getValue() - entries.get(0).getValue();
      weights.addView(
          RecipeUi.text(
              this,
              "Change between first and latest recorded weight: "
                  + (change > 0 ? "+" : "")
                  + decimal(change)
                  + " kg",
              14,
              false));
    }
  }

  private void weight() {
    EditText input = new EditText(this);
    input.setHint("Weight (kg)");
    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Weight on " + day)
            .setView(input)
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
                        store.weight(
                            day.toString(),
                            Double.parseDouble(input.getText().toString().replace(',', '.')));
                        dialog.dismiss();
                        render();
                      } catch (IllegalArgumentException invalid) {
                        input.setError("Enter a positive weight in kilograms.");
                      }
                    }));
    dialog.show();
  }

  private void profile() {
    RecipeUi.section(this, body, "Daily targets");
    LinearLayout targets = RecipeUi.cardBody(this, body);
    RecipeUi.setting(
            this,
            targets,
            R.drawable.health_goal,
            "Nutrition & hydration goals",
            goal("kcal") > 0
                ? number(goal("kcal")) + " kcal \u00b7 " + number(goal("water")) + " ml water"
                : "Set your own daily targets",
            this::goals)
        .setId(R.id.tracker_goal);
    RecipeUi.setting(
        this,
        targets,
        R.drawable.health_bell,
        "Reminders",
        "Meals, water and workouts",
        () -> startActivity(new Intent(this, ReminderSettingsActivity.class)));
    RecipeUi.section(this, body, "Your data");
    LinearLayout data = RecipeUi.cardBody(this, body);
    RecipeUi.setting(
            this,
            data,
            R.drawable.health_export,
            "Export data",
            "Save a JSON copy of your records",
            () -> {
              Intent intent =
                  new Intent(Intent.ACTION_CREATE_DOCUMENT)
                      .setType("application/json")
                      .addCategory(Intent.CATEGORY_OPENABLE)
                      .putExtra(Intent.EXTRA_TITLE, "dietpal-" + LocalDate.now() + ".json");
              try {
                startActivityForResult(intent, 401);
              } catch (android.content.ActivityNotFoundException unavailable) {
                Snackbar.make(root, "No file picker is available", Snackbar.LENGTH_LONG).show();
              }
            })
        .setId(R.id.profile_export);
    RecipeUi.setting(
        this,
        data,
        R.drawable.health_privacy,
        "Privacy & sources",
        "Storage, permissions and data providers",
        this::privacy);
    RecipeUi.setting(
            this,
            data,
            R.drawable.health_delete,
            "Delete local records",
            "Export first to keep a copy",
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Delete local records?")
                    .setMessage(
                        "This removes your diary, goals, weight, workouts, reminders, saved"
                            + " recipes, plans and shopping list. The optional model download stays"
                            + " installed. Export first if you want to keep a copy.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {
                          store.clear();
                          recipes.clearUserData();
                          try (com.example.dietpal.planning.PlanningStore plans =
                              new com.example.dietpal.planning.PlanningStore(this)) {
                            plans.clear();
                          }
                          try (WorkoutStore workouts = new WorkoutStore(this)) {
                            workouts.clear();
                          }
                          new ReminderScheduler(this).clear();
                          java.io.File photoCache = new java.io.File(getCacheDir(), "meal_photos");
                          java.io.File[] photos = photoCache.listFiles();
                          if (photos != null)
                            for (java.io.File photo : photos)
                              if (photo.isFile()
                                  && photo.getName().startsWith("meal-")
                                  && photo.getName().endsWith(".jpg")) photo.delete();
                          render();
                        })
                    .show())
        .setId(R.id.profile_delete);
    LinearLayout about = RecipeUi.cardBody(this, body);
    about.addView(RecipeUi.text(this, "DietPal Flagship", 17, true));
    MaterialButton notices = RecipeUi.button(this, "Open-source notices", false);
    notices.setOnClickListener(v -> startActivity(new Intent(this, NoticesActivity.class)));
    about.addView(notices);
    RecipeUi.gap(this, about, 6);
    about.addView(
        RecipeUi.text(this, "Version 2.0 \u00b7 Your records stay on this device", 13, false));
    RecipeUi.gap(this, about, 8);
    about.addView(
        RecipeUi.text(
            this,
            "Food tracking, meal planning and movement in one place. No account required.",
            13,
            false));
  }

  private void privacy() {
    new AlertDialog.Builder(this)
        .setTitle("Privacy & sources")
        .setMessage(
            "Diary, goals, water, weight, workout records, reminders, recipes, meal plans and lists"
                + " stay in this app on this device. Android backups are disabled. Export only"
                + " writes to a location you choose.\n\n"
                + "Recipe searches and images contact TheMealDB. Food names/barcodes you explicitly"
                + " search go to Open Food Facts. Your diary and goals are not sent. No analytics"
                + " or advertising SDK is included.\n\n"
                + "Meal photos are processed on this device. The optional model is downloaded from"
                + " the official LiteRT Community repository; photos are not uploaded. AI estimates"
                + " need your review.\n\n"
                + "USDA SR Legacy supplies ingredient nutrient data for the balanced collection."
                + " TheMealDB supplies external recipe content and artwork. Open Food Facts"
                + " supplies community product data under ODbL/DbCL. Check package labels. Missing"
                + " nutrition stays unknown.\n\n"
                + "DietPal organises your records; it does not prescribe calorie deficits or"
                + " provide clinical advice.")
        .setPositiveButton("Close", null)
        .show();
  }

  private void goals() {
    com.example.dietpal.planning.GoalDialog.show(this, day.toString(), this::render);
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != 401 || result != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    io.execute(
        () -> {
          try {
            Map<String, Object> export;
            try (TrackerStore exportStore = new TrackerStore(getApplicationContext());
                WorkoutStore workouts = new WorkoutStore(getApplicationContext())) {
              export = exportStore.export();
              export.put("workouts", workouts.entries(null));
            }
            try (com.example.dietpal.planning.PlanningStore plans =
                new com.example.dietpal.planning.PlanningStore(getApplicationContext())) {
              export.put("nutritionBackedPlans", plans.export());
            }
            export.put("reminders", new ReminderScheduler(getApplicationContext()).export());
            export.put("recipesAndPlanning", new RecipeStore(getApplicationContext()).export());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(export);
            try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
              if (output == null) throw new java.io.IOException("No output stream");
              output.write(json.getBytes(StandardCharsets.UTF_8));
            }
            runOnUiThread(
                () ->
                    android.widget.Toast.makeText(
                            this, "Data exported", android.widget.Toast.LENGTH_LONG)
                        .show());
          } catch (Exception failure) {
            runOnUiThread(
                () ->
                    android.widget.Toast.makeText(
                            this,
                            "Could not export. Choose another file location and retry.",
                            android.widget.Toast.LENGTH_LONG)
                        .show());
          }
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putInt("tab", tab);
    state.putString("day", day.toString());
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    io.shutdown();
    if (store != null) store.close();
    super.onDestroy();
  }
}
