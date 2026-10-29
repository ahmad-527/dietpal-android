package com.example.dietpal.vision;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** Pinned, optional model; user photos are never part of a network request. */
public final class LocalModel {
  public static final long BYTES = 2588147712L;
  public static final String SHA256 =
      "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c";
  public static final String REVISION = "1caa527c1e88b55f046fcd8d2b4265606680663d";
  private static final String URL =
      "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/"
          + REVISION
          + "/gemma-4-E2B-it.litertlm?download=true";
  private final Context context;

  public LocalModel(Context context) {
    this.context = context.getApplicationContext();
  }

  public File file() {
    return new File(context.getFilesDir(), "models/gemma4-e2b.litertlm");
  }

  public boolean installed() {
    return file().isFile() && file().length() == BYTES;
  }

  public String supportMessage() {
    boolean abi = false;
    for (String supported : Build.SUPPORTED_ABIS)
      if (supported.equals("arm64-v8a") || supported.equals("x86_64")) abi = true;
    if (!abi)
      return "Photo analysis requires a supported 64-bit device. Food logging and recipes are"
          + " available without it.";
    ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
    ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(info);
    if (info.totalMem < 5_500_000_000L)
      return "The local photo model needs a device with at least 6 GB RAM (8 GB recommended). You"
          + " can use the rest of DietPal without this download.";
    return "";
  }

  public void verify() throws Exception {
    if (!installed()) throw new IOException("Download the local model first.");
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] buffer = new byte[4 * 1024 * 1024];
    try (InputStream input = new FileInputStream(file())) {
      int count;
      while ((count = input.read(buffer)) != -1) {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
        digest.update(buffer, 0, count);
      }
    }
    StringBuilder value = new StringBuilder();
    for (byte b : digest.digest())
      value.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
    if (!SHA256.contentEquals(value))
      throw new IOException("Model integrity check failed. Download a fresh copy.");
  }

  public interface Progress {
    void bytes(long received);
  }

  public void download(Progress progress) throws Exception {
    File folder = file().getParentFile();
    if (!folder.isDirectory() && !folder.mkdirs())
      throw new IOException("Could not create model storage.");
    File part = new File(folder, "model.part");
    if (new StatFs(folder.getAbsolutePath()).getAvailableBytes() + part.length()
        < BYTES + 100_000_000L)
      throw new IOException("About 2.7 GB of free storage is required for the model.");
    HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
    connection.setConnectTimeout(30000);
    connection.setReadTimeout(60000);
    connection.setRequestProperty("User-Agent", "DietPal/2.0 model-downloader");
    try {
      if (connection.getResponseCode() != 200)
        throw new IOException("Model download is unavailable. Please retry later.");
      long received = 0;
      try (InputStream input = connection.getInputStream();
          FileOutputStream output = new FileOutputStream(part)) {
        byte[] buffer = new byte[1024 * 1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
          if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
          received += count;
          if (received > BYTES) throw new IOException("Unexpected model size.");
          output.write(buffer, 0, count);
          progress.bytes(received);
        }
      }
      if (received != BYTES) throw new IOException("Download was incomplete. Please retry.");
      // The path and content are pinned, and the final file is never exposed before verification.
      MessageDigest hash = MessageDigest.getInstance("SHA-256");
      try (InputStream input = new FileInputStream(part)) {
        byte[] buffer = new byte[4 * 1024 * 1024];
        int count;
        while ((count = input.read(buffer)) != -1) hash.update(buffer, 0, count);
      }
      StringBuilder actual = new StringBuilder();
      for (byte b : hash.digest())
        actual.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
      if (!SHA256.contentEquals(actual))
        throw new IOException("Downloaded model did not match its published checksum.");
      if (file().exists() && !file().delete())
        throw new IOException("Could not replace the previous model.");
      if (!part.renameTo(file())) throw new IOException("Could not finish model installation.");
    } finally {
      connection.disconnect();
    }
  }
}
