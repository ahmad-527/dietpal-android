package com.example.dietpal.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import com.example.dietpal.recipes.RecipeUi;

/** Exact values are exposed by adjacent native text; the ring is decorative. */
public final class NutritionRing extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final double progress;

  public NutritionRing(Context context, double progress) {
    super(context);
    this.progress = Math.max(0, Math.min(1, progress));
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float stroke = 10 * getResources().getDisplayMetrics().density;
    float size = Math.min(getWidth(), getHeight()) - stroke * 2;
    float left = (getWidth() - size) / 2, top = (getHeight() - size) / 2;
    RectF bounds = new RectF(left, top, left + size, top + size);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(stroke);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setColor(RecipeUi.SOFT);
    canvas.drawArc(bounds, 0, 360, false, paint);
    paint.setColor(RecipeUi.BLUE);
    canvas.drawArc(bounds, -90, (float) (progress * 360), false, paint);
  }
}
