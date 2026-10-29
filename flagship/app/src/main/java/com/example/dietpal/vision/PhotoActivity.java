package com.example.dietpal.vision;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.dietpal.R;
import com.example.dietpal.recipes.RecipeUi;
import com.example.dietpal.tracker.FoodLogActivity;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PhotoActivity extends AppCompatActivity {
  private LocalModel model;
  private PhotoAnalyzer analyzer;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private TextView status;
  private ProgressBar progress;
  private MaterialButton download, analyze, choose, camera;
  private LinearLayout results;
  private ImageView preview;
  private byte[] jpeg;
  private File cameraFile;
  private Uri cameraUri;
  private String day;

  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    model = new LocalModel(this);
    analyzer = new PhotoAnalyzer();
    day = getIntent().getStringExtra("day");
    if (day == null) day = java.time.LocalDate.now().toString();
    LinearLayout root = RecipeUi.root(this);
    RecipeUi.header(this, root, "Meal photo");
    LinearLayout body = RecipeUi.scroll(this, root);
    body.addView(
        RecipeUi.text(
            this,
            "Identify a meal on this device, then review the foods, portions and nutrition before"
                + " logging. Photos are not uploaded.",
            16,
            false));
    RecipeUi.gap(this, body, 14);
    status = RecipeUi.text(this, "", 14, false);
    status.setId(R.id.photo_status);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    body.addView(status);
    download =
        RecipeUi.button(
            this,
            model.installed() ? "Local model installed" : "Download free model · 2.59 GB",
            false);
    download.setId(R.id.photo_download);
    download.setOnClickListener(
        v ->
            new AlertDialog.Builder(this)
                .setTitle("Download the local model?")
                .setMessage(
                    "This downloads 2.59 GB from the official LiteRT Community model repository."
                        + " Use Wi-Fi if you want to avoid mobile-data use. After installation,"
                        + " meal-photo analysis runs on this device without an AI API account. The"
                        + " model uses the Apache 2.0 licence. You can continue tracking without"
                        + " downloading it.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Download", (d, w) -> download())
                .show());
    body.addView(download);
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setVisibility(View.GONE);
    body.addView(progress, new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 5)));
    preview = new ImageView(this);
    preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
    preview.setContentDescription("Selected meal photo");
    preview.setImageResource(R.drawable.recipe_placeholder);
    body.addView(preview, new LinearLayout.LayoutParams(-1, RecipeUi.dp(this, 210)));
    choose = RecipeUi.button(this, "Choose a meal photo", false);
    choose.setId(R.id.photo_choose);
    choose.setOnClickListener(
        v -> {
          Intent intent =
              new Intent(Intent.ACTION_OPEN_DOCUMENT)
                  .setType("image/*")
                  .addCategory(Intent.CATEGORY_OPENABLE);
          try {
            startActivityForResult(intent, 101);
          } catch (android.content.ActivityNotFoundException unavailable) {
            status.setText("No photo picker is available.");
          }
        });
    body.addView(choose);
    camera = RecipeUi.button(this, "Take a photo", false);
    camera.setOnClickListener(v -> camera());
    body.addView(camera);
    analyze = RecipeUi.button(this, "Analyse on this device", true);
    analyze.setId(R.id.photo_analyze);
    analyze.setOnClickListener(v -> analyze());
    body.addView(analyze);
    body.addView(
        RecipeUi.text(
            this,
            "Use a clear photo of one plate. Hidden ingredients and serving sizes cannot be"
                + " measured reliably from a picture. Results are rough estimates, not"
                + " label-verified nutrition.",
            13,
            false));
    RecipeUi.gap(this, body, 16);
    results = RecipeUi.column(this);
    results.setId(R.id.photo_results);
    body.addView(results);
    refresh();
    if (state != null && state.getString("camera") != null) {
      try {
        cameraFile = new File(getCacheDir(), state.getString("camera"));
        if (cameraFile
            .getCanonicalFile()
            .getParentFile()
            .equals(new File(getCacheDir(), "meal_photos").getCanonicalFile()))
          cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", cameraFile);
        else cameraFile = null;
      } catch (java.io.IOException invalid) {
        cameraFile = null;
      }
    }
  }

  private void refresh() {
    String support = model.supportMessage();
    status.setText(
        !support.isEmpty()
            ? support
            : model.installed()
                ? "Ready for local analysis. Review every result before logging."
                : "Optional: Gemma 4 E2B · Apache 2.0. At least 6 GB RAM; 8 GB recommended.");
    download.setEnabled(!model.installed() && support.isEmpty());
    analyze.setEnabled(model.installed() && jpeg != null && support.isEmpty());
  }

  private void busy(boolean busy) {
    choose.setEnabled(!busy);
    camera.setEnabled(!busy);
    analyze.setEnabled(!busy && model.installed() && jpeg != null);
    download.setEnabled(!busy && !model.installed() && model.supportMessage().isEmpty());
    progress.setVisibility(busy ? View.VISIBLE : View.GONE);
  }

  private void download() {
    busy(true);
    progress.setIndeterminate(false);
    progress.setMax(100);
    status.setText("Downloading local model…");
    worker.execute(
        () -> {
          try {
            model.download(
                received ->
                    runOnUiThread(
                        () -> {
                          if (!isDestroyed()) {
                            progress.setProgress((int) (received * 100 / LocalModel.BYTES));
                            status.setText(
                                "Downloading model · " + (received / 1_000_000) + " / 2588 MB");
                          }
                        }));
            runOnUiThread(
                () -> {
                  if (!isDestroyed()) {
                    download.setText("Local model installed");
                    busy(false);
                    refresh();
                  }
                });
          } catch (Exception error) {
            showError(error.getMessage());
          }
        });
  }

  private void camera() {
    try {
      File folder = new File(getCacheDir(), "meal_photos");
      if (!folder.isDirectory() && !folder.mkdirs()) throw new java.io.IOException();
      cameraFile = File.createTempFile("meal-", ".jpg", folder);
      cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", cameraFile);
      Intent intent =
          new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
              .putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
              .addFlags(
                  Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivityForResult(intent, 102);
    } catch (Exception unavailable) {
      status.setText("A camera could not be opened. You can choose an existing photo instead.");
    }
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK) return;
    Uri uri = request == 102 ? cameraUri : request == 101 && data != null ? data.getData() : null;
    if (uri == null) return;
    busy(true);
    progress.setIndeterminate(true);
    status.setText("Preparing the photo locally…");
    worker.execute(
        () -> {
          try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
              BitmapFactory.decodeStream(input, null, bounds);
            }
            if (bounds.outWidth <= 0
                || bounds.outHeight <= 0
                || bounds.outWidth > 50000
                || bounds.outHeight > 50000)
              throw new java.io.IOException("Choose a supported image.");
            BitmapFactory.Options options = new BitmapFactory.Options();
            int size = 1;
            while (bounds.outWidth / size > 1024 || bounds.outHeight / size > 1024) size *= 2;
            options.inSampleSize = size;
            Bitmap image;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
              image = BitmapFactory.decodeStream(input, null, options);
            }
            if (image == null) throw new java.io.IOException("The photo could not be read.");
            int orientation;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
              orientation =
                  new ExifInterface(input)
                      .getAttributeInt(
                          ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
            Matrix matrix = new Matrix();
            switch (orientation) {
              case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
              case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
              case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
              case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
              case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
              case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(270);
                matrix.postScale(-1, 1);
                break;
              case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;
              default:
                break;
            }
            if (!matrix.isIdentity()) {
              Bitmap rotated =
                  Bitmap.createBitmap(
                      image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
              image.recycle();
              image = rotated;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 90, output);
            jpeg = output.toByteArray();
            Bitmap display = image;
            runOnUiThread(
                () -> {
                  if (isDestroyed()) {
                    display.recycle();
                    return;
                  }
                  preview.setImageBitmap(display);
                  results.removeAllViews();
                  busy(false);
                  refresh();
                });
          } catch (Exception error) {
            showError("The selected image could not be prepared. Try another photo.");
          }
        });
  }

  private void analyze() {
    busy(true);
    progress.setIndeterminate(true);
    results.removeAllViews();
    status.setText("Analysing locally. This may take a little while…");
    worker.execute(
        () -> {
          try {
            List<MealEstimate> estimates = analyzer.analyze(getApplicationContext(), jpeg);
            runOnUiThread(
                () -> {
                  if (isDestroyed()) return;
                  busy(false);
                  status.setText(
                      estimates.isEmpty()
                          ? "No useful meal estimate was produced. Try a clearer photo or enter"
                              + " food manually."
                          : "Estimates ready. Review and edit each food before it enters your"
                              + " diary.");
                  int ordinal = 0;
                  for (MealEstimate estimate : estimates) {
                    LinearLayout card = RecipeUi.cardBody(this, results);
                    card.addView(RecipeUi.text(this, estimate.food.name, 24, true));
                    card.addView(
                        RecipeUi.text(
                            this,
                            "≈ "
                                + Math.round(estimate.grams)
                                + " g · ≈ "
                                + Math.round(
                                    estimate.food.forAmount(estimate.food.kcal, estimate.grams))
                                + " kcal",
                            18,
                            false));
                    card.addView(
                        RecipeUi.text(
                            this,
                            "On-device AI estimate. Ingredient identity, portions and nutrition may"
                                + " be wrong.",
                            13,
                            false));
                    MaterialButton review = RecipeUi.button(this, "Review & log", true);
                    if (ordinal++ == 0) review.setId(R.id.photo_review_first);
                    review.setOnClickListener(
                        v ->
                            startActivity(
                                new Intent(this, FoodLogActivity.class)
                                    .putExtra("day", day)
                                    .putExtra("food", new Gson().toJson(estimate.food))
                                    .putExtra("amount", estimate.grams)));
                    card.addView(review);
                  }
                });
          } catch (Exception error) {
            showError("Local analysis could not complete. Try again or enter food manually.");
          }
        });
  }

  private void showError(String message) {
    runOnUiThread(
        () -> {
          if (isDestroyed()) return;
          busy(false);
          status.setText(
              message == null ? "Could not complete this action. Please retry." : message);
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    if (cameraFile != null) state.putString("camera", "meal_photos/" + cameraFile.getName());
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onDestroy() {
    analyzer.cancel();
    worker.shutdownNow();
    if (cameraUri != null)
      revokeUriPermission(
          cameraUri,
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    if (cameraFile != null && !isChangingConfigurations()) cameraFile.delete();
    super.onDestroy();
  }
}
