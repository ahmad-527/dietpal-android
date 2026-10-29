package com.example.dietpal;

import static org.junit.Assert.*;
import com.example.dietpal.tracker.*;
import com.example.dietpal.recipes.*;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class TrackerTest {
    private Food food(){Food f=new Food();f.name="Synthetic nutrition fixture";f.kcal=200d;f.protein=10d;f.carbs=20d;f.fat=8d;return f;}
    @Test public void gramsAndPortionsUseTheirDeclaredBasis(){Food f=food();assertEquals(300d,f.forAmount(f.kcal,150),0.00001);assertEquals(15d,f.forAmount(f.protein,150),0.00001);f.unit="portion";assertEquals(500d,f.forAmount(f.kcal,2.5),0.00001);}
    @Test public void unknownNutritionIsDistinctFromZero(){Food f=food();assertNull(f.forAmount(null,50));assertEquals(0d,f.forAmount(0d,50),0);String json="{\"products\":[{\"product_name\":\"Fixture\",\"nutriments\":{\"energy-kcal_100g\":0,\"proteins_100g\":0}}]}";Food parsed=FoodApi.parse(new JsonParser().parse(json).getAsJsonObject(),false).get(0);assertEquals(0d,parsed.kcal,0);assertEquals(0d,parsed.protein,0);assertNull(parsed.carbs);assertNull(parsed.fat);}
    @Test public void invalidNutritionAndPortionsCannotBeStored(){Food f=food();for(double value:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY})assertThrows(IllegalArgumentException.class,()->f.validate(value));f.kcal=null;assertThrows(IllegalArgumentException.class,()->f.validate(100));f.kcal=200d;f.protein=101d;assertThrows(IllegalArgumentException.class,()->f.validate(100));}
    @Test public void diarySnapshotsValuesAndSurvivesReopen(){
        TrackerStore.Entry first;
        try(TrackerStore store=new TrackerStore(RuntimeEnvironment.getApplication())){Food f=food();first=store.add("2026-01-01","Breakfast",f,150);f.kcal=999d;store.add("2026-01-02","Lunch",food(),200);assertEquals(300d,store.entries("2026-01-01").get(0).calories(),0.0001);}
        try(TrackerStore reopened=new TrackerStore(RuntimeEnvironment.getApplication())){assertEquals(1,reopened.entries("2026-01-01").size());assertEquals(300d,reopened.entries("2026-01-01").get(0).calories(),0.0001);reopened.amount(first,50);assertEquals(100d,reopened.entries("2026-01-01").get(0).calories(),0.0001);reopened.remove(first.id);assertTrue(reopened.entries("2026-01-01").isEmpty());assertEquals(1,reopened.entries("2026-01-02").size());}
    }
    @Test public void partialMacrosRemainMarkedInTotals(){try(TrackerStore store=new TrackerStore(RuntimeEnvironment.getApplication())){Food f=food();f.fat=null;store.add("2026-01-03","Dinner",f,150);TrackerStore.Totals t=new TrackerStore.Totals(store.entries("2026-01-03"));assertEquals(300d,t.kcal,0.0001);assertEquals(15d,t.protein,0.0001);assertTrue(t.missingFat);assertFalse(t.missingProtein);}}
    @Test public void waterWeightsAndGoalsPersistWithoutCrossingDates(){try(TrackerStore store=new TrackerStore(RuntimeEnvironment.getApplication())){store.changeWater("2026-01-04",250);store.changeWater("2026-01-04",250);store.changeWater("2026-01-05",-250);assertEquals(500,store.water("2026-01-04"));assertEquals(0,store.water("2026-01-05"));store.weight("2026-01-04",72.5);assertEquals(72.5,store.weights().get("2026-01-04"),0);assertThrows(IllegalArgumentException.class,()->store.weight("2026-01-05",Double.NaN));store.goals(2000,100,250,70,2000);assertEquals(2000d,store.goal("kcal"),0);assertThrows(IllegalArgumentException.class,()->store.goals(-1,0,0,0,0));}}
    @Test public void exportIncludesRecordedSourcesAndDeleteClearsOnlyTrackingStore(){try(TrackerStore store=new TrackerStore(RuntimeEnvironment.getApplication())){store.add("2026-01-01","Breakfast",food(),100);String json=new com.google.gson.Gson().toJson(store.export());assertTrue(json.contains("Manual entry"));assertTrue(json.contains("Synthetic nutrition fixture"));store.clear();assertTrue(store.entries("2026-01-01").isEmpty());assertTrue(store.recent().isEmpty());assertTrue(store.weights().isEmpty());}}
    @Test public void startersSavedPlansAndChecklistSurviveNewStoreInstances(){
        RecipeStore first=new RecipeStore(RuntimeEnvironment.getApplication());List<Meal> starters=first.starters();assertEquals(6,starters.size());for(Meal meal:starters)assertTrue(meal.isComplete());Meal meal=starters.get(0);
        first.setSaved(meal,true);first.plan("2026-01-06","Dinner",meal);first.addIngredients(meal);first.addIngredients(meal);
        RecipeStore second=new RecipeStore(RuntimeEnvironment.getApplication());assertTrue(second.isSaved(meal.id));assertEquals(meal.id,second.plans().get("2026-01-06:Dinner").id);assertEquals(meal.ingredients.size(),second.shopping().size());String key=second.shopping().keySet().iterator().next();second.checkItem(key,true);second.clearChecked();assertEquals(meal.ingredients.size()-1,second.shopping().size());
    }
    @Test public void mealParserRetainsMeasuresAndRejectsUntrustedImageHosts(){String json="{\"meals\":[{\"idMeal\":\"123\",\"strMeal\":\"Fixture meal\",\"strIngredient1\":\"beans\",\"strMeasure1\":\"1 tin\",\"strInstructions\":\"Rinse.\\r\\nToss.\",\"strMealThumb\":\"https://bad.example/image.jpg\"}]}";Meal meal=MealParser.parse(new JsonParser().parse(json).getAsJsonObject(),"Vegetarian").get(0);assertEquals(Arrays.asList("1 tin beans"),meal.ingredients);assertEquals(Arrays.asList("Rinse.","Toss."),meal.steps);assertEquals("",meal.image);assertEquals("Vegetarian",meal.category);assertEquals(0,MealParser.parse(new JsonParser().parse("{\"meals\":null}").getAsJsonObject(),"All").size());}
}
