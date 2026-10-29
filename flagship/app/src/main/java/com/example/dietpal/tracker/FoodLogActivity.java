package com.example.dietpal.tracker;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dietpal.R;
import com.example.dietpal.recipes.RecipeUi;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class FoodLogActivity extends AppCompatActivity {
  private TrackerStore store;
  private FoodApi api;
  private LinearLayout results;
  private EditText query;
  private TextView status;
  private MaterialButton find;
  private String day;
  private AlertDialog foodDialog;
  private Food editingSource;
  private Bundle restoredDraft;
  private Spinner editingSlot, editingBasis, editingCarbsBasis;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    day = getIntent().getStringExtra("day");
    try {
      LocalDate.parse(day);
    } catch (RuntimeException invalid) {
      day = LocalDate.now().toString();
    }
    store = new TrackerStore(this);
    api = new FoodApi();
    LinearLayout root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Log food");
    LinearLayout body = RecipeUi.scroll(this, root);

    body.addView(
        RecipeUi.text(
            this,
            "Logging for "
                + day
                + ". Search packaged foods, type a barcode, or enter your own values.",
            15,
            false));
    RecipeUi.gap(this, body, 16);
    query = input(body, "Food name or barcode", R.id.food_query, false);
    query.setSingleLine(true);
    query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    find = RecipeUi.button(this, "Search foods", true);
    find.setId(R.id.food_search);
    find.setOnClickListener(v -> search());
    body.addView(find);
    query.setOnEditorActionListener(
        (v, action, event) -> {
          if (action == EditorInfo.IME_ACTION_SEARCH) {
            search();
            return true;
          }
          return false;
        });
    MaterialButton manual = RecipeUi.button(this, "Enter food manually", false);
    manual.setId(R.id.food_manual);
    manual.setOnClickListener(v -> edit(new Food()));
    body.addView(manual);
    status = RecipeUi.text(this, "Recent foods", 15, true);
    status.setId(R.id.food_results);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    body.addView(status);
    results = RecipeUi.column(this);
    body.addView(results);
    show(store.recent());
    RecipeUi.gap(this, body, 12);
    body.addView(
        RecipeUi.text(
            this,
            "Product nutrition: Open Food Facts (ODbL). Check the package label; community data can"
                + " be incomplete. Manual entry always works offline.",
            12,
            false));
    restoredDraft = state == null ? null : state.getBundle("foodDraft");
    String prefill =
        restoredDraft == null
            ? getIntent().getStringExtra("food")
            : restoredDraft.getString("source");
    if (prefill != null && (state == null || restoredDraft != null))
      try {
        Food food = new com.google.gson.Gson().fromJson(prefill, Food.class);
        if (food != null) edit(food, getIntent().getDoubleExtra("amount", 100));
      } catch (RuntimeException invalid) {
        status.setText("The suggested food could not be read. You can add it manually.");
      }
  }

  private void search() {
    ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(query.getWindowToken(), 0);
    status.setText("Looking up foods…");
    find.setEnabled(false);
    api.search(
        query.getText().toString(),
        new FoodApi.Listener() {
          public void loaded(List<Food> foods) {
            if (isDestroyed() || isFinishing()) return;
            find.setEnabled(true);
            status.setText(
                foods.isEmpty()
                    ? "No products found. Try another name or enter the label manually."
                    : foods.size() + " products · Open Food Facts");
            show(foods);
          }

          public void failed(String message) {
            if (isDestroyed() || isFinishing()) return;
            find.setEnabled(true);
            status.setText(message);
            show(store.recent());
          }
        });
  }

  private void show(List<Food> foods) {
    results.removeAllViews();
    for (Food food : foods) {
      LinearLayout card = RecipeUi.cardBody(this, results);
      card.addView(RecipeUi.text(this, food.name, 17, true));
      card.addView(
          RecipeUi.text(
              this,
              (food.kcal == null ? "Calories not supplied" : Math.round(food.kcal) + " kcal")
                  + (food.unit.equals("portion") ? " per portion" : " per 100 " + food.unit),
              14,
              false));
      MaterialButton add = RecipeUi.button(this, "Choose portion", false);
      add.setOnClickListener(v -> edit(food));
      card.addView(add);
    }
  }

  private EditText input(LinearLayout parent, String label, int id, boolean numeric) {
    parent.addView(RecipeUi.text(this, label, 13, false));
    EditText input = new EditText(this);
    input.setId(id);
    input.setTextSize(17);
    input.setTextColor(RecipeUi.INK);
    input.setHintTextColor(RecipeUi.MUTED);
    input.setHint(label);
    input.setSingleLine(true);
    input.setMinHeight(RecipeUi.dp(this, 50));
    input.setBackgroundResource(R.drawable.recipe_input);
    int p = RecipeUi.dp(this, 12);
    input.setPadding(p, p, p, p);
    if (numeric)
      input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    parent.addView(input, new LinearLayout.LayoutParams(-1, -2));
    RecipeUi.gap(this, parent, 8);
    return input;
  }

  private Spinner spinner(LinearLayout parent, String label, String[] choices) {
    parent.addView(RecipeUi.text(this, label, 13, false));
    Spinner spinner = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, choices);
    spinner.setAdapter(adapter);
    spinner.setMinimumHeight(RecipeUi.dp(this, 48));
    parent.addView(spinner);
    return spinner;
  }

  private String value(Double v) {
    return v == null ? "" : Double.toString(v);
  }

  private Double number(EditText input) {
    String text = input.getText().toString().trim().replace(',', '.');
    return text.isEmpty() ? null : Double.parseDouble(text);
  }

  private void edit(Food source) {
    edit(source, source.unit.equals("portion") ? 1 : 100);
  }

  private void edit(Food source, double initialAmount) {
    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    LinearLayout form = RecipeUi.column(this);
    int p = RecipeUi.dp(this, 20);
    form.setPadding(p, p, p, p);
    scroll.addView(form);
    EditText name = input(form, "Food name", R.id.food_name, false);
    name.setText(source.name);
    Spinner slot = spinner(form, "Meal", TrackerStore.SLOTS);
    String selectedSlot = getIntent().getStringExtra("slot");
    for (int i = 0; i < TrackerStore.SLOTS.length; i++)
      if (TrackerStore.SLOTS[i].equals(selectedSlot)) slot.setSelection(i);
    if (restoredDraft != null) slot.setSelection(restoredDraft.getInt("slot"));
    Spinner basis =
        spinner(
            form,
            "Nutrition values below are for",
            new String[] {"100 g", "100 ml", "One portion"});
    final int initialBasis =
        restoredDraft != null
            ? restoredDraft.getInt("basis")
            : source.unit.equals("g") ? 0 : source.unit.equals("ml") ? 1 : 2;
    basis.setSelection(initialBasis);
    EditText amount =
        input(
            form,
            source.unit.equals("portion") ? "Portions eaten" : "Amount eaten (" + source.unit + ")",
            R.id.food_amount,
            true);
    amount.setText(Double.toString(initialAmount));
    TextView amountLabel = (TextView) form.getChildAt(form.indexOfChild(amount) - 1);
    String initialLabel =
        initialBasis == 2
            ? "Portions eaten"
            : initialBasis == 0 ? "Amount eaten (g)" : "Amount eaten (ml)";
    amountLabel.setText(initialLabel);
    amount.setHint(initialLabel);
    EditText kcal = input(form, "Energy (kcal)", R.id.food_kcal, true);
    kcal.setText(value(source.kcal));
    EditText protein = input(form, "Protein (g), optional", R.id.food_protein, true);
    protein.setText(value(source.protein));
    Spinner carbohydrateBasis =
        spinner(
            form,
            "Carbohydrate label basis",
            new String[] {
              "Total carbohydrate (includes fibre)", "Available/net carbohydrate + fibre"
            });
    boolean available = source.carbs == null && source.availableCarbs != null;
    final int initialCarbsBasis =
        restoredDraft != null ? restoredDraft.getInt("carbsBasis") : available ? 1 : 0;
    carbohydrateBasis.setSelection(initialCarbsBasis);
    EditText carbs = input(form, "Label carbohydrate (g), optional", R.id.food_carbs, true);
    carbs.setText(value(available ? source.availableCarbs : source.carbs));
    EditText fiber =
        input(form, "Fibre (g); needed to convert available carbs", R.id.food_fiber, true);
    fiber.setText(value(source.fiber));
    carbohydrateBasis.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          private int previous = initialCarbsBasis;

          public void onNothingSelected(android.widget.AdapterView<?> parent) {}

          public void onItemSelected(
              android.widget.AdapterView<?> parent, View v, int position, long id) {
            if (previous != position) {
              boolean sameBasis =
                  basis.getSelectedItemPosition()
                      == (source.unit.equals("g") ? 0 : source.unit.equals("ml") ? 1 : 2);
              carbs.setText(
                  sameBasis ? value(position == 1 ? source.availableCarbs : source.carbs) : "");
              fiber.setText(sameBasis ? value(source.fiber) : "");
              previous = position;
            }
          }
        });
    EditText fat = input(form, "Fat (g), optional", R.id.food_fat, true);
    fat.setText(value(source.fat));
    if (restoredDraft != null) {
      for (EditText field : new EditText[] {name, amount, kcal, protein, carbs, fiber, fat})
        field.setText(restoredDraft.getString(Integer.toString(field.getId()), ""));
    }
    form.addView(
        RecipeUi.text(
            this,
            "Check the label or verified recipe values. Blank macros remain unknown. Portion totals"
                + " are calculated from the quantity you enter.",
            13,
            false));
    TextView error = RecipeUi.text(this, "", 14, false);
    error.setTextColor(0xFF9B3023);
    error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    form.addView(error);
    basis.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          private int previous = initialBasis;

          public void onNothingSelected(android.widget.AdapterView<?> parent) {}

          public void onItemSelected(
              android.widget.AdapterView<?> parent, View v, int position, long id) {
            if (previous != position) {
              for (EditText field : new EditText[] {kcal, protein, carbs, fat, fiber})
                field.setText("");
              amount.setText(position == 2 ? "1" : "100");
              String label =
                  position == 2
                      ? "Portions eaten"
                      : position == 0 ? "Amount eaten (g)" : "Amount eaten (ml)";
              amount.setHint(label);
              amountLabel.setText(label);
              previous = position;
            }
          }
        });
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Add to diary")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add food", null)
            .create();
    foodDialog = dialog;
    editingSource = source;
    editingSlot = slot;
    editingBasis = basis;
    editingCarbsBasis = carbohydrateBasis;
    restoredDraft = null;
    dialog.setOnShowListener(
        ignored -> {
          android.widget.Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
          save.setId(R.id.food_save);
          save.setOnClickListener(
              v -> {
                try {
                  Food food = new Food();
                  food.name = name.getText().toString().trim();
                  food.unit = new String[] {"g", "ml", "portion"}[basis.getSelectedItemPosition()];
                  food.kcal = number(kcal);
                  food.protein = number(protein);
                  food.carbs = number(carbs);
                  food.fiber = number(fiber);
                  if (carbohydrateBasis.getSelectedItemPosition() == 1 && food.carbs != null) {
                    if (food.fiber == null)
                      throw new IllegalArgumentException(
                          "Enter fibre to convert available carbs to total carbs, or select total"
                              + " carbohydrate from your label.");
                    food.availableCarbs = food.carbs;
                    food.carbs += food.fiber;
                  }
                  food.fat = number(fat);
                  food.code = source.code;
                  boolean changed =
                      !food.unit.equals(source.unit)
                          || !Objects.equals(food.kcal, source.kcal)
                          || !Objects.equals(food.protein, source.protein)
                          || !Objects.equals(food.carbs, source.carbs)
                          || !Objects.equals(food.fat, source.fat);
                  food.source =
                      changed && !source.source.equals("Manual entry")
                          ? "Edited label values · " + source.source
                          : source.source;
                  Double quantity = number(amount);
                  if (quantity == null)
                    throw new IllegalArgumentException("Enter the quantity eaten.");
                  store.add(
                      day, TrackerStore.SLOTS[slot.getSelectedItemPosition()], food, quantity);
                  dialog.dismiss();
                  setResult(RESULT_OK);
                  finish();
                } catch (IllegalArgumentException invalid) {
                  error.setText(
                      invalid instanceof NumberFormatException
                          ? "Enter valid numbers. Use a decimal point or comma."
                          : invalid.getMessage());
                }
              });
        });
    dialog.show();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    if (foodDialog != null && foodDialog.isShowing()) {
      Bundle draft = new Bundle();
      draft.putString("source", new com.google.gson.Gson().toJson(editingSource));
      draft.putInt("slot", editingSlot.getSelectedItemPosition());
      draft.putInt("basis", editingBasis.getSelectedItemPosition());
      draft.putInt("carbsBasis", editingCarbsBasis.getSelectedItemPosition());
      for (int id :
          new int[] {
            R.id.food_name,
            R.id.food_amount,
            R.id.food_kcal,
            R.id.food_protein,
            R.id.food_carbs,
            R.id.food_fiber,
            R.id.food_fat
          }) {
        EditText field = foodDialog.findViewById(id);
        draft.putString(Integer.toString(id), field.getText().toString());
      }
      state.putBundle("foodDraft", draft);
    }
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    if (foodDialog != null) foodDialog.dismiss();
    if (api != null) api.cancel();
    if (store != null) store.close();
    super.onDestroy();
  }
}
