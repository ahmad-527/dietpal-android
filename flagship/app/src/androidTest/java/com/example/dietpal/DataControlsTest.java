package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.dietpal.planning.*;
import com.example.dietpal.recipes.*;
import com.example.dietpal.tracker.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DataControlsTest {
  @Test
  public void exportedBytesContainAllRecordGroupsAndCancelPreservesThem() throws Exception {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    String day = "2099-04-13";
    File folder = new File(app.getCacheDir(), "meal_photos");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    File file = new File(folder, "export-test.json");
    if (file.exists()) assertTrue(file.delete());
    Uri output = FileProvider.getUriForFile(app, app.getPackageName() + ".files", file);
    try (TrackerStore store = new TrackerStore(app)) {
      Food food = new Food();
      food.name = "Export fixture";
      food.kcal = 100d;
      food.protein = 10d;
      food.carbs = 10d;
      food.fat = 2d;
      TrackerStore.Entry entry = store.add(day, "Lunch", food, 150);
      try (ActivityScenario<FlagshipActivity> scenario =
          ActivityScenario.launch(FlagshipActivity.class)) {
        scenario.onActivity(
            a ->
                a.onActivityResult(
                    401, android.app.Activity.RESULT_OK, new Intent().setData(output)));
        long until = SystemClock.elapsedRealtime() + 10000;
        JsonObject json = null;
        while (SystemClock.elapsedRealtime() < until) {
          if (file.isFile())
            try {
              json =
                  JsonParser.parseString(
                          new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                      .getAsJsonObject();
              break;
            } catch (RuntimeException partial) {
            }
          SystemClock.sleep(100);
        }
        assertNotNull("No complete JSON file was written", json);
        for (String key :
            new String[] {
              "diary",
              "goals",
              "weightsKg",
              "waterMl",
              "recentFoods",
              "workouts",
              "nutritionBackedPlans",
              "reminders",
              "recipesAndPlanning"
            }) assertTrue(key, json.has(key));
        assertTrue(json.toString().contains("Export fixture"));
        onView(allOf(withText("Profile"), isDisplayed())).perform(click());
        onView(withId(R.id.profile_delete)).perform(scrollTo(), click());
        onView(withText("Cancel")).perform(click());
        assertTrue(store.entries(day).stream().anyMatch(e -> e.id.equals(entry.id)));
        onView(withText("Open-source notices")).perform(scrollTo(), click());
        onView(withText("Open-source notices"))
            .check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
      } finally {
        store.remove(entry.id);
        file.delete();
      }
    }
  }
}
