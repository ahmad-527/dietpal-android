package com.example.dietpal;

public class PrivacyPolicyFragment extends LegacyInformationFragment {
  protected String title() {
    return "Privacy";
  }

  protected String information() {
    return "Accounts and salted password hashes are stored locally in the app's private database."
               + " Registration and login do not contact a server. Android backup is disabled. BMI"
               + " inputs and timers are not saved as health records.\n\n"
               + "Recipe browsing, searches and images contact TheMealDB in the public build. If a"
               + " developer configures a Spoonacular key, recipe requests contact Spoonacular"
               + " instead. Those providers receive requests, search terms and connection"
               + " information such as your IP address. Accounts and passwords are not sent to"
               + " recipe providers. External links open in your chosen app.\n\n"
               + "DietPal includes no advertising or analytics service. To erase its local"
               + " accounts, open Android app settings and clear its storage, or uninstall it. Data"
               + " you separately saved outside this app must be removed separately.";
  }
}
