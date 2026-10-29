package com.example.dietpal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "dietpal.db";
  private static final String TABLE_NAME = "user_table";
  private static final String COL_1 = "ID";
  private static final String COL_2 = "USERNAME";
  private static final String COL_3 = "EMAIL";
  private static final String COL_4 = "PASSWORD";

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, 2);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE "
            + TABLE_NAME
            + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, USERNAME TEXT NOT NULL UNIQUE, EMAIL TEXT"
            + " NOT NULL, PASSWORD TEXT NOT NULL)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Preserve legacy demo accounts while replacing their plaintext passwords.
    if (oldVersion < 2) {
      try (Cursor cursor =
          db.query(TABLE_NAME, new String[] {COL_1, COL_4}, null, null, null, null, null)) {
        while (cursor.moveToNext()) {
          ContentValues update = new ContentValues();
          String legacyPassword = cursor.isNull(1) ? "" : cursor.getString(1);
          update.put(COL_4, PasswordHasher.hash(legacyPassword));
          db.update(
              TABLE_NAME, update, COL_1 + " = ?", new String[] {Long.toString(cursor.getLong(0))});
        }
      }
    }
  }

  // Method to insert user data into the database
  public boolean insertData(String username, String email, String password) {
    if (username == null
        || username.trim().isEmpty()
        || email == null
        || email.trim().isEmpty()
        || password == null
        || password.length() < 8
        || password.length() > 256) return false;
    username = username.trim();
    if (checkUsernameExists(username)) return false;
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put(COL_2, username);
    contentValues.put(COL_3, email.trim());
    contentValues.put(COL_4, PasswordHasher.hash(password));
    long result = db.insert(TABLE_NAME, null, contentValues);
    return result != -1;
  }

  // Method to check if a user with the given username and password exists in the database
  public boolean checkUser(String username, String password) {
    if (username == null || password == null || password.length() > 256) return false;
    SQLiteDatabase db = this.getReadableDatabase();
    try (Cursor cursor =
        db.query(
            TABLE_NAME,
            new String[] {COL_4},
            COL_2 + " = ?",
            new String[] {username.trim()},
            null,
            null,
            null)) {
      return cursor.moveToFirst() && PasswordHasher.verify(password, cursor.getString(0));
    }
  }

  // Method to check if a user with the given username already exists in the database
  public boolean checkUsernameExists(String username) {
    if (username == null) return false;
    SQLiteDatabase db = this.getReadableDatabase();
    try (Cursor cursor =
        db.query(
            TABLE_NAME,
            new String[] {COL_1},
            COL_2 + " = ?",
            new String[] {username.trim()},
            null,
            null,
            null)) {
      return cursor.moveToFirst();
    }
  }
}
