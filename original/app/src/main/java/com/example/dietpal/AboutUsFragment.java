package com.example.dietpal;

public class AboutUsFragment extends LegacyInformationFragment {
  protected String title() {
    return "About DietPal Original";
  }

  protected String information() {
    return "DietPal began as an Android coursework project with local registration, BMI"
               + " calculation, animated exercise guides, a timer and recipe discovery.\n\n"
               + "This repaired edition retains that experience, with compatibility,"
               + " account-security and recipe-connection repairs. The repository also contains a"
               + " separate flagship edition and a frozen historical source branch.\n\n"
               + "BMI is a general screening calculation. Recipes and exercise demonstrations are"
               + " for general information; this app does not assess medical suitability or provide"
               + " a personalised training programme.";
  }
}
