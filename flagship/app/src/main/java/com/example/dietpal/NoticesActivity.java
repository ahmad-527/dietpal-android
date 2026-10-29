package com.example.dietpal;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class NoticesActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle state) {
    setTheme(R.style.Theme_DietPal_Flagship);
    super.onCreate(state);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(0xFFF3F5F9);
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    int p = Math.round(20 * getResources().getDisplayMetrics().density);
    body.setPadding(p, p, p, p);
    scroll.addView(body);
    setContentView(scroll);
    Button back = new Button(this);
    back.setText("Back");
    back.setOnClickListener(v -> finish());
    body.addView(back);
    TextView title = new TextView(this);
    title.setText("Open-source notices");
    title.setTextSize(24);
    title.setTextColor(0xFF182332);
    body.addView(title);
    try {
      String[] names = getAssets().list("licenses");
      java.util.Arrays.sort(names);
      // Show attribution first, followed by the retained full upstream notices.
      java.util.List<String> ordered = new java.util.ArrayList<>(java.util.Arrays.asList(names));
      ordered.remove("COMPONENTS.txt");
      ordered.add(0, "COMPONENTS.txt");
      for (String name : ordered) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream input = getAssets().open("licenses/" + name)) {
          byte[] b = new byte[8192];
          int n;
          while ((n = input.read(b)) != -1) out.write(b, 0, n);
        }
        TextView text = new TextView(this);
        text.setText(name + "\n\n" + new String(out.toByteArray(), StandardCharsets.UTF_8));
        text.setTextSize(13);
        text.setTextColor(0xFF182332);
        text.setTextIsSelectable(true);
        text.setPadding(0, p, 0, p);
        body.addView(text);
      }
    } catch (IOException unavailable) {
      TextView error = new TextView(this);
      error.setText("Notices are also available in the project's licenses folder on GitHub.");
      body.addView(error);
    }
  }
}
