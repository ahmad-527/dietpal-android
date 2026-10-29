package com.example.dietpal.Models;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.dietpal.Adapters.RandomRecipeAdapter;
import com.example.dietpal.Listeners.RandomRecipeResponseListener;
import com.example.dietpal.Listeners.RecipeClickListener;
import com.example.dietpal.R;
import com.example.dietpal.RecipeDetailsActivity;
import com.example.dietpal.RequestManager;

import java.util.ArrayList;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {

    ProgressDialog dialog;
    RequestManager manager;
    RandomRecipeAdapter randomRecipeAdapter;
    RecyclerView recyclerView;
    Spinner spinner;
    List<String> tags=new ArrayList<>();

    SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        dialog=new ProgressDialog(this);
        dialog.setTitle("Loading...");

        searchView=findViewById(R.id.searchView_home);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                tags.clear();
                tags.add(query);
                manager.getRandomRecipes(randomRecipeResponseListener,tags);
                dialog.show();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        spinner=findViewById(R.id.spinner_tags);
        ArrayAdapter arrayAdapter= ArrayAdapter.createFromResource(
                this,
                R.array.tags,
                R.layout.spinner_text
        );

        arrayAdapter.setDropDownViewResource(R.layout.spinner_inner_text);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(spinnerSelectedListener);


        manager= new RequestManager(this);
       // manager.getRandomRecipes(randomRecipeResponseListener);
       // dialog.show();

    }

    private  final RandomRecipeResponseListener randomRecipeResponseListener= new RandomRecipeResponseListener() {
        @Override
        public void didFetch(RandomRecipeApiResponse response, String message) {

            dialog.dismiss();
            recyclerView=findViewById(R.id.recycler_random);
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new GridLayoutManager(RecipeActivity.this,1));

            randomRecipeAdapter=new RandomRecipeAdapter(RecipeActivity.this,response.recipes,recipeClickListener);
            recyclerView.setAdapter(randomRecipeAdapter);

        }

        @Override
        public void didError(String message) {

            Toast.makeText(RecipeActivity.this,message,Toast.LENGTH_SHORT);

        }
    };


    private final AdapterView.OnItemSelectedListener spinnerSelectedListener= new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            tags.clear();
            tags.add(parent.getSelectedItem().toString());
            manager.getRandomRecipes(randomRecipeResponseListener,tags);
            dialog.show();

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private final RecipeClickListener recipeClickListener=new RecipeClickListener() {
        @Override
        public void onRecipeClicked(String id) {

            startActivity(new Intent(RecipeActivity.this, RecipeDetailsActivity.class)
                    .putExtra("id",id));


        }
    };
}