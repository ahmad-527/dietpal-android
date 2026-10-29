package com.example.dietpal.Listeners;

import com.example.dietpal.Models.SimilarRecipeResponse;

import java.util.List;

public interface SimilarRecipesListener {

    void didFetch(List<SimilarRecipeResponse> response,String message);
    void didError(String message);
}
