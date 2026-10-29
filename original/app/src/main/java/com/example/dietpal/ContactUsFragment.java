package com.example.dietpal;

import android.content.Intent;
import android.net.Uri;
import android.widget.LinearLayout;

public class ContactUsFragment extends LegacyInformationFragment {
  protected String title() {
    return "Project and support";
  }

  protected String information() {
    return "Source, setup instructions and known limitations are available in the DietPal GitHub"
               + " repository.\n\n"
               + "For a reproducible bug, include the edition, Android version and steps. Keep"
               + " passwords, health records and API keys out of public issues.";
  }

  protected void actions(LinearLayout body) {
    button(
        body,
        "Open project on GitHub",
        new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ahmad-527/dietpal-android")));
  }
}
