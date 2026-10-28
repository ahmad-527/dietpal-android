package com.example.dietpal;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import com.example.dietpal.Listeners.*;
import com.example.dietpal.Models.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@LooperMode(LooperMode.Mode.PAUSED)
public class RecipeAndWorkoutTest {
    @Test public void missingKeyReportsEveryEndpointWithoutStartingNetworkCalls() {
        RequestManager manager = new RequestManager("");
        List<String> errors = new ArrayList<>();
        manager.getRandomRecipes(new RandomRecipeResponseListener() {
            public void didFetch(RandomRecipeApiResponse response, String message) { fail(); }
            public void didError(String message) { errors.add(message); }
        }, Collections.emptyList());
        manager.getRecipeDetails(new RecipeDetailsListener() {
            public void didFetch(RecipeDetailsResponse response, String message) { fail(); }
            public void didError(String message) { errors.add(message); }
        }, 1);
        manager.getSimilarRecipes(new SimilarRecipesListener() {
            public void didFetch(List<SimilarRecipeResponse> response, String message) { fail(); }
            public void didError(String message) { errors.add(message); }
        }, 1);
        manager.getInstructions(new InstructionsListener() {
            public void didFetch(List<InstructionsResponse> response, String message) { fail(); }
            public void didError(String message) { errors.add(message); }
        }, 1);
        assertEquals(4, errors.size());
        for (String error : errors) assertTrue(error.contains("not configured"));
        manager.cancelPendingRequests();
    }

    @Test public void missingKeyRecipeScreenShowsStatusAndClosesLoadingDialog() {
        Assume.assumeTrue(BuildConfig.SPOONACULAR_API_KEY.isEmpty());
        try (ActivityController<RecipeActivity> controller = Robolectric.buildActivity(RecipeActivity.class).setup().visible()) {
            shadowOf(Looper.getMainLooper()).idle();
            TextView status = controller.get().findViewById(R.id.recipe_status);
            assertEquals(View.VISIBLE, status.getVisibility());
            assertTrue(status.getText().toString().contains("not configured"));
            assertTrue(ShadowDialog.getLatestDialog() == null || !ShadowDialog.getLatestDialog().isShowing());
        }
    }

    @Test public void missingRecipeIdAndInvalidExerciseIdCloseGracefully() {
        try (ActivityController<RecipeDetailsActivity> details = Robolectric.buildActivity(RecipeDetailsActivity.class).setup()) {
            assertTrue(details.get().isFinishing());
        }
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), workout.class).putExtra("value", "invalid");
        try (ActivityController<workout> exercise = Robolectric.buildActivity(workout.class, intent).setup()) {
            assertTrue(exercise.get().isFinishing());
        }
    }

    @Test public void allFifteenWorkoutLayoutsContainTimerAndStartControl() {
        for (int i = 1; i <= 15; i++) {
            Intent intent = new Intent(RuntimeEnvironment.getApplication(), workout.class).putExtra("value", Integer.toString(i));
            try (ActivityController<workout> exercise = Robolectric.buildActivity(workout.class, intent).setup()) {
                assertNotNull(exercise.get().findViewById(R.id.startButton));
                assertNotNull(exercise.get().findViewById(R.id.time));
            }
        }
    }

    @Test public void stoppingWorkoutPreventsLaterTimerNavigation() {
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), workout.class).putExtra("value", "1");
        try (ActivityController<workout> exercise = Robolectric.buildActivity(workout.class, intent).setup()) {
            exercise.get().findViewById(R.id.startButton).performClick();
            exercise.pause().stop();
            shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.MINUTES);
            assertNull(shadowOf(exercise.get()).getNextStartedActivity());
        }
    }
}
