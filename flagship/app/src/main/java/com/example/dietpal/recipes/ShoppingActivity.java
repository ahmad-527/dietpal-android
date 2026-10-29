package com.example.dietpal.recipes;

import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dietpal.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.Map;

public final class ShoppingActivity extends AppCompatActivity {
  private RecipeStore store;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    store = new RecipeStore(this);
    render();
  }

  private void render() {
    LinearLayout root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Shopping list");
    LinearLayout body = RecipeUi.scroll(this, root);

    body.addView(
        RecipeUi.text(
            this,
            "Your ingredient checklist, saved on this device. Quantities stay grouped by recipe.",
            14,
            false));
    RecipeUi.gap(this, body, 20);
    Map<String, RecipeStore.ShoppingItem> items = store.shopping();
    if (items.isEmpty()) {
      LinearLayout card = RecipeUi.cardBody(this, body);
      card.addView(RecipeUi.text(this, "Your shopping list is empty", 19, true));
      card.addView(
          RecipeUi.text(this, "Open a recipe and tap Add ingredients to list.", 16, false));
      return;
    }
    String last = "";
    LinearLayout group = body;
    for (Map.Entry<String, RecipeStore.ShoppingItem> entry : items.entrySet()) {
      RecipeStore.ShoppingItem item = entry.getValue();
      if (!last.equals(item.recipe)) {
        group = RecipeUi.cardBody(this, body);
        group.addView(RecipeUi.text(this, item.recipe, 18, true));
        last = item.recipe;
      }
      MaterialCheckBox check = new MaterialCheckBox(this);
      check.setText(item.ingredient);
      check.setTextColor(RecipeUi.INK);
      check.setTextSize(16);
      check.setMinHeight(RecipeUi.dp(this, 48));
      check.setChecked(item.checked);
      check.setOnCheckedChangeListener(
          (button, checked) -> store.checkItem(entry.getKey(), checked));
      group.addView(check);
    }
    android.widget.Button clear = RecipeUi.button(this, "Remove checked items", false);
    clear.setId(R.id.shopping_clear_checked);
    clear.setOnClickListener(
        v -> {
          store.clearChecked();
          render();
        });
    body.addView(clear);
  }
}
