package com.example.dietpal.Models;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dietpal.R;
import com.example.dietpal.RecipeDetailsActivity;
import com.example.dietpal.recipes.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {
  private RecipeRepository repository;
  private RecipeStore store;
  private RecyclerView recycler;
  private TextView status, empty;
  private EditText search;
  private ProgressBar progress;
  private MaterialButton browse, saved, suggested;
  private boolean balancedOnly = true;
  private ChipGroup chips;
  private String category = "All";
  private boolean savedOnly;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Runnable pending;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    store = new RecipeStore(this);
    repository = new RecipeRepository(this);
    if (state != null) {
      category = state.getString("category", "All");
      savedOnly = state.getBoolean("saved");
      balancedOnly = state.getBoolean("balanced", true);
    }
    LinearLayout root = RecipeUi.root(this), header = RecipeUi.header(this, root, "Recipes");
    android.widget.ImageButton list =
        RecipeUi.iconButton(
            this,
            R.drawable.health_cart,
            "Shopping list",
            () -> startActivity(new Intent(this, ShoppingActivity.class)));
    list.setId(R.id.recipe_shopping);
    list.setOnClickListener(v -> startActivity(new Intent(this, ShoppingActivity.class)));
    header.addView(list);
    LinearLayout top = RecipeUi.column(this);
    int p = RecipeUi.dp(this, 16);
    top.setPadding(p, 0, p, 0);
    root.addView(top);

    search = new EditText(this);
    search.setId(R.id.recipe_query);
    search.setSingleLine(true);
    search.setHint("Search recipe names");
    search.setTextSize(16);
    search.setTextColor(RecipeUi.INK);
    search.setHintTextColor(RecipeUi.MUTED);
    search.setBackgroundResource(R.drawable.recipe_input);
    search.setPadding(p, RecipeUi.dp(this, 8), p, RecipeUi.dp(this, 8));
    search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    top.addView(search, new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 48)));
    LinearLayout tabs = RecipeUi.row(this);
    suggested = RecipeUi.button(this, "For you", true);
    suggested.setId(R.id.recipe_suggested);
    tabs.addView(suggested, RecipeUi.weighted(this));
    suggested.setOnClickListener(
        v -> {
          savedOnly = false;
          balancedOnly = true;
          category = "All";
          filters();
          load();
        });
    browse = RecipeUi.button(this, "Discover", !savedOnly);
    saved = RecipeUi.button(this, "Saved", savedOnly);
    browse.setId(R.id.recipe_browse);
    saved.setId(R.id.recipe_saved);
    tabs.addView(browse, RecipeUi.weighted(this));
    tabs.addView(saved, RecipeUi.weighted(this));
    top.addView(tabs);
    browse.setOnClickListener(
        v -> {
          savedOnly = false;
          balancedOnly = false;
          category = "All";
          filters();
          load();
        });
    saved.setOnClickListener(
        v -> {
          savedOnly = true;
          balancedOnly = false;
          category = "All";
          filters();
          load();
        });
    android.widget.HorizontalScrollView scroller = new android.widget.HorizontalScrollView(this);
    scroller.setHorizontalScrollBarEnabled(false);
    chips = new ChipGroup(this);
    chips.setSingleLine(true);
    chips.setSingleSelection(true);
    chips.setSelectionRequired(true);
    scroller.addView(chips);
    top.addView(scroller);
    filters();
    LinearLayout stateRow = RecipeUi.row(this);
    status = RecipeUi.text(this, "", 12, false);
    status.setId(R.id.recipe_status);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    stateRow.addView(status, RecipeUi.weighted(this));
    MaterialButton retry = RecipeUi.button(this, "Refresh", false);
    retry.setId(R.id.recipe_retry);
    retry.setOnClickListener(v -> load());
    stateRow.addView(retry);
    top.addView(stateRow);
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setIndeterminate(true);
    progress.setId(R.id.recipe_progress);
    top.addView(progress, new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 3)));
    empty = RecipeUi.text(this, "", 17, true);
    empty.setId(R.id.recipe_empty);
    empty.setPadding(p, p, p, p);
    root.addView(empty);
    recycler = new RecyclerView(this);
    recycler.setId(R.id.recipe_list);
    recycler.setLayoutManager(new GridLayoutManager(this, RecipeUi.compact(this) ? 1 : 2));
    recycler.setClipToPadding(false);
    recycler.setPadding(p, RecipeUi.dp(this, 10), p, p);
    root.addView(recycler, new LinearLayout.LayoutParams(-1, 0, 1));
    root.addView(
        HealthNavigation.create(
            this,
            2,
            destination -> {
              startActivity(
                  new Intent(this, com.example.dietpal.FlagshipActivity.class)
                      .putExtra("tab", destination)
                      .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
              finish();
            }));
    if (state != null) search.setText(state.getString("query", ""));
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

          public void onTextChanged(CharSequence s, int a, int b, int c) {
            if (pending != null) handler.removeCallbacks(pending);
            repository.cancel();
            pending = () -> load();
            handler.postDelayed(pending, 450);
          }

          public void afterTextChanged(Editable e) {}
        });
    search.setOnEditorActionListener(
        (v, action, event) -> {
          if (action == EditorInfo.IME_ACTION_SEARCH) {
            if (pending != null) handler.removeCallbacks(pending);
            load();
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(search.getWindowToken(), 0);
            search.clearFocus();
            return true;
          }
          return false;
        });
    load();
  }

  private void filters() {
    chips.removeAllViews();
    for (String name :
        balancedOnly
            ? new String[] {
              "All", "Vegetarian", "Vegan", "Protein \u226525 g", "Breakfast", "Mains", "Sides"
            }
            : new String[] {
              "All", "Vegetarian", "Vegan", "Chicken", "Seafood", "Pasta", "Breakfast", "Dessert"
            }) {
      Chip chip = new Chip(this);
      chip.setId(View.generateViewId());
      chip.setText(name);
      chip.setTextSize(13);
      chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(RecipeUi.WHITE));
      chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(RecipeUi.LINE));
      chip.setChipStrokeWidth(RecipeUi.dp(this, 1));
      chip.setCheckable(true);
      chip.setEnsureMinTouchTargetSize(true);
      chip.setChecked(name.equals(category));
      chips.addView(chip);
      chip.setOnClickListener(
          v -> {
            category = name;
            load();
          });
    }
  }

  private void load() {
    if (pending != null) handler.removeCallbacks(pending);
    repository.cancel();
    String query = search.getText().toString().trim();
    MaterialButton[] modes = {suggested, browse, saved};
    boolean[] active = {balancedOnly && !savedOnly, !balancedOnly && !savedOnly, savedOnly};
    for (int i = 0; i < modes.length; i++) {
      modes[i].setBackgroundTintList(
          android.content.res.ColorStateList.valueOf(active[i] ? RecipeUi.BLUE : RecipeUi.SOFT));
      modes[i].setTextColor(active[i] ? RecipeUi.WHITE : RecipeUi.BLUE);
    }
    if (balancedOnly && !savedOnly) {
      progress.setVisibility(View.GONE);
      List<Meal> matches = RecipeRepository.matching(store.balanced(), query, category);
      try (com.example.dietpal.planning.PlanningStore prefs =
          new com.example.dietpal.planning.PlanningStore(this)) {
        java.util.Set<String> hidden = prefs.hiddenRecipes();
        matches.removeIf(meal -> hidden.contains(meal.id));
      }
      String date = getIntent().getStringExtra("day");
      if (date == null) date = java.time.LocalDate.now().toString();
      com.example.dietpal.planning.PlannerProfile profile = null;
      try (com.example.dietpal.planning.PlanningStore plans =
          new com.example.dietpal.planning.PlanningStore(this)) {
        com.example.dietpal.planning.PlannedDay plan = plans.day(date);
        profile = plan == null ? plans.targetsAt(date) : plan.profile;
        if (profile == null) profile = plans.lastProfile();
      }
      if (profile != null
          && profile.effectiveFrom != null
          && java.time.LocalDate.parse(profile.effectiveFrom)
              .isAfter(java.time.LocalDate.parse(date))) profile = null;
      if (profile != null) {
        final com.example.dietpal.planning.PlannerProfile prefs = profile;
        matches.removeIf(meal -> !prefs.allows(meal));
        matches.sort(
            java.util.Comparator.comparingDouble(
                meal -> {
                  double[] total = meal.nutrition.vector();
                  for (int n = 0; n < 4; n++) total[n] *= 4;
                  return prefs.error(total);
                }));
      }
      show(matches, "Balanced suggestions · estimates per base serving");
      return;
    }
    if (savedOnly) {
      progress.setVisibility(View.GONE);
      show(RecipeRepository.matching(store.saved(), query, category), "Saved on this device");
      return;
    }
    List<Meal> initial = repository.offline(query, category);
    show(initial, "Refreshing recipes…");
    progress.setVisibility(View.VISIBLE);
    repository.browse(
        query,
        category,
        new RecipeRepository.Listener() {
          public void loaded(List<Meal> meals, String origin) {
            if (isFinishing() || isDestroyed()) return;
            progress.setVisibility(View.GONE);
            show(meals, origin);
          }

          public void failed(String message) {
            if (isFinishing() || isDestroyed()) return;
            progress.setVisibility(View.GONE);
            status.setText(message);
          }
        });
  }

  private void show(List<Meal> meals, String origin) {
    status.setText(origin + " · " + meals.size() + " recipes");
    empty.setVisibility(meals.isEmpty() ? View.VISIBLE : View.GONE);
    empty.setText(
        savedOnly
            ? "No saved recipes match. Save a recipe or change your filters."
            : origin.startsWith("Offline")
                ? "No offline recipes match. Try another name or reconnect and refresh."
                : "No recipes match. Try a different name or category.");
    recycler.setAdapter(
        new MealAdapter(
            this,
            meals,
            meal -> {
              store.remember(meal);
              startActivity(
                  new Intent(this, RecipeDetailsActivity.class)
                      .putExtra("id", meal.id)
                      .putExtra("day", getIntent().getStringExtra("day")));
            }));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (search != null && (savedOnly || balancedOnly)) load();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putString("category", category);
    out.putBoolean("saved", savedOnly);
    out.putBoolean("balanced", balancedOnly);
    out.putString("query", search.getText().toString());
    super.onSaveInstanceState(out);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (repository != null) repository.cancel();
    super.onDestroy();
  }
}
