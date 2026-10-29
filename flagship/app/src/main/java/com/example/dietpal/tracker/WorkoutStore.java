package com.example.dietpal.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WorkoutStore extends SQLiteOpenHelper {
  public WorkoutStore(Context context) {
    super(context.getApplicationContext(), "dietpal_workouts_v1.db", null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE workouts(id TEXT PRIMARY KEY,day TEXT NOT NULL,title TEXT NOT NULL,seconds"
            + " INTEGER NOT NULL,sets INTEGER,reps INTEGER,source TEXT NOT NULL,created INTEGER NOT"
            + " NULL)");
    db.execSQL("CREATE INDEX workout_day ON workouts(day)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new IllegalStateException("A non-destructive workout migration is required");
  }

  public void log(
      String day, String title, int seconds, Integer sets, Integer reps, String source) {
    LocalDate.parse(day);
    if (title.trim().isEmpty()
        || title.length() > 150
        || seconds < 0
        || seconds > 86400
        || (sets != null && (sets <= 0 || sets > 1000))
        || (reps != null && (reps <= 0 || reps > 100000)))
      throw new IllegalArgumentException(
          "Enter a workout name and valid non-negative duration or positive sets/reps.");
    if (seconds == 0 && sets == null && reps == null)
      throw new IllegalArgumentException("Enter a duration, sets or repetitions.");
    ContentValues v = new ContentValues();
    v.put("id", UUID.randomUUID().toString());
    v.put("day", day);
    v.put("title", title.trim());
    v.put("seconds", seconds);
    if (sets != null) v.put("sets", sets);
    if (reps != null) v.put("reps", reps);
    v.put("source", source);
    v.put("created", System.currentTimeMillis());
    getWritableDatabase().insertOrThrow("workouts", null, v);
  }

  public List<Entry> entries(String day) {
    List<Entry> values = new ArrayList<>();
    String where = day == null ? null : "day = ?";
    String[] args = day == null ? null : new String[] {day};
    try (Cursor c =
        getReadableDatabase().query("workouts", null, where, args, null, null, "created DESC")) {
      while (c.moveToNext()) {
        Entry e = new Entry();
        e.id = c.getString(c.getColumnIndexOrThrow("id"));
        e.day = c.getString(c.getColumnIndexOrThrow("day"));
        e.title = c.getString(c.getColumnIndexOrThrow("title"));
        e.seconds = c.getInt(c.getColumnIndexOrThrow("seconds"));
        int sets = c.getColumnIndexOrThrow("sets"), reps = c.getColumnIndexOrThrow("reps");
        e.sets = c.isNull(sets) ? null : c.getInt(sets);
        e.reps = c.isNull(reps) ? null : c.getInt(reps);
        e.source = c.getString(c.getColumnIndexOrThrow("source"));
        values.add(e);
      }
    }
    return values;
  }

  public void remove(String id) {
    getWritableDatabase().delete("workouts", "id = ?", new String[] {id});
  }

  public void clear() {
    getWritableDatabase().delete("workouts", null, null);
  }

  public static final class Entry {
    public String id, day, title, source;
    public int seconds;
    public Integer sets, reps;
  }
}
