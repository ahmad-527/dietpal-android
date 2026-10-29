package com.example.dietpal;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.LinearLayout;

public class SettingsFragment extends LegacyInformationFragment {
  protected String title() {
    return "Settings";
  }

  protected String information() {
    return "Recipe connection: "
        + new RequestManager(requireContext()).providerName()
        + ".\n\n"
        + "The public build works with TheMealDB without a private API key. A developer can"
        + " optionally configure Spoonacular before building.\n\n"
        + "Accounts stay on this phone. Log out from Home. To erase local accounts, use Android app"
        + " settings, then Storage and Clear storage. This cannot be undone.";
  }

  protected void actions(LinearLayout body) {
    button(body, "Open-source notices", new Intent(requireContext(), NoticesActivity.class));
    button(
        body,
        "Android app settings",
        new Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + requireContext().getPackageName())));
  }
}
