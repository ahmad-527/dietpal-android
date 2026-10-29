package com.example.dietpal.recipes;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dietpal.R;
import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;
import java.util.List;
import java.util.function.Consumer;

public final class MealAdapter extends RecyclerView.Adapter<MealAdapter.Holder> {
  private final Activity activity;
  private final List<Meal> meals;
  private final Consumer<Meal> open;

  public MealAdapter(Activity activity, List<Meal> meals, Consumer<Meal> open) {
    this.activity = activity;
    this.meals = meals;
    this.open = open;
  }

  @NonNull
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
    MaterialCardView card = new MaterialCardView(activity);
    card.setCardBackgroundColor(RecipeUi.WHITE);
    card.setRadius(RecipeUi.dp(activity, 18));
    card.setCardElevation(0);
    card.setStrokeWidth(0);
    RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
    params.setMargins(
        RecipeUi.dp(activity, 4), 0, RecipeUi.dp(activity, 4), RecipeUi.dp(activity, 12));
    card.setLayoutParams(params);
    LinearLayout content = RecipeUi.column(activity);
    card.addView(content);
    ImageView image = new ImageView(activity);
    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    image.setImportantForAccessibility(ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO);
    content.addView(
        image,
        new LinearLayout.LayoutParams(
            -1, RecipeUi.dp(activity, RecipeUi.compact(activity) ? 156 : 130)));
    LinearLayout labels = RecipeUi.column(activity);
    int pad = RecipeUi.dp(activity, 12);
    labels.setPadding(pad, pad, pad, pad);
    content.addView(labels);
    TextView meta = RecipeUi.text(activity, "", 12, false);
    meta.setTextColor(RecipeUi.GREEN);
    labels.addView(meta);
    TextView title = RecipeUi.text(activity, "", 16, true);
    androidx.core.view.ViewCompat.setAccessibilityHeading(title, false);
    title.setMinLines(2);
    labels.addView(title);
    RecipeUi.gap(activity, labels, 8);
    TextView action = RecipeUi.text(activity, "View recipe  →", 14, false);
    action.setTextColor(RecipeUi.GREEN);
    labels.addView(action);
    card.setFocusable(true);
    card.setClickable(true);
    return new Holder(card, image, title, meta);
  }

  public void onBindViewHolder(@NonNull Holder holder, int position) {
    Meal meal = meals.get(position);
    holder.title.setText(meal.title);
    holder.meta.setText(
        meal.nutrition == null
            ? meal.subtitle()
            : String.format(
                java.util.Locale.getDefault(),
                "~%.0f kcal · P %.0f g",
                meal.nutrition.kcal,
                meal.nutrition.protein));
    Picasso.get().cancelRequest(holder.image);
    holder.image.setImageResource(
        meal.nutrition == null
            ? R.drawable.recipe_placeholder
            : meal.mealType.equals("Breakfast")
                ? R.drawable.balanced_breakfast
                : meal.mealType.equals("Main")
                    ? R.drawable.balanced_main
                    : R.drawable.balanced_side);
    android.view.ViewGroup.LayoutParams imageSize = holder.image.getLayoutParams();
    imageSize.height = RecipeUi.dp(activity, meal.nutrition == null ? 130 : 105);
    holder.image.setLayoutParams(imageSize);
    if (!meal.image.isEmpty())
      Picasso.get()
          .load(meal.image)
          .placeholder(R.drawable.recipe_placeholder)
          .error(R.drawable.recipe_placeholder)
          .fit()
          .centerCrop()
          .into(holder.image);
    holder.itemView.setContentDescription(meal.title + ". " + meal.subtitle() + ". View recipe");
    holder.itemView.setOnClickListener(v -> open.accept(meal));
  }

  public int getItemCount() {
    return meals.size();
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final ImageView image;
    final TextView title, meta;

    Holder(MaterialCardView view, ImageView image, TextView title, TextView meta) {
      super(view);
      this.image = image;
      this.title = title;
      this.meta = meta;
    }
  }
}
