package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.dietpal.Models.RecipeActivity;
import com.example.dietpal.recipes.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BalancedRecipeTest {
  private void shot(Context app, String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    File folder = new File(app.getExternalFilesDir(null), "flagship_screenshots");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      image.recycle();
    }
  }

  @Test
  public void bundledRecipesAndCalculatedDetailsWorkOffline() throws Exception {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Meal first = new RecipeStore(app).balanced().get(0);
    try (ActivityScenario<RecipeActivity> scenario =
        ActivityScenario.launch(RecipeActivity.class)) {
      long until = SystemClock.elapsedRealtime() + 10000;
      AtomicBoolean ready = new AtomicBoolean();
      while (!ready.get() && SystemClock.elapsedRealtime() < until) {
        scenario.onActivity(
            a -> {
              RecyclerView list = a.findViewById(R.id.recipe_list);
              ready.set(
                  list.getAdapter() != null
                      && list.getAdapter().getItemCount() > 20
                      && list.findViewHolderForAdapterPosition(0) != null);
            });
        SystemClock.sleep(100);
      }
      assertTrue("Bundled catalog did not load", ready.get());
      onView(withId(R.id.recipe_status))
          .check(matches(withText(containsString("Balanced suggestions"))));
      shot(app, "recipes-for-you");
      scenario.onActivity(
          a ->
              ((RecyclerView) a.findViewById(R.id.recipe_list))
                  .findViewHolderForAdapterPosition(0)
                  .itemView
                  .performClick());
      onView(withId(R.id.recipe_detail_title)).check(matches(withText(first.title)));
      onView(withId(R.id.recipe_detail_source)).check(matches(withText(containsString("USDA"))));
      onView(withText(containsString("Estimated nutrition")))
          .perform(scrollTo())
          .check(matches(isDisplayed()));
      shot(app, "balanced-recipe-details");
      onView(withId(R.id.recipe_ingredients)).perform(scrollTo()).check(matches(isDisplayed()));
    }
  }
}
