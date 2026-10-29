package com.example.dietpal.recipes;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import com.example.dietpal.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/** Small shared view vocabulary for the flagship's native Android screens. */
public final class RecipeUi {
  public static final int INK = 0xFF182333, MUTED = 0xFF58677B;
  public static final int PAPER = 0xFFF3F5F9, GREEN = 0xFF245FD5;
  public static final int SOFT = 0xFFEAF0FC, WHITE = Color.WHITE;
  public static final int BLUE = GREEN,
      LINE = 0xFFE4E9F1,
      PROTEIN = 0xFF7956BC,
      CARBS = 0xFFB65D1C,
      FAT = 0xFF187D70;

  private RecipeUi() {}

  public static int dp(Activity a, int value) {
    return Math.round(value * a.getResources().getDisplayMetrics().density);
  }

  public static LinearLayout column(Activity a) {
    LinearLayout v = new LinearLayout(a);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  public static LinearLayout row(Activity a) {
    LinearLayout v = new LinearLayout(a);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  public static LinearLayout root(Activity a) {
    a.getWindow().setStatusBarColor(PAPER);
    a.getWindow().setNavigationBarColor(WHITE);
    a.getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    LinearLayout root = column(a);
    root.setBackgroundColor(PAPER);
    a.setContentView(root);
    return root;
  }

  public static TextView text(Activity a, String text, int size, boolean heading) {
    TextView v = new TextView(a);
    v.setText(text);
    v.setTextSize(size);
    v.setTextColor(heading ? INK : MUTED);
    v.setIncludeFontPadding(false);
    v.setTypeface(Typeface.create(heading ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
    if (heading) {
      androidx.core.view.ViewCompat.setAccessibilityHeading(v, true);
    }
    v.setLineSpacing(dp(a, 2), 1f);
    return v;
  }

  public static MaterialButton button(Activity a, String text, boolean primary) {
    MaterialButton b = new MaterialButton(a);
    b.setText(text);
    b.setAllCaps(false);
    b.setTextSize(14);
    b.setLetterSpacing(0);
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setTextColor(primary ? WHITE : GREEN);
    b.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : SOFT));
    b.setCornerRadius(dp(a, 12));
    b.setMinHeight(dp(a, 48));
    b.setMinimumHeight(dp(a, 48));
    b.setInsetTop(dp(a, 4));
    b.setInsetBottom(dp(a, 4));
    b.setElevation(0);
    b.setStateListAnimator(null);
    b.setPadding(dp(a, 14), dp(a, 8), dp(a, 14), dp(a, 8));
    return b;
  }

  public static LinearLayout header(Activity a, LinearLayout root, String title) {
    LinearLayout bar = row(a);
    bar.setPadding(dp(a, 4), dp(a, 4), dp(a, 12), dp(a, 4));
    ImageButton back = new ImageButton(a);
    back.setImageResource(R.drawable.health_back);
    back.setContentDescription("Go back");
    back.setBackgroundColor(Color.TRANSPARENT);
    back.setOnClickListener(v -> a.finish());
    bar.addView(back, new LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)));
    TextView name = text(a, title, 23, true);
    bar.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    root.addView(bar);
    return bar;
  }

  public static LinearLayout scroll(Activity a, LinearLayout root) {
    NestedScrollView scroll = new NestedScrollView(a);
    scroll.setFillViewport(true);
    LinearLayout body = column(a);
    body.setPadding(dp(a, 16), dp(a, 8), dp(a, 16), dp(a, 24));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    return body;
  }

  public static MaterialCardView card(Activity a, LinearLayout parent) {
    MaterialCardView card = new MaterialCardView(a);
    card.setCardBackgroundColor(WHITE);
    card.setRadius(dp(a, 22));
    card.setCardElevation(0);
    card.setStrokeWidth(0);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(a, 12);
    parent.addView(card, p);
    return card;
  }

  public static LinearLayout cardBody(Activity a, LinearLayout parent) {
    MaterialCardView card = card(a, parent);
    LinearLayout body = column(a);
    int p = dp(a, 16);
    body.setPadding(p, p, p, p);
    card.addView(body);
    return body;
  }

  public static void gap(Activity a, LinearLayout parent, int height) {
    View v = new View(a);
    parent.addView(v, new LinearLayout.LayoutParams(1, dp(a, height)));
  }

  public static boolean compact(Activity a) {
    return a.getResources().getConfiguration().screenWidthDp < 360
        || a.getResources().getConfiguration().fontScale >= 1.3f;
  }

  public static TextView value(Activity a, String value, int size, int color) {
    TextView v = text(a, value, size, true);
    v.setTextColor(color);
    androidx.core.view.ViewCompat.setAccessibilityHeading(v, false);
    return v;
  }

  public static android.graphics.drawable.GradientDrawable surface(
      Activity a, int color, int radius) {
    android.graphics.drawable.GradientDrawable s = new android.graphics.drawable.GradientDrawable();
    s.setColor(color);
    s.setCornerRadius(dp(a, radius));
    return s;
  }

  public static ImageButton iconButton(Activity a, int icon, String label, Runnable action) {
    ImageButton v = new ImageButton(a);
    v.setImageResource(icon);
    v.setImageTintList(ColorStateList.valueOf(BLUE));
    v.setContentDescription(label);
    v.setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12));
    android.util.TypedValue ripple = new android.util.TypedValue();
    a.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true);
    v.setBackgroundResource(ripple.resourceId);
    v.setOnClickListener(view -> action.run());
    v.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)));
    return v;
  }

  public static LinearLayout.LayoutParams weighted(Activity a) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
    p.setMargins(dp(a, 3), 0, dp(a, 3), 0);
    return p;
  }

  public static void divider(Activity a, LinearLayout parent) {
    View line = new View(a);
    line.setBackgroundColor(LINE);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(a, 1));
    p.topMargin = dp(a, 10);
    p.bottomMargin = dp(a, 10);
    parent.addView(line, p);
  }

  public static void section(Activity a, LinearLayout parent, String title) {
    TextView v = text(a, title, 19, true);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.topMargin = dp(a, 8);
    p.bottomMargin = dp(a, 12);
    parent.addView(v, p);
  }

  public static View setting(
      Activity a, LinearLayout parent, int icon, String title, String subtitle, Runnable action) {
    LinearLayout row = row(a);
    row.setMinimumHeight(dp(a, 64));
    android.widget.ImageView image = new android.widget.ImageView(a);
    image.setImageResource(icon);
    image.setImageTintList(ColorStateList.valueOf(BLUE));
    image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    row.addView(image, new LinearLayout.LayoutParams(dp(a, 24), dp(a, 24)));
    LinearLayout labels = column(a);
    labels.setPadding(dp(a, 14), dp(a, 10), dp(a, 8), dp(a, 10));
    labels.addView(text(a, title, 16, true));
    if (!subtitle.isEmpty()) {
      gap(a, labels, 4);
      labels.addView(text(a, subtitle, 12, false));
    }
    row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    android.widget.ImageView arrow = new android.widget.ImageView(a);
    arrow.setImageResource(R.drawable.health_next);
    arrow.setImageTintList(ColorStateList.valueOf(MUTED));
    arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    row.addView(arrow, new LinearLayout.LayoutParams(dp(a, 20), dp(a, 20)));
    row.setBackgroundResource(android.R.drawable.list_selector_background);
    row.setFocusable(true);
    row.setOnClickListener(v -> action.run());
    parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    return row;
  }
}
