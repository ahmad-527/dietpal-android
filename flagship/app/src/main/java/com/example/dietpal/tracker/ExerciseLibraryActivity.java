package com.example.dietpal.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dietpal.R;
import com.example.dietpal.recipes.RecipeUi;
import com.google.android.material.card.MaterialCardView;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public final class ExerciseLibraryActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    LinearLayout root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Exercise library");
    RecyclerView list = new RecyclerView(this);
    list.setId(R.id.exercise_library);
    list.setLayoutManager(new GridLayoutManager(this, RecipeUi.compact(this) ? 1 : 2));
    int padding = RecipeUi.dp(this, 12);
    list.setPadding(padding, padding, padding, padding);
    list.setClipToPadding(false);
    root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
    list.setAdapter(
        new RecyclerView.Adapter<Holder>() {
          @NonNull
          public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            MaterialCardView card = new MaterialCardView(ExerciseLibraryActivity.this);
            card.setRadius(RecipeUi.dp(ExerciseLibraryActivity.this, 18));
            card.setCardElevation(0);
            card.setCardBackgroundColor(RecipeUi.WHITE);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
            params.setMargins(padding / 2, padding / 2, padding / 2, padding / 2);
            card.setLayoutParams(params);
            LinearLayout body = RecipeUi.column(ExerciseLibraryActivity.this);
            body.setPadding(padding, padding, padding, padding);
            card.addView(body);
            GifImageView image = new GifImageView(ExerciseLibraryActivity.this);
            image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            image.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            body.addView(
                image,
                new LinearLayout.LayoutParams(-1, RecipeUi.dp(ExerciseLibraryActivity.this, 130)));
            android.widget.TextView title =
                RecipeUi.text(ExerciseLibraryActivity.this, "", 16, true);
            title.setMinLines(2);
            body.addView(title);
            body.addView(RecipeUi.text(ExerciseLibraryActivity.this, "Open timer →", 12, false));
            card.setFocusable(true);
            return new Holder(card, image, title);
          }

          public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.image.setImageResource(ExerciseCatalog.IMAGES[position]);
            holder.title.setText(ExerciseCatalog.NAMES[position]);
            holder.itemView.setContentDescription(
                ExerciseCatalog.NAMES[position] + ". Open exercise timer");
            holder.itemView.setOnClickListener(
                v ->
                    startActivity(
                        new Intent(ExerciseLibraryActivity.this, GuidedExerciseActivity.class)
                            .putExtra("exercise", position)
                            .putExtra("day", getIntent().getStringExtra("day"))));
          }

          public int getItemCount() {
            return ExerciseCatalog.NAMES.length;
          }

          @Override
          public void onViewDetachedFromWindow(@NonNull Holder holder) {
            if (holder.image.getDrawable() instanceof GifDrawable)
              ((GifDrawable) holder.image.getDrawable()).stop();
          }

          @Override
          public void onViewAttachedToWindow(@NonNull Holder holder) {
            if (holder.image.getDrawable() instanceof GifDrawable)
              ((GifDrawable) holder.image.getDrawable()).start();
          }
        });
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final GifImageView image;
    final android.widget.TextView title;

    Holder(MaterialCardView card, GifImageView image, android.widget.TextView title) {
      super(card);
      this.image = image;
      this.title = title;
    }
  }
}
