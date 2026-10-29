package com.example.dietpal;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.dietpal.planning.*;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PlannerJourneyTest {
  private final LocalDate start = LocalDate.of(2096, 2, 27);

  private Context app() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext();
  }

  private void emulatorOnly() {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
  }

  private void select(int id, String text) {
    onView(withId(id)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(String.class)), is(text))).perform(click());
  }

  private void goals() {
    onView(withId(R.id.plan_kcal)).perform(scrollTo(), replaceText("2000"));
    onView(withId(R.id.plan_protein)).perform(scrollTo(), replaceText("120"));
    onView(withId(R.id.plan_carbs)).perform(scrollTo(), replaceText("230"));
    onView(withId(R.id.plan_fat)).perform(scrollTo(), replaceText("65"), closeSoftKeyboard());
  }

  private void awaitSummary(String expected) {
    long until = SystemClock.elapsedRealtime() + 120_000;
    while (SystemClock.elapsedRealtime() < until) {
      AtomicBoolean ready = new AtomicBoolean();
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                for (Activity activity :
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                  TextView view = activity.findViewById(R.id.plan_summary);
                  if (view != null
                      && view.isShown()
                      && activity.hasWindowFocus()
                      && view.getText().toString().contains(expected)) ready.set(true);
                }
              });
      if (ready.get()) return;
      SystemClock.sleep(100);
    }
    fail("Plan preview did not finish: " + expected);
  }

  private void shot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(image);
    File folder = new File(app().getExternalFilesDir(null), "flagship_screenshots");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      image.recycle();
    }
  }

  private List<PlannedDay> backup(PlanningStore store, int days) {
    List<PlannedDay> records = new ArrayList<>();
    for (int i = 0; i < days; i++) {
      PlannedDay day = store.day(start.plusDays(i).toString());
      if (day != null) records.add(day);
    }
    return records;
  }

  private void restore(PlanningStore store, int days, List<PlannedDay> backup) {
    for (int i = 0; i < days; i++) store.remove(start.plusDays(i).toString());
    store.save(backup);
  }

  private void save() {
    onView(withId(R.id.plan_save)).perform(scrollTo(), click());
    onView(allOf(withText("Save"), isDisplayed())).perform(click());
    awaitSummary("days");
    SystemClock.sleep(500);
  }

  private void restoreProfile(String json) {
    android.content.SharedPreferences.Editor editor =
        app().getSharedPreferences("dietpal_planner", Context.MODE_PRIVATE).edit();
    if (json == null) editor.remove("lastProfile");
    else editor.putString("lastProfile", json);
    editor.commit();
  }

  @Test
  public void veganTwoMealsAndNextWeekOneMealKeepPriorWeekUnchanged() throws Exception {
    emulatorOnly();
    try (PlanningStore store = new PlanningStore(app())) {
      String previousProfile =
          app()
              .getSharedPreferences("dietpal_planner", Context.MODE_PRIVATE)
              .getString("lastProfile", null);
      List<PlannedDay> old = backup(store, 14);
      try {
        try (ActivityScenario<PlannerActivity> scenario =
            ActivityScenario.launch(
                new Intent(app(), PlannerActivity.class).putExtra("day", start.toString()))) {
          select(R.id.plan_diet, "Vegan");
          select(R.id.plan_meal_count, "2");
          goals();
          onView(withId(R.id.plan_generate)).perform(scrollTo(), click());
          awaitSummary("7 days");
          assertNull(store.day(start.toString()));
          shot("planner-vegan-preview");
          save();
          PlannedDay first = store.day(start.toString());
          assertNotNull(first);
          assertEquals(2, first.meals.size());
          for (PlannedDay.Occasion meal : first.meals)
            for (PlannedDay.Dish dish : meal.dishes) assertEquals("vegan", dish.recipe.diet);
          String firstSnapshot = new Gson().toJson(first);
          onView(withText("Change preferences or dates")).perform(scrollTo(), click());
          onView(withText("Move start forward one week")).perform(scrollTo(), click());
          select(R.id.plan_meal_count, "1");
          onView(withId(R.id.plan_generate)).perform(scrollTo(), click());
          awaitSummary("7 days");
          save();
          assertEquals(firstSnapshot, new Gson().toJson(store.day(start.toString())));
          assertEquals(1, store.day(start.plusWeeks(1).toString()).meals.size());
          shot("planner-next-week");
        }
      } finally {
        restore(store, 14, old);
        restoreProfile(previousProfile);
      }
    }
  }

  @Test
  public void fullLeapYearPreviewAndPersistenceHave366Dates() throws Exception {
    emulatorOnly();
    try (PlanningStore store = new PlanningStore(app())) {
      String previousProfile =
          app()
              .getSharedPreferences("dietpal_planner", Context.MODE_PRIVATE)
              .getString("lastProfile", null);
      List<PlannedDay> old = backup(store, 366);
      try {
        try (ActivityScenario<PlannerActivity> scenario =
            ActivityScenario.launch(
                new Intent(app(), PlannerActivity.class).putExtra("day", start.toString()))) {
          select(R.id.plan_diet, "Mixed");
          select(R.id.plan_meal_count, "3");
          select(R.id.plan_horizon, "Year");
          goals();
          onView(withId(R.id.plan_generate)).perform(scrollTo(), click());
          awaitSummary("366 days");
          shot("planner-year-preview");
          save();
          for (int i = 0; i < 366; i++)
            assertNotNull("Missing date " + i, store.day(start.plusDays(i).toString()));
          assertNull(store.day(start.plusYears(1).toString()));
        }
      } finally {
        restore(store, 366, old);
        restoreProfile(previousProfile);
      }
    }
  }
}
