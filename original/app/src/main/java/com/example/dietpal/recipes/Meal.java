package com.example.dietpal.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A recipe's supplied facts. No inferred calories, timings, ratings or health scores. */
public final class Meal {
    public String id = "";
    public String title = "";
    public String category = "";
    public String area = "";
    public String image = "";
    public String sourceUrl = "";
    public String provider = "starter";
    public List<String> ingredients = new ArrayList<>();
    public List<String> steps = new ArrayList<>();

    public boolean isComplete() { return !ingredients.isEmpty() && !steps.isEmpty(); }

    public boolean matches(String query, String filter) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        boolean categoryMatches = filter.equals("All") || category.equalsIgnoreCase(filter);
        // Online search is by meal name; offline/saved search uses the same meaning.
        return categoryMatches && title.toLowerCase(Locale.ROOT).contains(q);
    }

    public String subtitle() {
        if (area.isEmpty()) return category;
        return category.isEmpty() ? area : category + " · " + area;
    }
}
