package com.example.dietpal.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** On-device diary. Historical entries snapshot their original food/label values. */
public final class TrackerStore extends SQLiteOpenHelper {
  public static final String[] SLOTS = {"Breakfast", "Lunch", "Dinner", "Snacks"};
  private final Gson gson = new Gson();
  private final SharedPreferences preferences;

  public TrackerStore(Context context) {
    super(context.getApplicationContext(), "dietpal_tracker_v1.db", null, 1);
    preferences =
        context.getApplicationContext().getSharedPreferences("dietpal_goals", Context.MODE_PRIVATE);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE entries (id TEXT PRIMARY KEY, day TEXT NOT NULL, slot TEXT NOT NULL, food"
            + " TEXT NOT NULL, amount REAL NOT NULL, created INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX entries_day ON entries(day)");
    db.execSQL(
        "CREATE TABLE foods (name TEXT PRIMARY KEY, data TEXT NOT NULL, created INTEGER NOT NULL)");
    db.execSQL("CREATE TABLE water (day TEXT PRIMARY KEY, ml INTEGER NOT NULL)");
    db.execSQL("CREATE TABLE weights (day TEXT PRIMARY KEY, kg REAL NOT NULL)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new IllegalStateException("A non-destructive tracker migration is required");
  }

  private void day(String value) {
    LocalDate.parse(value);
  }

  public Entry add(String date, String slot, Food food, double amount) {
    day(date);
    if (!java.util.Arrays.asList(SLOTS).contains(slot))
      throw new IllegalArgumentException("Choose a meal group.");
    food.validate(amount);
    Entry entry = new Entry();
    entry.id = UUID.randomUUID().toString();
    entry.day = date;
    entry.slot = slot;
    entry.food = gson.fromJson(gson.toJson(food), Food.class);
    entry.amount = amount;
    entry.created = System.currentTimeMillis();
    restore(entry);
    ContentValues values = new ContentValues();
    values.put("name", food.name);
    values.put("data", gson.toJson(food));
    values.put("created", entry.created);
    getWritableDatabase()
        .insertWithOnConflict("foods", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    return entry;
  }

  public void restore(Entry entry) {
    ContentValues v = new ContentValues();
    v.put("id", entry.id);
    v.put("day", entry.day);
    v.put("slot", entry.slot);
    v.put("food", gson.toJson(entry.food));
    v.put("amount", entry.amount);
    v.put("created", entry.created);
    getWritableDatabase().insertOrThrow("entries", null, v);
  }

  public void remove(String id) {
    getWritableDatabase().delete("entries", "id = ?", new String[] {id});
  }

  public void amount(Entry entry, double amount) {
    entry.food.validate(amount);
    ContentValues values = new ContentValues();
    values.put("amount", amount);
    getWritableDatabase().update("entries", values, "id = ?", new String[] {entry.id});
  }

  public List<Entry> entries(String date) {
    day(date);
    List<Entry> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query("entries", null, "day = ?", new String[] {date}, null, null, "created ASC")) {
      while (c.moveToNext()) result.add(read(c));
    }
    return result;
  }

  private Entry read(Cursor c) {
    Entry e = new Entry();
    e.id = c.getString(c.getColumnIndexOrThrow("id"));
    e.day = c.getString(c.getColumnIndexOrThrow("day"));
    e.slot = c.getString(c.getColumnIndexOrThrow("slot"));
    e.food = gson.fromJson(c.getString(c.getColumnIndexOrThrow("food")), Food.class);
    e.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
    e.created = c.getLong(c.getColumnIndexOrThrow("created"));
    return e;
  }

  public List<Food> recent() {
    List<Food> values = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query("foods", new String[] {"data"}, null, null, null, null, "created DESC", "30")) {
      while (c.moveToNext()) values.add(gson.fromJson(c.getString(0), Food.class));
    }
    return values;
  }

  public int water(String date) {
    day(date);
    try (Cursor c =
        getReadableDatabase()
            .query(
                "water", new String[] {"ml"}, "day = ?", new String[] {date}, null, null, null)) {
      return c.moveToFirst() ? c.getInt(0) : 0;
    }
  }

  public void changeWater(String date, int change) {
    int amount = Math.max(0, Math.min(100000, water(date) + change));
    ContentValues v = new ContentValues();
    v.put("day", date);
    v.put("ml", amount);
    getWritableDatabase().insertWithOnConflict("water", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public void weight(String date, double kg) {
    day(date);
    if (!Double.isFinite(kg) || kg <= 0 || kg > 1000)
      throw new IllegalArgumentException("Enter a positive weight in kilograms.");
    ContentValues v = new ContentValues();
    v.put("day", date);
    v.put("kg", kg);
    getWritableDatabase().insertWithOnConflict("weights", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public Map<String, Double> weights() {
    Map<String, Double> values = new LinkedHashMap<>();
    try (Cursor c =
        getReadableDatabase()
            .query("weights", new String[] {"day", "kg"}, null, null, null, null, "day ASC")) {
      while (c.moveToNext()) values.put(c.getString(0), c.getDouble(1));
    }
    return values;
  }

  public double goal(String key) {
    try {
      return Double.parseDouble(preferences.getString(key, "0"));
    } catch (NumberFormatException invalid) {
      return 0;
    }
  }

  public void goals(double kcal, double protein, double carbs, double fat, double water) {
    double[] values = {kcal, protein, carbs, fat, water};
    for (double value : values)
      if (!Double.isFinite(value) || value < 0 || value > 20000)
        throw new IllegalArgumentException(
            "Goals must be non-negative numbers. Leave zero for an unset goal.");
    preferences
        .edit()
        .putString("kcal", Double.toString(kcal))
        .putString("protein", Double.toString(protein))
        .putString("carbs", Double.toString(carbs))
        .putString("fat", Double.toString(fat))
        .putString("water", Double.toString(water))
        .apply();
  }

  public Map<String, Object> export() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("format", "DietPal tracker export v1");
    out.put("exportedAt", java.time.Instant.now().toString());
    out.put("goals", preferences.getAll());
    List<Entry> entries = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query("entries", null, null, null, null, null, "day ASC, created ASC")) {
      while (c.moveToNext()) entries.add(read(c));
    }
    out.put("diary", entries);
    out.put("weightsKg", weights());
    Map<String, Integer> water = new LinkedHashMap<>();
    try (Cursor c =
        getReadableDatabase()
            .query("water", new String[] {"day", "ml"}, null, null, null, null, "day ASC")) {
      while (c.moveToNext()) water.put(c.getString(0), c.getInt(1));
    }
    out.put("waterMl", water);
    out.put("recentFoods", recent());
    return out;
  }

  public void clear() {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (String table : new String[] {"entries", "foods", "water", "weights"})
        db.delete(table, null, null);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    preferences.edit().clear().apply();
  }

  public static final class Entry {
    public String id, day, slot;
    public Food food;
    public double amount;
    public long created;

    public double calories() {
      return food.forAmount(food.kcal, amount);
    }
  }

  public static final class Totals {
    public double kcal, protein, carbs, fat;
    public boolean missingProtein, missingCarbs, missingFat;

    public Totals(List<Entry> entries) {
      for (Entry e : entries) {
        kcal += e.calories();
        if (e.food.protein == null) missingProtein = true;
        else protein += e.food.forAmount(e.food.protein, e.amount);
        if (e.food.carbs == null) missingCarbs = true;
        else carbs += e.food.forAmount(e.food.carbs, e.amount);
        if (e.food.fat == null) missingFat = true;
        else fat += e.food.forAmount(e.food.fat, e.amount);
      }
    }
  }
}
