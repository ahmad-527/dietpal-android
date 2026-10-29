package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.dietpal.recipes.*;
import com.example.dietpal.tracker.TrackerStore;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FlagshipJourneyTest {
  private void shot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(500, 5000);
    Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(image);
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File dir = new File(c.getExternalFilesDir(null), "flagship_screenshots");
    assertTrue(dir.isDirectory() || dir.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      image.recycle();
    }
  }

  @Test
  public void logPortionPersistWaterPlanAndProgress() throws Exception {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    String today = LocalDate.now().toString();
    String name = "Oats & yogurt";
    java.util.Set<String> originalIds = new java.util.HashSet<>();
    try (TrackerStore store = new TrackerStore(app)) {
      for (TrackerStore.Entry entry : store.entries(today)) originalIds.add(entry.id);
      int water = store.water(today);
      double[] goals = {
        store.goal("kcal"),
        store.goal("protein"),
        store.goal("carbs"),
        store.goal("fat"),
        store.goal("water")
      };
      double before = new TrackerStore.Totals(store.entries(today)).kcal;
      RecipeStore recipes = new RecipeStore(app);
      String planKey = today + ":Lunch";
      Meal previous = recipes.plans().get(planKey);
      store.goals(2000, 100, 250, 70, 2000);
      try (ActivityScenario<FlagshipActivity> scenario =
          ActivityScenario.launch(FlagshipActivity.class)) {
        shot("today-empty");
        onView(withId(R.id.diary_add_food)).perform(scrollTo(), click());
        onView(withId(R.id.food_manual)).perform(scrollTo(), click());
        onView(withId(R.id.food_name)).perform(scrollTo(), replaceText(name));
        onView(withId(R.id.food_amount)).perform(scrollTo(), replaceText("150"));
        onView(withId(R.id.food_kcal)).perform(scrollTo(), replaceText("200"));
        onView(withId(R.id.food_protein)).perform(scrollTo(), replaceText("10"));
        onView(withId(R.id.food_carbs)).perform(scrollTo(), replaceText("20"));
        onView(withId(R.id.food_fat)).perform(scrollTo(), replaceText("8"), closeSoftKeyboard());
        onView(withId(R.id.food_save)).perform(click());
        onView(withId(R.id.diary_total))
            .check(matches(withText(containsString(Long.toString(Math.round(before + 300))))));
        assertEquals(before + 300, new TrackerStore.Totals(store.entries(today)).kcal, 0.0001);
        shot("today-diary");
        onView(withId(R.id.water_add)).perform(scrollTo(), click());
        assertEquals(water + 250, store.water(today));
        scenario.recreate();
        onView(withId(R.id.diary_total))
            .check(matches(withText(containsString(Long.toString(Math.round(before + 300))))));
        Meal meal = recipes.starters().get(0);
        recipes.plan(today, "Lunch", meal);
        onView(allOf(withText("Plan"), isDisplayed())).perform(click());
        onView(withText(meal.title)).check(matches(isDisplayed()));
        shot("meal-plan");
        onView(allOf(withText("Progress"), isDisplayed())).perform(click());
        shot("progress");
        onView(allOf(withText("Profile"), isDisplayed())).perform(click());
        onView(withId(R.id.profile_export)).perform(scrollTo()).check(matches(isDisplayed()));
        shot("profile");
      } finally {
        for (TrackerStore.Entry entry : store.entries(today))
          if (!originalIds.contains(entry.id)) store.remove(entry.id);
        store.changeWater(today, water - store.water(today));
        store.goals(goals[0], goals[1], goals[2], goals[3], goals[4]);
        if (previous == null) recipes.removePlan(planKey);
        else recipes.plan(today, "Lunch", previous);
      }
    }
  }
}
