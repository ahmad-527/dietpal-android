package com.example.dietpal;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Versioned local password records for the portfolio edition; requires API 26+. */
final class PasswordHasher {
  private static final int ITERATIONS = 600_000;
  private static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 256;
  private static final String PREFIX = "pbkdf2_sha256";
  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordHasher() {}

  static String hash(String password) {
    if (password == null) throw new IllegalArgumentException("Password is required");
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    return PREFIX
        + "$"
        + ITERATIONS
        + "$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + Base64.getEncoder().encodeToString(derive(password, salt));
  }

  static boolean verify(String password, String record) {
    if (password == null || record == null) return false;
    String[] fields = record.split("\\$", -1);
    if (fields.length != 4
        || !PREFIX.equals(fields[0])
        || !Integer.toString(ITERATIONS).equals(fields[1])) return false;
    try {
      byte[] salt = Base64.getDecoder().decode(fields[2]);
      byte[] expected = Base64.getDecoder().decode(fields[3]);
      return salt.length == SALT_BYTES
          && expected.length == KEY_BITS / 8
          && MessageDigest.isEqual(expected, derive(password, salt));
    } catch (IllegalArgumentException invalidRecord) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt) {
    char[] characters = password.toCharArray();
    PBEKeySpec spec = new PBEKeySpec(characters, salt, ITERATIONS, KEY_BITS);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException unavailable) {
      throw new IllegalStateException("Password hashing is unavailable", unavailable);
    } finally {
      spec.clearPassword();
      Arrays.fill(characters, '\0');
    }
  }
}
