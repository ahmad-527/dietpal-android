package com.example.dietpal.Models;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dietpal.Adapters.RandomRecipeAdapter;
import com.example.dietpal.Listeners.RandomRecipeResponseListener;
import com.example.dietpal.R;
import com.example.dietpal.RecipeDetailsActivity;
import com.example.dietpal.RequestManager;
import java.util.ArrayList;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {
    private ProgressDialog dialog;
    private RequestManager manager;
    private RecyclerView recycler;
    private TextView status;
    private final List<String> tags = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);
        manager = new RequestManager(this);
        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading recipes...");
        status = findViewById(R.id.recipe_status);
        recycler = findViewById(R.id.recycler_random);
        recycler.setLayoutManager(new GridLayoutManager(this, 1));
        SearchView search = findViewById(R.id.searchView_home);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                tags.clear(); tags.add(query.trim()); fetchRecipes(); return true;
            }
            @Override public boolean onQueryTextChange(String text) { return false; }
        });
        Spinner spinner = findViewById(R.id.spinner_tags);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.tags, R.layout.spinner_text);
        adapter.setDropDownViewResource(R.layout.spinner_inner_text);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tags.clear(); tags.add(parent.getSelectedItem().toString()); fetchRecipes();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void fetchRecipes() {
        status.setVisibility(View.GONE);
        dialog.show();
        manager.getRandomRecipes(new RandomRecipeResponseListener() {
            @Override public void didFetch(RandomRecipeApiResponse response, String message) {
                if (isFinishing() || isDestroyed()) return;
                dialog.dismiss();
                if (response == null || response.recipes == null || response.recipes.isEmpty()) {
                    showError("No recipes matched this request.");
                    return;
                }
                recycler.setAdapter(new RandomRecipeAdapter(RecipeActivity.this, response.recipes,
                        id -> startActivity(new Intent(RecipeActivity.this, RecipeDetailsActivity.class).putExtra("id", id))));
            }
            @Override public void didError(String message) {
                if (isFinishing() || isDestroyed()) return;
                showError(message);
            }
        }, tags);
    }

    private void showError(String message) {
        dialog.dismiss();
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    @Override protected void onDestroy() {
        if (manager != null) manager.cancelPendingRequests();
        if (dialog != null) dialog.dismiss();
        super.onDestroy();
    }
}
