package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RangeSupplementJourneyTest {
  private Context app() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext();
  }

  private void emulatorOnly() {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
  }

  private void set(int id, String text) {
    onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
  }

  private void restorePreference(String key, String value) {
    SharedPreferences.Editor editor =
        app().getSharedPreferences("dietpal_planner", Context.MODE_PRIVATE).edit();
    if (value == null) editor.remove(key);
    else editor.putString(key, value);
    editor.commit();
  }

  private void awaitSummary() {
    long until = SystemClock.elapsedRealtime() + 60_000;
    while (SystemClock.elapsedRealtime() < until) {
      AtomicBoolean ready = new AtomicBoolean();
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                for (Activity a :
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                  TextView text = a.findViewById(R.id.plan_summary);
                  if (text != null
                      && text.isShown()
                      && a.hasWindowFocus()
                      && text.getText().toString().contains("7 days")) ready.set(true);
                }
              });
      if (ready.get()) return;
      SystemClock.sleep(100);
    }
    fail("No generated range plan preview");
  }

  private void shot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(image);
    File dir = new File(app().getExternalFilesDir(null), "flagship_screenshots");
    assertTrue(dir.isDirectory() || dir.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      image.recycle();
    }
  }

  @Test
  public void trackingTargetsPersistRangesAndCalorieMinimum() throws Exception {
    emulatorOnly();
    SharedPreferences prefs = app().getSharedPreferences("dietpal_planner", Context.MODE_PRIVATE);
    String old = prefs.getString("targetTimeline", null);
    LocalDate date = LocalDate.now();
    try (ActivityScenario<FlagshipActivity> scenario =
        ActivityScenario.launch(FlagshipActivity.class)) {
      onView(allOf(withText("Profile"), isDisplayed())).perform(click());
      onView(withId(R.id.tracker_goal)).perform(scrollTo(), click());
      set(R.id.goal_energy, ">2000");
      set(R.id.goal_protein, "180");
      set(R.id.goal_carbs, "100-150");
      set(R.id.goal_fat, "60");
      set(R.id.goal_water, "2000");
      onView(allOf(withText("Save"), isDisplayed())).perform(click());
      try (PlanningStore store = new PlanningStore(app())) {
        PlannerProfile saved = store.targetsAt(date.toString());
        assertNotNull(saved);
        assertEquals(">2000", saved.goals()[0].raw());
        assertEquals("100-150", saved.goals()[2].raw());
      }
      onView(allOf(withText("Today"), isDisplayed())).perform(click());
      scenario.recreate();
      onView(withText(containsString("100-150"))).check(matches(isDisplayed()));
      shot("today-range-goals");
    } finally {
      restorePreference("targetTimeline", old);
    }
  }

  @Test
  public void rangePlanIncludesChosenWheyLabelWithoutChangingItsServing() throws Exception {
    emulatorOnly();
    LocalDate start = LocalDate.of(2098, 4, 3);
    SharedPreferences prefs = app().getSharedPreferences("dietpal_planner", Context.MODE_PRIVATE);
    String oldProfile = prefs.getString("lastProfile", null);
    try (PlanningStore store = new PlanningStore(app())) {
      List<PlannedDay> backup = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        PlannedDay old = store.day(start.plusDays(i).toString());
        if (old != null) backup.add(old);
      }
      try (ActivityScenario<PlannerActivity> scenario =
          ActivityScenario.launch(
              new Intent(app(), PlannerActivity.class).putExtra("day", start.toString()))) {
        set(R.id.plan_kcal, "<=2000");
        set(R.id.plan_protein, "180");
        set(R.id.plan_carbs, "100-150");
        set(R.id.plan_fat, "60");
        onView(withId(R.id.plan_supplement)).perform(scrollTo(), click());
        set(R.id.supplement_name, "Test whey label / 30 g scoop");
        set(R.id.supplement_amount, "1");
        set(R.id.supplement_kcal, "120");
        set(R.id.supplement_protein, "24");
        set(R.id.supplement_carbs, "3");
        set(R.id.supplement_fat, "2");
        onView(allOf(withText("Include in draft"), isDisplayed())).perform(click());
        onView(withId(R.id.plan_generate)).perform(scrollTo(), click());
        awaitSummary();
        shot("range-supplement-preview");
        onView(withId(R.id.plan_save)).perform(scrollTo(), click());
        onView(allOf(withText("Save"), isDisplayed())).perform(click());
        SystemClock.sleep(700);
        for (int i = 0; i < 7; i++) {
          PlannedDay day = store.day(start.plusDays(i).toString());
          assertNotNull(day);
          assertEquals("100-150", day.profile.goals()[2].raw());
          assertEquals("<=2000", day.profile.goals()[0].raw());
          assertEquals(1, day.profile.supplements.size());
          assertEquals(1, day.profile.supplements.get(0).servings, 0);
          assertTrue(day.profile.supplements.get(0).allergens.contains("milk"));
          assertArrayEquals(new double[] {120, 24, 3, 2}, day.profile.fixedNutrition(), .00001);
          assertTrue(Arrays.toString(day.totals()), day.withinTolerance());
        }
      } finally {
        for (int i = 0; i < 7; i++) store.remove(start.plusDays(i).toString());
        store.save(backup);
        restorePreference("lastProfile", oldProfile);
      }
    }
  }
}
