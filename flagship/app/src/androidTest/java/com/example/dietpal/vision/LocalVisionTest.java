package com.example.dietpal.vision;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

import android.content.*;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.dietpal.R;
import com.example.dietpal.tracker.*;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Real pinned-model inference; requires separately provisioned private model and food fixture. */
@RunWith(AndroidJUnit4.class)
public class LocalVisionTest {
  @Test
  public void photoScreenPreparesRealImageAndReviewsBeforeLogging() throws Exception {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File source = new File(app.getCacheDir(), "vision-ui-fixture.jpg");
    assertTrue(source.isFile());
    File folder = new File(app.getCacheDir(), "meal_photos");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    File selected = new File(folder, "meal-test-input.jpg");
    Files.copy(
        source.toPath(), selected.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    android.net.Uri uri =
        androidx.core.content.FileProvider.getUriForFile(
            app, app.getPackageName() + ".files", selected);
    String date = "2099-04-14";
    Set<String> before = new HashSet<>();
    try (TrackerStore store = new TrackerStore(app)) {
      for (TrackerStore.Entry row : store.entries(date)) before.add(row.id);
      try (ActivityScenario<PhotoActivity> scenario =
          ActivityScenario.launch(new Intent(app, PhotoActivity.class).putExtra("day", date))) {
        scenario.onActivity(
            a ->
                a.onActivityResult(101, android.app.Activity.RESULT_OK, new Intent().setData(uri)));
        long until = SystemClock.elapsedRealtime() + 10000;
        java.util.concurrent.atomic.AtomicBoolean ready =
            new java.util.concurrent.atomic.AtomicBoolean();
        while (!ready.get() && SystemClock.elapsedRealtime() < until) {
          scenario.onActivity(a -> ready.set(a.findViewById(R.id.photo_analyze).isEnabled()));
          SystemClock.sleep(100);
        }
        assertTrue("Real image preparation did not finish", ready.get());
        onView(withId(R.id.photo_analyze)).perform(scrollTo(), click());
        until = SystemClock.elapsedRealtime() + 180000;
        ready.set(false);
        java.util.concurrent.atomic.AtomicReference<String> status =
            new java.util.concurrent.atomic.AtomicReference<>();
        while (!ready.get() && SystemClock.elapsedRealtime() < until) {
          scenario.onActivity(
              a -> {
                status.set(
                    ((android.widget.TextView) a.findViewById(R.id.photo_status))
                        .getText()
                        .toString());
                ready.set(a.findViewById(R.id.photo_review_first) != null);
              });
          if (status.get().startsWith("Local analysis could not")
              || status.get().startsWith("No useful")) break;
          SystemClock.sleep(500);
        }
        assertTrue(status.get(), ready.get());
        assertEquals(before.size(), store.entries(date).size());
        onView(withId(R.id.photo_review_first)).perform(scrollTo(), click());
        onView(withId(R.id.food_amount))
            .perform(scrollTo(), replaceText("150"), closeSoftKeyboard());
        onView(withId(R.id.food_kcal)).perform(scrollTo(), replaceText("200"), closeSoftKeyboard());
        onView(withId(R.id.food_save)).perform(click());
        assertEquals(before.size() + 1, store.entries(date).size());
        for (TrackerStore.Entry row : store.entries(date))
          if (!before.contains(row.id))
            assertTrue(row.food.source.contains("On-device AI estimate"));
      } finally {
        for (TrackerStore.Entry row : store.entries(date))
          if (!before.contains(row.id)) store.remove(row.id);
        selected.delete();
        source.delete();
      }
    }
  }

  @Test
  public void realOfflineModelEstimateRequiresEditableExplicitLogging() throws Exception {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
    LocalModel model = new LocalModel(app);
    assertTrue("Provision the pinned model before this integration test", model.installed());
    assertEquals("", model.supportMessage());
    File photo = new File(app.getCacheDir(), "vision-test-meal.jpg");
    assertTrue("Provision a real meal-photo fixture", photo.isFile());
    File proof = new File(app.getExternalFilesDir(null), "local-vision-proof.json");
    Map<String, Object> receipt = new LinkedHashMap<>();
    receipt.put("modelSha256", LocalModel.SHA256);
    receipt.put("abi", Arrays.asList(Build.SUPPORTED_ABIS));
    long start = SystemClock.elapsedRealtime();
    List<MealEstimate> estimates;
    try {
      estimates = new PhotoAnalyzer().analyze(app, Files.readAllBytes(photo.toPath()));
      receipt.put("elapsedMs", SystemClock.elapsedRealtime() - start);
      receipt.put("estimates", estimates);
      receipt.put("inference", "completed");
    } catch (Throwable failure) {
      receipt.put("elapsedMs", SystemClock.elapsedRealtime() - start);
      StringWriter trace = new StringWriter();
      failure.printStackTrace(new PrintWriter(trace));
      receipt.put("failure", trace.toString());
      Files.write(proof.toPath(), new Gson().toJson(receipt).getBytes(StandardCharsets.UTF_8));
      throw failure;
    }
    assertFalse("Real food fixture yielded no usable estimate", estimates.isEmpty());
    assertTrue(estimates.size() <= 6);
    String date = "2099-04-12";
    Set<String> before = new HashSet<>();
    try (TrackerStore store = new TrackerStore(app)) {
      for (TrackerStore.Entry row : store.entries(date)) before.add(row.id);
      MealEstimate first = estimates.get(0);
      first.food.validate(first.grams);
      Intent intent =
          new Intent(app, FoodLogActivity.class)
              .putExtra("day", date)
              .putExtra("food", new Gson().toJson(first.food))
              .putExtra("amount", first.grams);
      try (ActivityScenario<FoodLogActivity> scenario = ActivityScenario.launch(intent)) {
        assertEquals(
            "Inference alone must not enter the diary", before.size(), store.entries(date).size());
        onView(withId(R.id.food_name))
            .perform(scrollTo(), replaceText("Reviewed meal-photo estimate"), closeSoftKeyboard());
        onView(withId(R.id.food_amount))
            .perform(scrollTo(), replaceText("150"), closeSoftKeyboard());
        onView(withId(R.id.food_kcal)).perform(scrollTo(), replaceText("200"), closeSoftKeyboard());
        scenario.recreate();
        onView(withId(R.id.food_save)).perform(click());
        assertEquals(before.size() + 1, store.entries(date).size());
        for (TrackerStore.Entry row : store.entries(date))
          if (!before.contains(row.id)) {
            assertEquals("Reviewed meal-photo estimate", row.food.name);
            assertEquals(300.0, row.food.forAmount(row.food.kcal, row.amount), 0.001);
            assertTrue(row.food.source.contains("On-device AI estimate"));
          }
        receipt.put("review", "edited, recreated, explicitly logged once");
      } finally {
        for (TrackerStore.Entry row : store.entries(date))
          if (!before.contains(row.id)) store.remove(row.id);
      }
    } finally {
      photo.delete();
    }
    Files.write(proof.toPath(), new Gson().toJson(receipt).getBytes(StandardCharsets.UTF_8));
  }
}
