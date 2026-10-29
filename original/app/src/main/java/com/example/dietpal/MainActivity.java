package com.example.dietpal;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.dietpal.Models.RecipeActivity;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
  NavigationView navigationView;
  DrawerLayout drawerLayout;
  Toolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    navigationView = findViewById(R.id.navigationView);
    drawerLayout = findViewById(R.id.drawerLayout);
    toolbar = findViewById(R.id.toolbar);

    ActionBarDrawerToggle actionBarDrawerToggle =
        new ActionBarDrawerToggle(
            MainActivity.this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer);
    drawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();
    if (savedInstanceState == null) {
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.frameLayout, new HomeFragment())
          .commit();
      navigationView.setCheckedItem(R.id.home);
    }
    navigationView.setNavigationItemSelectedListener(
        new NavigationView.OnNavigationItemSelectedListener() {
          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.home) {
              getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new HomeFragment())
                  .commit();

            } else if (item.getItemId() == R.id.bmicalc) {
              getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new BmiFragment())
                  .commit();
            } else if (item.getItemId() == R.id.meals) {

              startActivity(new Intent(MainActivity.this, RecipeActivity.class));
            } else if (item.getItemId() == R.id.settings) {
              getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new SettingsFragment())
                  .commit();

            } else if (item.getItemId() == R.id.aboutUs) {
              getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new AboutUsFragment())
                  .commit();

            } else if (item.getItemId() == R.id.contact) {

              getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new ContactUsFragment())
                  .commit();

            } else if (item.getItemId() == R.id.privacyPol) {
              getSupportFragmentManager()
                  .beginTransaction()
                  .replace(R.id.frameLayout, new PrivacyPolicyFragment())
                  .commit();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
          }
        });
  }
}
