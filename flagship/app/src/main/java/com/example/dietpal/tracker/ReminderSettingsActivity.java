package com.example.dietpal.tracker;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dietpal.R;
import com.example.dietpal.recipes.RecipeUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.Locale;

public final class ReminderSettingsActivity extends AppCompatActivity {
  private ReminderScheduler scheduler;
  private String pending;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    scheduler = new ReminderScheduler(this);
    if (state != null) pending = state.getString("pending");
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (scheduler != null) {
      if (pending != null && scheduler.notificationsAllowed()) {
        scheduler.set(pending, true, scheduler.hour(pending), scheduler.minute(pending));
        pending = null;
      }
      scheduler.refresh();
      render();
    }
  }

  private void render() {
    LinearLayout root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Your reminders");
    LinearLayout body = RecipeUi.scroll(this, root);
    body.addView(
        RecipeUi.text(
            this,
            "Choose which reminders you want and when. They stay on this phone. Timing follows your"
                + " phone's notification and battery settings.",
            15,
            false));
    RecipeUi.gap(this, body, 16);
    if (!scheduler.notificationsAllowed())
      body.addView(
          RecipeUi.text(
              this,
              "Phone notifications are currently off for DietPal. Turn on a reminder to enable"
                  + " them.",
              14,
              false));
    for (String type : ReminderScheduler.TYPES) {
      LinearLayout card = RecipeUi.cardBody(this, body);
      String title =
          type.equals("meal") ? "Food diary" : type.equals("water") ? "Water" : "Workout";
      SwitchMaterial toggle = new SwitchMaterial(this);
      toggle.setText(title);
      toggle.setTextSize(20);
      toggle.setTextColor(RecipeUi.INK);
      toggle.setMinHeight(RecipeUi.dp(this, 48));
      toggle.setChecked(scheduler.enabled(type) && scheduler.notificationsAllowed());
      toggle.setOnCheckedChangeListener(
          (button, checked) -> {
            if (checked && !scheduler.notificationsAllowed()) {
              pending = type;
              if (Build.VERSION.SDK_INT >= 33
                  && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                      != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 501);
              else
                try {
                  startActivity(
                      new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                          .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                } catch (android.content.ActivityNotFoundException unavailable) {
                  android.widget.Toast.makeText(
                          this,
                          "Open your phone settings to enable DietPal notifications",
                          android.widget.Toast.LENGTH_LONG)
                      .show();
                }
              render();
              return;
            }
            scheduler.set(type, checked, scheduler.hour(type), scheduler.minute(type));
          });
      card.addView(toggle);
      MaterialButton time =
          RecipeUi.button(
              this,
              String.format(
                  Locale.getDefault(),
                  "Every day · %02d:%02d",
                  scheduler.hour(type),
                  scheduler.minute(type)),
              false);
      time.setOnClickListener(
          v ->
              new TimePickerDialog(
                      this,
                      (picker, h, m) -> {
                        scheduler.set(type, scheduler.enabled(type), h, m);
                        render();
                      },
                      scheduler.hour(type),
                      scheduler.minute(type),
                      android.text.format.DateFormat.is24HourFormat(this))
                  .show());
      card.addView(time);
    }
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    if (request == 501 && pending != null) {
      if (scheduler.notificationsAllowed())
        scheduler.set(pending, true, scheduler.hour(pending), scheduler.minute(pending));
      pending = null;
      render();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("pending", pending);
    super.onSaveInstanceState(state);
  }
}
