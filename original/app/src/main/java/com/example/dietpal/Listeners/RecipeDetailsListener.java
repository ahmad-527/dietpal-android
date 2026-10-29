package com.example.dietpal.Listeners;

import com.example.dietpal.Models.RecipeDetailsResponse;

public interface RecipeDetailsListener {

    void didFetch(RecipeDetailsResponse response,String message);
    void didError(String message);
}
