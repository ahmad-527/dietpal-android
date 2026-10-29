package com.example.dietpal;

import static org.junit.Assert.*;
import org.junit.Test;

public class PasswordHasherTest {
    @Test public void authenticatesOnlyTheCorrectPassword() {
        String record = PasswordHasher.hash("demo-password");
        assertTrue(PasswordHasher.verify("demo-password", record));
        assertFalse(PasswordHasher.verify("wrong-password", record));
        assertFalse(record.contains("demo-password"));
    }

    @Test public void saltsEqualPasswordsIndependently() {
        String first = PasswordHasher.hash("same-password");
        String second = PasswordHasher.hash("same-password");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify("same-password", second));
    }

    @Test public void rejectsMalformedAndPlaintextRecords() {
        for (String record : new String[]{null, "", "demo-password", "pbkdf2_sha256$999999999$x$x",
                "pbkdf2_sha256$600000$invalid!$invalid!", "pbkdf2_sha256$600000$AA==$AA=="}) {
            assertFalse(PasswordHasher.verify("demo-password", record));
        }
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("demo-password")));
    }
}
