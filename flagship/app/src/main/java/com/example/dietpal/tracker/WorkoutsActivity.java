package com.example.dietpal.tracker;

import android.content.Intent;
import android.os.Bundle;
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

public final class WorkoutsActivity extends AppCompatActivity {
  private WorkoutStore store;
  private String day;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    store = new WorkoutStore(this);
    day = getIntent().getStringExtra("day");
    try {
      LocalDate.parse(day);
    } catch (RuntimeException invalid) {
      day = LocalDate.now().toString();
    }
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (store != null) render();
  }

  private void render() {
    LinearLayout root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Movement");
    LinearLayout body = RecipeUi.scroll(this, root);
    body.addView(
        RecipeUi.text(
            this,
            "Your workout log for "
                + day
                + ". Duration and repetitions are records, not an estimate of calories burned.",
            15,
            false));
    MaterialButton guided = RecipeUi.button(this, "Open guided exercises", true);
    guided.setOnClickListener(
        v -> startActivity(new Intent(this, ExerciseLibraryActivity.class).putExtra("day", day)));
    body.addView(guided);
    MaterialButton log = RecipeUi.button(this, "Log a workout", false);
    log.setOnClickListener(v -> log());
    body.addView(log);
    RecipeUi.gap(this, body, 16);
    int count = 0, total = 0;
    for (WorkoutStore.Entry entry : store.entries(day)) {
      count++;
      total += entry.seconds;
      LinearLayout card = RecipeUi.cardBody(this, body);
      card.addView(RecipeUi.text(this, entry.title, 18, true));
      card.addView(
          RecipeUi.text(
              this,
              entry.seconds / 60
                  + " min "
                  + entry.seconds % 60
                  + " sec"
                  + (entry.sets == null ? "" : " · " + entry.sets + " sets")
                  + (entry.reps == null ? "" : " · " + entry.reps + " reps"),
              15,
              false));
      card.addView(RecipeUi.text(this, entry.source, 12, false));
      MaterialButton remove = RecipeUi.button(this, "Remove record", false);
      remove.setOnClickListener(
          v ->
              new AlertDialog.Builder(this)
                  .setTitle("Remove this record?")
                  .setNegativeButton("Cancel", null)
                  .setPositiveButton(
                      "Remove",
                      (dialog, which) -> {
                        store.remove(entry.id);
                        render();
                      })
                  .show());
      card.addView(remove);
    }
    if (count == 0)
      body.addView(RecipeUi.text(this, "No workouts logged for this date.", 17, true));
    else
      body.addView(
          RecipeUi.text(this, count + " records · " + total / 60 + " logged minutes", 17, false));
  }

  private EditText field(LinearLayout form, String label, boolean numeric) {
    form.addView(RecipeUi.text(this, label, 14, false));
    EditText input = new EditText(this);
    input.setHint(label);
    input.setSingleLine(true);
    if (numeric) input.setInputType(InputType.TYPE_CLASS_NUMBER);
    form.addView(input);
    return input;
  }

  private void log() {
    LinearLayout form = RecipeUi.column(this);
    int p = RecipeUi.dp(this, 20);
    form.setPadding(p, p, p, p);
    EditText title = field(form, "Workout name", false),
        minutes = field(form, "Duration (minutes), optional", true),
        sets = field(form, "Sets, optional", true),
        reps = field(form, "Repetitions, optional", true);
    TextView error = RecipeUi.text(this, "", 14, false);
    form.addView(error);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Log workout · " + day)
            .setView(form)
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
                        Integer m = number(minutes), s = number(sets), r = number(reps);
                        if (m != null && m > 1440)
                          throw new IllegalArgumentException("Enter a duration within one day.");
                        store.log(
                            day,
                            title.getText().toString(),
                            m == null ? 0 : m * 60,
                            s,
                            r,
                            "Manually logged");
                        dialog.dismiss();
                        render();
                      } catch (IllegalArgumentException invalid) {
                        error.setText(
                            invalid instanceof NumberFormatException
                                ? "Enter valid whole numbers."
                                : invalid.getMessage());
                      }
                    }));
    dialog.show();
  }

  private Integer number(EditText input) {
    String value = input.getText().toString().trim();
    return value.isEmpty() ? null : Integer.parseInt(value);
  }

  @Override
  protected void onDestroy() {
    if (store != null) store.close();
    super.onDestroy();
  }
}
