package com.example.dietpal;

import static org.junit.Assert.*;

import com.example.dietpal.planning.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PlanningPersistenceTest {
  private PlannerProfile profile(String date, String protein) {
    PlannerProfile p = new PlannerProfile();
    p.effectiveFrom = date;
    p.setGoals(
        new Goal[] {
          Goal.parse(">2000", true),
          Goal.parse(protein, true),
          Goal.parse("100.25-150.75", true),
          Goal.parse("60", true)
        });
    return p;
  }

  @Test
  public void futureGoalChangesCannotRewriteEarlierTargetVersions() {
    try (PlanningStore store = new PlanningStore(RuntimeEnvironment.getApplication())) {
      store.clear();
      store.saveTargets(profile("2026-10-29", "180"));
      store.saveTargets(profile("2026-11-05", "200"));
      assertNull(store.targetsAt("2026-10-28"));
      assertEquals("180", store.targetsAt("2026-11-04").goals()[1].raw());
      assertEquals("200", store.targetsAt("2026-11-05").goals()[1].raw());
      assertEquals(">2000", store.targetsAt("2026-11-06").goals()[0].raw());
      assertEquals("100.25-150.75", store.targetsAt("2026-11-06").goals()[2].raw());
      store.clear();
    }
  }

  @Test
  public void hiddenRecipePreferencesSurviveReopenAndAreExported() {
    try (PlanningStore store = new PlanningStore(RuntimeEnvironment.getApplication())) {
      store.clear();
      store.hideRecipe("balanced-berry-yogurt-oats", true);
    }
    try (PlanningStore reopened = new PlanningStore(RuntimeEnvironment.getApplication())) {
      assertTrue(reopened.hiddenRecipes().contains("balanced-berry-yogurt-oats"));
      assertTrue(reopened.export().containsKey("preferences"));
      reopened.hideRecipe("balanced-berry-yogurt-oats", false);
      assertTrue(reopened.hiddenRecipes().isEmpty());
      reopened.clear();
    }
  }
}
