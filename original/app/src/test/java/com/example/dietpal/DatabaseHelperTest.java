package com.example.dietpal;

import static org.junit.Assert.*;
import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DatabaseHelperTest {
    private Application app;
    private DatabaseHelper helper;

    @Before public void setUp() {
        app = RuntimeEnvironment.getApplication();
        app.deleteDatabase("dietpal.db");
        helper = new DatabaseHelper(app);
    }

    @After public void tearDown() {
        helper.close();
        app.deleteDatabase("dietpal.db");
    }

    @Test public void storesHashAndVerifiesCredentials() {
        assertTrue(helper.insertData("tester", "tester@example.com", "demo-password"));
        assertTrue(helper.checkUser("tester", "demo-password"));
        assertFalse(helper.checkUser("tester", "wrong-password"));
        try (Cursor row = helper.getReadableDatabase().rawQuery("SELECT PASSWORD FROM user_table", null)) {
            assertTrue(row.moveToFirst());
            assertNotEquals("demo-password", row.getString(0));
            assertTrue(row.getString(0).startsWith("pbkdf2_sha256$600000$"));
        }
    }

    @Test public void handlesSqlMetacharactersAsLiteralUsernames() {
        String name = "tester' OR 1=1 --";
        assertTrue(helper.insertData(name, "tester@example.com", "demo-password"));
        assertTrue(helper.checkUser(name, "demo-password"));
        assertFalse(helper.checkUser("' OR 1=1 --", "demo-password"));
        assertFalse(helper.insertData(name, "duplicate@example.com", "different-password"));
    }

    @Test public void rejectsInvalidNewCredentials() {
        assertFalse(helper.insertData(" ", "tester@example.com", "demo-password"));
        assertFalse(helper.insertData("tester", "tester@example.com", "short"));
        assertFalse(helper.checkUser(null, "demo-password"));
    }

    @Test public void upgradesLegacyAccountsWithoutDeletingThem() {
        helper.close();
        try (SQLiteDatabase old = app.openOrCreateDatabase("dietpal.db", 0, null)) {
            old.execSQL("CREATE TABLE user_table (ID INTEGER PRIMARY KEY AUTOINCREMENT, USERNAME TEXT, EMAIL TEXT, PASSWORD TEXT)");
            old.execSQL("INSERT INTO user_table (USERNAME, EMAIL, PASSWORD) VALUES (?, ?, ?)",
                    new Object[]{"legacy", "legacy@example.com", "legacy-password"});
            old.setVersion(1);
        }
        helper = new DatabaseHelper(app);
        assertTrue(helper.checkUser("legacy", "legacy-password"));
        try (Cursor row = helper.getReadableDatabase().rawQuery("SELECT PASSWORD FROM user_table", null)) {
            assertTrue(row.moveToFirst());
            assertNotEquals("legacy-password", row.getString(0));
        }
    }
}
