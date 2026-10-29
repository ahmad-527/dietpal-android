package com.example.dietpal;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;

/** Replaces empty coursework information screens in the repaired edition. */
public abstract class LegacyInformationFragment extends Fragment {
  protected abstract String title();

  protected abstract String information();

  protected void actions(LinearLayout body) {}

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    ScrollView scroll = new ScrollView(requireContext());
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(0xFFFFF8EF);
    LinearLayout body = new LinearLayout(requireContext());
    body.setOrientation(LinearLayout.VERTICAL);
    int p = Math.round(24 * getResources().getDisplayMetrics().density);
    body.setPadding(p, p, p, p);
    scroll.addView(body);
    TextView heading = new TextView(requireContext());
    heading.setText(title());
    heading.setTextSize(26);
    heading.setTextColor(0xFF3F2922);
    heading.setTypeface(null, android.graphics.Typeface.BOLD);
    body.addView(heading);
    TextView text = new TextView(requireContext());
    text.setText(information());
    text.setTextSize(17);
    text.setTextColor(0xFF3F2922);
    text.setTextIsSelectable(true);
    text.setPadding(0, p, 0, p);
    body.addView(text);
    actions(body);
    return scroll;
  }

  protected void button(LinearLayout body, String label, Intent intent) {
    Button button = new Button(requireContext());
    button.setText(label);
    button.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
    body.addView(button, new LinearLayout.LayoutParams(-1, -2));
    button.setOnClickListener(
        v -> {
          try {
            startActivity(intent);
          } catch (android.content.ActivityNotFoundException unavailable) {
            Toast.makeText(
                    requireContext(), "No app is available to open this link.", Toast.LENGTH_LONG)
                .show();
          }
        });
  }
}
