package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

import android.app.NotificationManager;
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
import com.example.dietpal.tracker.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.droidsonroids.gif.*;

/** Native integration checks run only against the project's disposable emulator. */
@RunWith(AndroidJUnit4.class)
public class AuxiliaryJourneyTest {
  private Context app() {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    return InstrumentationRegistry.getInstrumentation().getTargetContext();
  }

  private void set(int id, String value) {
    onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard());
  }

  private void shot(String name) throws Exception {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    File dir = new File(app().getExternalFilesDir(null), "flagship_screenshots");
    assertTrue(dir.isDirectory() || dir.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    } finally {
      bitmap.recycle();
    }
  }

  @Test
  public void realStagingFoodLookupKeepsMissingTotalCarbsUnknown() throws Exception {
    app();
    CountDownLatch done = new CountDownLatch(1);
    AtomicReference<List<Food>> result = new AtomicReference<>();
    AtomicReference<String> error = new AtomicReference<>();
    FoodApi api = new FoodApi("https://world.openfoodfacts.net/", true);
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () ->
                api.search(
                    "3017624010701",
                    new FoodApi.Listener() {
                      public void loaded(List<Food> foods) {
                        result.set(foods);
                        done.countDown();
                      }

                      public void failed(String message) {
                        error.set(message);
                        done.countDown();
                      }
                    }));
    assertTrue("Live staging lookup timed out", done.await(30, TimeUnit.SECONDS));
    assertNull(error.get(), error.get());
    assertFalse(result.get().isEmpty());
    Food food = result.get().get(0);
    assertTrue(food.name.toLowerCase(Locale.ROOT).contains("nutella"));
    assertEquals("g", food.unit);
    assertNotNull(food.kcal);
    assertTrue(food.kcal > 500 && food.kcal < 600);
    assertNotNull(food.protein);
    // Staging is mutable: absent total carbs must not become an invented zero.
    if (food.fiber == null && food.availableCarbs != null) assertNull(food.carbs);
    InstrumentationRegistry.getInstrumentation().runOnMainSync(api::cancel);
  }

  @Test
  public void guidedGifTimerPausesSurvivesRecreationAndSavesOnce() throws Exception {
    Context context = app();
    String date = "2099-04-10";
    Set<String> before = new HashSet<>();
    try (WorkoutStore store = new WorkoutStore(context)) {
      for (WorkoutStore.Entry row : store.entries(date)) before.add(row.id);
      Intent intent =
          new Intent(context, GuidedExerciseActivity.class)
              .putExtra("exercise", 0)
              .putExtra("day", date);
      try (ActivityScenario<GuidedExerciseActivity> scenario = ActivityScenario.launch(intent)) {
        scenario.onActivity(
            activity -> {
              GifDrawable gif =
                  (GifDrawable)
                      ((GifImageView) activity.findViewById(R.id.guided_image)).getDrawable();
              assertTrue("Expected an actual animated guide", gif.getNumberOfFrames() > 1);
              assertTrue(gif.getDuration() > 0);
            });
        onView(withId(R.id.guided_duration)).perform(scrollTo(), click());
        onView(withId(R.id.guided_seconds)).perform(replaceText("5"), closeSoftKeyboard());
        onView(withText("Set")).perform(click());
        onView(withId(R.id.guided_start)).perform(scrollTo(), click());
        SystemClock.sleep(1200);
        onView(withId(R.id.guided_start)).perform(click());
        AtomicReference<String> paused = new AtomicReference<>();
        scenario.onActivity(
            activity ->
                paused.set(
                    ((TextView) activity.findViewById(R.id.guided_clock)).getText().toString()));
        scenario.recreate();
        SystemClock.sleep(1000);
        onView(withId(R.id.guided_clock)).check(matches(withText(paused.get())));
        onView(withId(R.id.guided_start)).perform(scrollTo(), click());
        SystemClock.sleep(5200);
        onView(withId(R.id.guided_save)).perform(scrollTo(), click());
        scenario.recreate();
        onView(withId(R.id.guided_save))
            .check(matches(withText("Saved to movement log")))
            .check(matches(isNotEnabled()));
        List<WorkoutStore.Entry> added = new ArrayList<>();
        for (WorkoutStore.Entry row : store.entries(date))
          if (!before.contains(row.id)) added.add(row);
        assertEquals(1, added.size());
        assertEquals(5, added.get(0).seconds);
        assertTrue(added.get(0).source.contains("user confirmed"));
        shot("workout-completed");
      } finally {
        for (WorkoutStore.Entry row : store.entries(date))
          if (!before.contains(row.id)) store.remove(row.id);
      }
    }
  }

  @Test
  public void optedInReminderPostsNativeNotificationAndCanBeDisabled() throws Exception {
    Context context = app();
    InstrumentationRegistry.getInstrumentation()
        .getUiAutomation()
        .grantRuntimePermission(
            context.getPackageName(), android.Manifest.permission.POST_NOTIFICATIONS);
    ReminderScheduler scheduler = new ReminderScheduler(context);
    SharedPreferences prefs =
        context.getSharedPreferences("dietpal_reminders", Context.MODE_PRIVATE);
    Map<String, ?> previous = new HashMap<>(prefs.getAll());
    NotificationManager manager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assertEquals(
        "Test emulator must have no pre-existing DietPal notifications",
        0,
        manager.getActiveNotifications().length);
    try (ActivityScenario<ReminderSettingsActivity> scenario =
        ActivityScenario.launch(ReminderSettingsActivity.class)) {
      assertTrue(scheduler.notificationsAllowed());
      scheduler.set("water", false, 12, 0);
      scenario.recreate();
      onView(withText("Water")).perform(scrollTo(), click());
      assertTrue(scheduler.enabled("water"));
      scenario.recreate();
      assertTrue(scheduler.enabled("water"));
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () ->
                  new ReminderReceiver()
                      .onReceive(
                          context,
                          new Intent("com.example.dietpal.REMINDER").putExtra("type", "water")));
      long until = SystemClock.elapsedRealtime() + 5000;
      while (manager.getActiveNotifications().length == 0 && SystemClock.elapsedRealtime() < until)
        SystemClock.sleep(100);
      assertEquals(1, manager.getActiveNotifications().length);
      assertEquals(
          "A moment for water",
          manager
              .getActiveNotifications()[0]
              .getNotification()
              .extras
              .getString(android.app.Notification.EXTRA_TITLE));
      assertNotNull(manager.getActiveNotifications()[0].getNotification().contentIntent);
      onView(withText("Water")).perform(scrollTo(), click());
      assertFalse(scheduler.enabled("water"));
      shot("reminders");
    } finally {
      scheduler.clear();
      SharedPreferences.Editor edit = prefs.edit().clear();
      for (Map.Entry<String, ?> entry : previous.entrySet()) {
        if (entry.getValue() instanceof Boolean)
          edit.putBoolean(entry.getKey(), (Boolean) entry.getValue());
        else if (entry.getValue() instanceof Integer)
          edit.putInt(entry.getKey(), (Integer) entry.getValue());
      }
      edit.commit();
      scheduler.refresh();
    }
  }

  @Test
  public void foodEntryDraftSurvivesRotationBeforeExplicitSave() throws Exception {
    Context context = app();
    String date = "2099-04-11";
    Set<String> before = new HashSet<>();
    try (TrackerStore store = new TrackerStore(context)) {
      for (TrackerStore.Entry row : store.entries(date)) before.add(row.id);
      Intent intent = new Intent(context, FoodLogActivity.class).putExtra("day", date);
      try (ActivityScenario<FoodLogActivity> scenario = ActivityScenario.launch(intent)) {
        onView(withId(R.id.food_manual)).perform(scrollTo(), click());
        set(R.id.food_name, "Rotation draft");
        set(R.id.food_amount, "150");
        set(R.id.food_kcal, "200");
        set(R.id.food_protein, "10");
        set(R.id.food_carbs, "20");
        set(R.id.food_fat, "8");
        scenario.recreate();
        onView(withId(R.id.food_name))
            .perform(scrollTo())
            .check(matches(withText("Rotation draft")));
        onView(withId(R.id.food_amount)).perform(scrollTo()).check(matches(withText("150")));
        assertEquals(before.size(), store.entries(date).size());
        onView(withId(R.id.food_save)).perform(click());
        assertEquals(before.size() + 1, store.entries(date).size());
        TrackerStore.Totals totals = new TrackerStore.Totals(store.entries(date));
        assertTrue(totals.kcal >= 300);
      } finally {
        for (TrackerStore.Entry row : store.entries(date))
          if (!before.contains(row.id)) store.remove(row.id);
      }
    }
  }
}
