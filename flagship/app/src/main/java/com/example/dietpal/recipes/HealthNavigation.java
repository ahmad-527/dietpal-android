package com.example.dietpal.recipes;

import android.app.Activity;
import android.content.res.ColorStateList;
import com.example.dietpal.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import java.util.function.IntConsumer;

/** The same destinations and selected state on every top-level screen. */
public final class HealthNavigation {
  private HealthNavigation() {}

  public static BottomNavigationView create(Activity a, int selected, IntConsumer destination) {
    BottomNavigationView nav = new BottomNavigationView(a);
    nav.setId(R.id.flagship_navigation);
    nav.setBackgroundColor(RecipeUi.WHITE);
    int[] icons = {
      R.drawable.health_home,
      R.drawable.health_recipes,
      R.drawable.health_calendar,
      R.drawable.health_chart,
      R.drawable.health_profile
    };
    String[] labels = {"Today", "Recipes", "Plan", "Progress", "Profile"};
    for (int i = 0; i < labels.length; i++)
      nav.getMenu().add(0, i + 1, i, labels[i]).setIcon(icons[i]);
    ColorStateList colors =
        new ColorStateList(
            new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
            new int[] {RecipeUi.BLUE, RecipeUi.MUTED});
    nav.setItemIconTintList(colors);
    nav.setItemTextColor(colors);
    nav.setItemActiveIndicatorColor(ColorStateList.valueOf(RecipeUi.SOFT));
    nav.setElevation(0);
    nav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
    nav.setSelectedItemId(selected);
    nav.setOnItemSelectedListener(
        item -> {
          if (item.getItemId() != selected) destination.accept(item.getItemId());
          return item.getItemId() == selected;
        });
    return nav;
  }
}
