package com.example.dietpal;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;

import android.os.Build;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OriginalInformationTest {
  @Test
  public void informationRoutesHaveUsefulContent() {
    Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"));
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a ->
              a.getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new SettingsFragment())
                  .commitNow());
      onView(withText(containsString("Recipe connection: TheMealDB")))
          .check(matches(isDisplayed()));
      onView(withText("Android app settings")).perform(scrollTo()).check(matches(isDisplayed()));
      scenario.onActivity(
          a ->
              a.getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new AboutUsFragment())
                  .commitNow());
      onView(withText("About DietPal Original")).check(matches(isDisplayed()));
      scenario.onActivity(
          a ->
              a.getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new ContactUsFragment())
                  .commitNow());
      onView(withText("Open project on GitHub")).perform(scrollTo()).check(matches(isDisplayed()));
      scenario.onActivity(
          a ->
              a.getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new PrivacyPolicyFragment())
                  .commitNow());
      onView(withText("Privacy")).check(matches(isDisplayed()));
      onView(withText(containsString("salted password hashes"))).check(matches(isDisplayed()));
      scenario.recreate();
      onView(withText("Privacy")).check(matches(isDisplayed()));
    }
  }
}
