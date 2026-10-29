package com.example.dietpal.planning;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Dated snapshots. Replacing next week cannot rewrite any date outside that week. */
public final class PlanningStore extends SQLiteOpenHelper {
  private final Gson gson = new Gson();
  private final SharedPreferences preferences;

  public PlanningStore(Context context) {
    super(context.getApplicationContext(), "dietpal_plans_v1.db", null, 1);
    preferences =
        context
            .getApplicationContext()
            .getSharedPreferences("dietpal_planner", Context.MODE_PRIVATE);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE days(day TEXT PRIMARY KEY, data TEXT NOT NULL)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int old, int version) {
    throw new IllegalStateException("A non-destructive planning migration is required");
  }

  public PlannedDay day(String day) {
    LocalDate.parse(day);
    try (Cursor c =
        getReadableDatabase()
            .query(
                "days", new String[] {"data"}, "day = ?", new String[] {day}, null, null, null)) {
      return c.moveToFirst() ? gson.fromJson(c.getString(0), PlannedDay.class) : null;
    }
  }

  public List<PlannedDay> all() {
    List<PlannedDay> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query("days", new String[] {"data"}, null, null, null, null, "day ASC")) {
      while (c.moveToNext()) out.add(gson.fromJson(c.getString(0), PlannedDay.class));
    }
    return out;
  }

  public void save(List<PlannedDay> days) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (PlannedDay day : days) {
        LocalDate.parse(day.day);
        day.profile.validate();
        ContentValues v = new ContentValues();
        v.put("day", day.day);
        v.put("data", gson.toJson(day));
        if (db.insertWithOnConflict("days", null, v, SQLiteDatabase.CONFLICT_REPLACE) < 0)
          throw new android.database.sqlite.SQLiteException("Could not save planned date");
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    if (!days.isEmpty())
      preferences.edit().putString("lastProfile", gson.toJson(days.get(0).profile)).apply();
  }

  public PlannerProfile lastProfile() {
    String json = preferences.getString("lastProfile", null);
    return json == null ? null : gson.fromJson(json, PlannerProfile.class);
  }

  public java.util.Set<String> hiddenRecipes() {
    return new java.util.HashSet<>(
        preferences.getStringSet("hiddenRecipes", java.util.Collections.emptySet()));
  }

  public void hideRecipe(String id, boolean hidden) {
    java.util.Set<String> ids = hiddenRecipes();
    if (hidden) ids.add(id);
    else ids.remove(id);
    preferences.edit().putStringSet("hiddenRecipes", ids).apply();
  }

  public java.util.Map<String, Object> export() {
    java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
    out.put("datedPlans", all());
    out.put("preferences", preferences.getAll());
    return out;
  }

  public PlannerProfile targetsAt(String date) {
    LocalDate.parse(date);
    java.util.Map<String, PlannerProfile> timeline = targetTimeline();
    String latest = null;
    for (String key : timeline.keySet())
      if (key.compareTo(date) <= 0 && (latest == null || key.compareTo(latest) > 0)) latest = key;
    return latest == null ? null : timeline.get(latest);
  }

  public void saveTargets(PlannerProfile profile) {
    LocalDate.parse(profile.effectiveFrom);
    for (Goal goal : profile.goals()) goal.validate(true);
    java.util.Map<String, PlannerProfile> timeline = targetTimeline();
    timeline.put(profile.effectiveFrom, profile);
    preferences.edit().putString("targetTimeline", gson.toJson(timeline)).apply();
  }

  private java.util.Map<String, PlannerProfile> targetTimeline() {
    java.util.Map<String, PlannerProfile> values =
        gson.fromJson(
            preferences.getString("targetTimeline", "{}"),
            new com.google.gson.reflect.TypeToken<
                java.util.LinkedHashMap<String, PlannerProfile>>() {}.getType());
    return values == null ? new java.util.LinkedHashMap<>() : values;
  }

  public void remove(String day) {
    getWritableDatabase().delete("days", "day = ?", new String[] {day});
  }

  public void clear() {
    getWritableDatabase().delete("days", null, null);
    preferences.edit().clear().apply();
  }
}
