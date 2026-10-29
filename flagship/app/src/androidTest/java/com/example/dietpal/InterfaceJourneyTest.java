package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.dietpal.Models.RecipeActivity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Exercise real top-level navigation and the system large-font configuration on a disposable AVD.
 */
@RunWith(AndroidJUnit4.class)
public class InterfaceJourneyTest {
  private void emulatorOnly() {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
  }

  private String shell(String command) throws Exception {
    try (ParcelFileDescriptor descriptor =
            InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .executeShellCommand(command);
        FileInputStream input = new FileInputStream(descriptor.getFileDescriptor());
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int n;
      while ((n = input.read(buffer)) >= 0) out.write(buffer, 0, n);
      return new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
    }
  }

  private void shot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(bitmap);
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File folder = new File(app.getExternalFilesDir(null), "flagship_screenshots");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }
  }

  @Test
  public void recipesCanNavigateDirectlyToEveryMainDestination() throws Exception {
    emulatorOnly();
    try (ActivityScenario<RecipeActivity> scenario =
        ActivityScenario.launch(RecipeActivity.class)) {
      onView(withId(R.id.recipe_query)).check(matches(isDisplayed()));
      onView(
              allOf(
                  withText("Profile"),
                  isDescendantOfA(withId(R.id.flagship_navigation)),
                  isDisplayed()))
          .perform(click());
      onView(withId(R.id.profile_export)).perform(scrollTo()).check(matches(isDisplayed()));
      onView(
              allOf(
                  withText("Today"),
                  isDescendantOfA(withId(R.id.flagship_navigation)),
                  isDisplayed()))
          .perform(click());
      onView(withId(R.id.diary_add_food)).perform(scrollTo()).check(matches(isDisplayed()));
      shot("today-final");
    }
  }

  @Test
  public void largeSystemTextKeepsLoggingAndRecipeSearchReachable() throws Exception {
    emulatorOnly();
    String previous = shell("settings get system font_scale");
    assertTrue(previous.equals("null") || previous.matches("[0-9.]+"));
    try {
      shell("settings put system font_scale 1.5");
      SystemClock.sleep(1200);
      try (ActivityScenario<FlagshipActivity> scenario =
          ActivityScenario.launch(FlagshipActivity.class)) {
        scenario.onActivity(
            a -> assertTrue(a.getResources().getConfiguration().fontScale >= 1.49f));
        onView(withId(R.id.diary_add_food)).perform(scrollTo()).check(matches(isDisplayed()));
        shot("today-large-text");
        onView(
                allOf(
                    withText("Profile"),
                    isDescendantOfA(withId(R.id.flagship_navigation)),
                    isDisplayed()))
            .perform(click());
        onView(withId(R.id.profile_export)).perform(scrollTo()).check(matches(isDisplayed()));
        shot("profile-large-text");
      }
      try (ActivityScenario<RecipeActivity> scenario =
          ActivityScenario.launch(RecipeActivity.class)) {
        onView(withId(R.id.recipe_query)).perform(replaceText("oats"), closeSoftKeyboard());
        onView(withId(R.id.recipe_query)).check(matches(isDisplayed()));
        shot("recipes-large-text");
      }
    } finally {
      shell(
          previous.equals("null")
              ? "settings delete system font_scale"
              : "settings put system font_scale " + previous);
      SystemClock.sleep(800);
    }
  }
}
