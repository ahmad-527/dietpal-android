package com.example.dietpal.planning;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.dietpal.recipes.Meal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Keeps a draft and its background computation alive across Android configuration changes. */
public final class PlannerViewModel extends ViewModel {
  public final MutableLiveData<Integer> progress = new MutableLiveData<>(-1);
  public final ExecutorService worker = Executors.newSingleThreadExecutor();
  public List<PlannedDay> draft;
  public PlannerProfile profile;
  public String start, horizon = "Week", error;
  public boolean saved, busy;
  public int page;

  public void generate(List<Meal> catalog) {
    if (busy) return;
    profile.validate();
    PlannerProfile snapshot =
        new com.google.gson.Gson()
            .fromJson(new com.google.gson.Gson().toJson(profile), PlannerProfile.class);
    LocalDate first = LocalDate.parse(start), end = MealPlanner.endExclusive(first, horizon);
    busy = true;
    saved = false;
    error = null;
    draft = null;
    progress.setValue(0);
    worker.execute(
        () -> {
          try {
            List<PlannedDay> days = new ArrayList<>();
            Map<String, Integer> recent = new LinkedHashMap<>();
            int total = (int) java.time.temporal.ChronoUnit.DAYS.between(first, end);
            for (int i = 0; i < total; i++) {
              PlannedDay day =
                  MealPlanner.generate(first.plusDays(i), snapshot, catalog, recent, 0);
              days.add(day);
              for (PlannedDay.Occasion meal : day.meals)
                for (PlannedDay.Dish dish : meal.dishes)
                  recent.merge(dish.recipe.id, 1, Integer::sum);
              if (i >= 6)
                for (PlannedDay.Occasion meal : days.get(i - 6).meals)
                  for (PlannedDay.Dish dish : meal.dishes)
                    recent.computeIfPresent(dish.recipe.id, (id, count) -> Math.max(0, count - 1));
              progress.postValue((i + 1) * 100 / total);
            }
            draft = days;
            page = 0;
          } catch (Exception failure) {
            error = failure.getMessage();
          } finally {
            busy = false;
            progress.postValue(-1);
          }
        });
  }

  @Override
  protected void onCleared() {
    worker.shutdownNow();
  }
}
