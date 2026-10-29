package com.example.dietpal.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import com.example.dietpal.recipes.RecipeUi;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/** Accessible companion view; exact daily values are also listed as text. */
public final class WeeklyChart extends View {
  private final double[] values;
  private final LocalDate start;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public WeeklyChart(Context c, LocalDate start, double[] values) {
    super(c);
    this.start = start;
    this.values = values;
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float density = getResources().getDisplayMetrics().density,
        pad = 20 * density,
        bottom = getHeight() - 30 * density;
    double max = 1;
    for (double value : values) max = Math.max(max, value);
    float step = (getWidth() - 2 * pad) / 7f;
    for (int i = 0; i < 7; i++) {
      float x = pad + i * step, height = (float) (values[i] / max) * (bottom - 20 * density);
      paint.setColor(RecipeUi.SOFT);
      canvas.drawRoundRect(
          x + 5 * density,
          10 * density,
          x + step - 5 * density,
          bottom,
          8 * density,
          8 * density,
          paint);
      paint.setColor(RecipeUi.GREEN);
      if (height > 0)
        canvas.drawRoundRect(
            x + 5 * density,
            bottom - height,
            x + step - 5 * density,
            bottom,
            8 * density,
            8 * density,
            paint);
      paint.setColor(RecipeUi.MUTED);
      paint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
      paint.setTextAlign(Paint.Align.CENTER);
      String name =
          start.plusDays(i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
      canvas.drawText(
          name.substring(0, Math.min(2, name.length())),
          x + step / 2,
          bottom + 21 * density,
          paint);
    }
  }
}
