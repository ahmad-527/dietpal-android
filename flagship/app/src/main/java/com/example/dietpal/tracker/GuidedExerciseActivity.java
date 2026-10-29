package com.example.dietpal.tracker;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dietpal.R;
import com.example.dietpal.recipes.RecipeUi;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.util.Locale;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/** A user-controlled timer. Saving a completed session requires explicit confirmation. */
public final class GuidedExerciseActivity extends AppCompatActivity {
  private long duration = 30_000, remaining = 30_000, deadline;
  private boolean running, finished, saved;
  private CountDownTimer timer;
  private TextView clock;
  private MaterialButton start, save, length;
  private GifImageView image;
  private int exercise;
  private String day;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    exercise = getIntent().getIntExtra("exercise", 0);
    if (exercise < 0 || exercise >= ExerciseCatalog.NAMES.length) {
      finish();
      return;
    }
    day = getIntent().getStringExtra("day");
    try {
      LocalDate.parse(day);
    } catch (RuntimeException invalid) {
      day = LocalDate.now().toString();
    }
    if (state != null) {
      duration = state.getLong("duration", duration);
      remaining = state.getLong("remaining", duration);
      finished = state.getBoolean("finished");
      saved = state.getBoolean("saved");
    }
    LinearLayout root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Exercise timer");
    LinearLayout body = RecipeUi.scroll(this, root);
    RecipeUi.section(this, body, ExerciseCatalog.NAMES[exercise]);
    LinearLayout media = RecipeUi.cardBody(this, body);
    image = new GifImageView(this);
    image.setId(R.id.guided_image);
    image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
    image.setContentDescription("Animated exercise demonstration");
    image.setImageResource(ExerciseCatalog.IMAGES[exercise]);
    media.addView(image, new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 240)));
    clock = RecipeUi.value(this, "", 52, RecipeUi.BLUE);
    clock.setId(R.id.guided_clock);
    clock.setGravity(android.view.Gravity.CENTER);
    media.addView(clock, new LinearLayout.LayoutParams(-1, -2));
    RecipeUi.gap(this, media, 12);
    length = RecipeUi.button(this, "", false);
    length.setId(R.id.guided_duration);
    length.setOnClickListener(v -> chooseDuration());
    media.addView(length);
    LinearLayout controls = RecipeUi.row(this);
    start = RecipeUi.button(this, "Start timer", true);
    start.setId(R.id.guided_start);
    start.setOnClickListener(
        v -> {
          if (running) pause();
          else begin();
        });
    controls.addView(start, RecipeUi.weighted(this));
    MaterialButton reset = RecipeUi.button(this, "Reset", false);
    reset.setOnClickListener(
        v -> {
          pause();
          remaining = duration;
          finished = false;
          saved = false;
          update();
        });
    controls.addView(reset, RecipeUi.weighted(this));
    media.addView(controls);
    save = RecipeUi.button(this, "Save completed session", true);
    save.setId(R.id.guided_save);
    save.setOnClickListener(
        v -> {
          if (!finished || saved) return;
          try (WorkoutStore store = new WorkoutStore(this)) {
            store.log(
                day,
                ExerciseCatalog.NAMES[exercise],
                (int) (duration / 1000),
                null,
                null,
                "Guided timer · user confirmed");
          }
          saved = true;
          update();
        });
    body.addView(save);
    body.addView(
        RecipeUi.text(
            this,
            "Choose your duration. Leaving this screen pauses the timer. Save only the session you"
                + " completed; no calorie-burn estimate is assigned.",
            13,
            false));
    update();
  }

  private void chooseDuration() {
    if (running) pause();
    EditText seconds = new EditText(this);
    seconds.setId(R.id.guided_seconds);
    seconds.setInputType(InputType.TYPE_CLASS_NUMBER);
    seconds.setText(Long.toString(duration / 1000));
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Duration in seconds")
            .setView(seconds)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        int value = Integer.parseInt(seconds.getText().toString());
                        if (value < 5 || value > 3600) throw new NumberFormatException();
                        duration = remaining = value * 1000L;
                        finished = false;
                        saved = false;
                        dialog.dismiss();
                        update();
                      } catch (NumberFormatException invalid) {
                        seconds.setError("Choose 5–3600 seconds.");
                      }
                    }));
    dialog.show();
  }

  private void begin() {
    if (finished || remaining <= 0) return;
    running = true;
    deadline = SystemClock.elapsedRealtime() + remaining;
    timer =
        new CountDownTimer(remaining, 250) {
          @Override
          public void onTick(long left) {
            remaining = left;
            update();
          }

          @Override
          public void onFinish() {
            running = false;
            remaining = 0;
            finished = true;
            update();
            clock.announceForAccessibility("Timer finished");
          }
        }.start();
    update();
  }

  private void pause() {
    if (running) {
      remaining = Math.max(0, deadline - SystemClock.elapsedRealtime());
      if (remaining == 0) finished = true;
    }
    if (timer != null) timer.cancel();
    timer = null;
    running = false;
    update();
  }

  private void update() {
    if (clock == null) return;
    long seconds = (remaining + 999) / 1000;
    clock.setText(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
    length.setText("Duration · " + duration / 1000 + " seconds");
    start.setText(running ? "Pause" : "Start timer");
    start.setEnabled(!finished);
    save.setText(saved ? "Saved to movement log" : "Save completed session");
    save.setEnabled(finished && !saved);
  }

  @Override
  protected void onPause() {
    pause();
    if (image != null && image.getDrawable() instanceof GifDrawable)
      ((GifDrawable) image.getDrawable()).stop();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (image != null && image.getDrawable() instanceof GifDrawable)
      ((GifDrawable) image.getDrawable()).start();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putLong("duration", duration);
    state.putLong(
        "remaining", running ? Math.max(0, deadline - SystemClock.elapsedRealtime()) : remaining);
    state.putBoolean("finished", finished);
    state.putBoolean("saved", saved);
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    if (timer != null) timer.cancel();
    if (image != null && image.getDrawable() instanceof GifDrawable)
      ((GifDrawable) image.getDrawable()).stop();
    super.onDestroy();
  }
}
