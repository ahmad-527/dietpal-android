package com.example.dietpal.Listeners;

import com.example.dietpal.Models.InstructionsResponse;

import java.util.List;

public interface InstructionsListener {

    void didFetch(List<InstructionsResponse> response, String message);
    void didError(String message);
}
