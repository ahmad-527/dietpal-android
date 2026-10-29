package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Emulator-only end-to-end check using synthetic credentials and no recipe API key. */
@RunWith(AndroidJUnit4.class)
public class OriginalJourneyTest {
  private void awaitScreen(int id) {
    long limit = SystemClock.elapsedRealtime() + 15_000;
    while (SystemClock.elapsedRealtime() < limit) {
      AtomicBoolean ready = new AtomicBoolean(false);
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                for (Activity activity :
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                  View view = activity.findViewById(id);
                  if (view != null && view.isShown() && activity.hasWindowFocus()) ready.set(true);
                }
              });
      if (ready.get()) return;
      SystemClock.sleep(50);
    }
    fail("Expected screen did not appear within 15 seconds: " + id);
  }

  private void screenshot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(500, 5000);
    Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(bitmap);
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File directory = new File(app.getExternalFilesDir(null), "original_screenshots");
    assertTrue(directory.isDirectory() || directory.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(directory, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }
  }

  private void awaitText(int id, String expected) {
    long until = SystemClock.elapsedRealtime() + 35000;
    while (SystemClock.elapsedRealtime() < until) {
      AtomicBoolean ready = new AtomicBoolean(false);
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                for (Activity activity :
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                  android.widget.TextView view = activity.findViewById(id);
                  if (view != null
                      && view.isShown()
                      && activity.hasWindowFocus()
                      && view.getText().toString().contains(expected)) ready.set(true);
                }
              });
      if (ready.get()) return;
      SystemClock.sleep(100);
    }
    fail("Expected real recipe response: " + expected);
  }

  @Test
  public void completeCoreFlowAndCaptureScreens() throws Exception {
    Assume.assumeTrue(
        "Run this test on a fresh Android emulator",
        Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    Assume.assumeTrue(
        "This check uses the public no-private-key recipe provider",
        BuildConfig.SPOONACULAR_API_KEY.isEmpty());
    for (String command :
        new String[] {
          "input keyevent KEYCODE_WAKEUP",
          "wm dismiss-keyguard",
          "settings put global window_animation_scale 0",
          "settings put global transition_animation_scale 0",
          "settings put global animator_duration_scale 0"
        }) {
      try (ParcelFileDescriptor descriptor =
              InstrumentationRegistry.getInstrumentation()
                  .getUiAutomation()
                  .executeShellCommand(command);
          ParcelFileDescriptor.AutoCloseInputStream input =
              new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
        byte[] buffer = new byte[256];
        while (input.read(buffer) != -1) {}
      }
    }
    String username = "portfolio" + System.nanoTime();
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    try (ActivityScenario<Welcome> welcome = ActivityScenario.launch(Welcome.class)) {
      awaitScreen(R.id.Gstrt);
      screenshot("welcome");
      onView(withId(R.id.Gstrt)).perform(click());
      awaitScreen(R.id.UsrReg);
      onView(withId(R.id.UsrReg)).perform(replaceText(username));
      onView(withId(R.id.editEmail)).perform(replaceText("demo@example.com"));
      onView(withId(R.id.editPassword)).perform(replaceText("demo-password"));
      onView(withId(R.id.editPassword2)).perform(replaceText("demo-password"), closeSoftKeyboard());
      onView(withId(R.id.RegisterButton1)).perform(click());
      awaitScreen(R.id.LoginButton);
      onView(withId(R.id.editText)).perform(replaceText(username));
      onView(withId(R.id.editPassword)).perform(replaceText("demo-password"), closeSoftKeyboard());
      onView(withId(R.id.LoginButton)).perform(click());
      awaitScreen(R.id.cardBMI);
      screenshot("home");
      onView(withId(R.id.cardBMI)).perform(click());
      awaitScreen(R.id.editTextWeight);
      onView(withId(R.id.editTextWeight)).perform(replaceText("70"));
      onView(withId(R.id.editTextHeight)).perform(replaceText("0"), closeSoftKeyboard());
      onView(withId(R.id.btnCalculate)).perform(click());
      onView(withId(R.id.textViewResult)).check(matches(withText(containsString("positive"))));
      onView(withId(R.id.editTextHeight)).perform(replaceText("175"), closeSoftKeyboard());
      onView(withId(R.id.btnCalculate)).perform(click());
      onView(withId(R.id.textViewResult)).check(matches(withText(containsString("22.9"))));
      screenshot("bmi");
      pressBack();
      awaitScreen(R.id.cardGym);
      onView(withId(R.id.cardGym)).perform(click());
      awaitScreen(R.id.mountain);
      screenshot("workouts");
      onView(withId(R.id.mountain)).perform(click());
      awaitScreen(R.id.startButton);
      screenshot("workout");
      onView(withId(R.id.startButton)).perform(click());
      onView(withId(R.id.startButton)).check(matches(withText("PAUSE")));
      onView(withId(R.id.startButton)).perform(click());
      onView(withId(R.id.startButton)).check(matches(withText("START")));
      pressBack();
      pressBack();
      awaitScreen(R.id.cardMeals);
      onView(withId(R.id.cardMeals)).perform(click());
      awaitText(R.id.recipe_status, "Recipe data & images: TheMealDB");
      screenshot("recipes");
      onView(withId(androidx.appcompat.R.id.search_src_text))
          .perform(
              replaceText("Arrabiata"),
              closeSoftKeyboard(),
              androidx.test.espresso.action.ViewActions.pressImeActionButton());
      awaitText(R.id.textview_title, "Spicy Arrabiata Penne");
      screenshot("recipes-search");
      onView(withText("Spicy Arrabiata Penne")).perform(click());
      awaitText(R.id.textView_meal_name, "Spicy Arrabiata Penne");
      onView(withId(R.id.recycler_meal_ingredients))
          .check(
              (view, error) -> {
                if (error != null) throw error;
                assertNotNull(((androidx.recyclerview.widget.RecyclerView) view).getAdapter());
                assertTrue(
                    ((androidx.recyclerview.widget.RecyclerView) view).getAdapter().getItemCount()
                        > 0);
              });
      onView(withId(R.id.recycler_meal_instructions))
          .check(
              (view, error) -> {
                if (error != null) throw error;
                assertNotNull(((androidx.recyclerview.widget.RecyclerView) view).getAdapter());
                assertTrue(
                    ((androidx.recyclerview.widget.RecyclerView) view).getAdapter().getItemCount()
                        > 0);
              });
      screenshot("recipe-details");
      pressBack();
      pressBack();
      awaitScreen(R.id.LogOut);
      onView(withId(R.id.LogOut)).perform(click());
      awaitScreen(R.id.Gstrt);
    } finally {
      // Remove only this test's synthetic account, never other app data.
      try (DatabaseHelper database = new DatabaseHelper(app)) {
        database
            .getWritableDatabase()
            .delete("user_table", "USERNAME = ?", new String[] {username});
      }
    }
  }
}
