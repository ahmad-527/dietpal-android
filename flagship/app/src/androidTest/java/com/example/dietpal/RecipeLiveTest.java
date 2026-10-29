package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.dietpal.Models.RecipeActivity;
import com.example.dietpal.recipes.RecipeStore;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Requires actual HTTPS results: never substitutes fixtures or treats an offline fallback as a
 * pass.
 */
@RunWith(AndroidJUnit4.class)
public class RecipeLiveTest {
  private void awaitText(int id, String expected) {
    long until = SystemClock.elapsedRealtime() + 35_000;
    while (SystemClock.elapsedRealtime() < until) {
      AtomicBoolean ok = new AtomicBoolean();
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                for (Activity a :
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                  TextView v = a.findViewById(id);
                  if (v != null
                      && v.isShown()
                      && a.hasWindowFocus()
                      && v.getText().toString().contains(expected)) ok.set(true);
                }
              });
      if (ok.get()) return;
      SystemClock.sleep(100);
    }
    fail("Real response/UI did not appear: " + expected);
  }

  private boolean hasLoadedPhoto(android.view.View view) {
    if (view instanceof android.widget.ImageView
        && ((android.widget.ImageView) view).getDrawable()
            instanceof android.graphics.drawable.BitmapDrawable) return true;
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++)
        if (hasLoadedPhoto(group.getChildAt(i))) return true;
    }
    return false;
  }

  private void awaitRecipePhoto() {
    long until = SystemClock.elapsedRealtime() + 35_000;
    while (SystemClock.elapsedRealtime() < until) {
      AtomicBoolean loaded = new AtomicBoolean();
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                for (Activity a :
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                  android.view.View list = a.findViewById(R.id.recipe_list);
                  if (list != null && hasLoadedPhoto(list)) loaded.set(true);
                }
              });
      if (loaded.get()) return;
      SystemClock.sleep(100);
    }
    fail("A real recipe photo did not load");
  }

  private void shot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(500, 5000);
    Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(bitmap);
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File dir = new File(c.getExternalFilesDir(null), "flagship_screenshots");
    assertTrue(dir.isDirectory() || dir.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }
  }

  @Test
  public void liveSearchDetailsSaveAndShopping() throws Exception {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    try (ActivityScenario<RecipeActivity> activity =
        ActivityScenario.launch(RecipeActivity.class)) {
      onView(withId(R.id.recipe_browse)).perform(click());
      awaitText(R.id.recipe_status, "Live recipes");
      awaitRecipePhoto();
      shot("recipes-discover");
      onView(withId(R.id.recipe_query))
          .perform(replaceText("Arrabiata"), pressImeActionButton(), closeSoftKeyboard());
      awaitText(R.id.recipe_status, "Live recipes");
      onView(withId(R.id.recipe_status)).check(matches(withText(containsString("1 recipe"))));
      awaitRecipePhoto();
      shot("recipes-search");
      onView(withText("Spicy Arrabiata Penne")).perform(click());
      awaitText(R.id.recipe_detail_title, "Spicy Arrabiata Penne");
      onView(withId(R.id.recipe_ingredients)).check(matches(isDisplayed()));
      shot("recipe-details");
      if (!new RecipeStore(InstrumentationRegistry.getInstrumentation().getTargetContext())
          .isSaved("mealdb-52771")) onView(withId(R.id.recipe_save)).perform(scrollTo(), click());
      onView(withId(R.id.recipe_save)).check(matches(withText("Saved ✓")));
      onView(withId(R.id.recipe_add_ingredients)).perform(scrollTo(), click());
      Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
      RecipeStore store = new RecipeStore(app);
      assertTrue(store.isSaved("mealdb-52771"));
      assertFalse(store.shopping().isEmpty());
      pressBack();
      onView(withId(R.id.recipe_saved)).perform(click());
      awaitText(R.id.recipe_status, "Saved on this device");
      shot("recipes-saved");
      onView(withId(R.id.recipe_shopping)).perform(click());
      onView(withText("Spicy Arrabiata Penne")).check(matches(isDisplayed()));
      shot("shopping-list");
    }
  }
}
