package com.example.dietpal;

import static org.junit.Assert.*;

import com.example.dietpal.tracker.Food;
import com.example.dietpal.tracker.FoodApi;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class FoodApiSchemaTest {
  @Test
  public void actualStagingStructuredResponseDoesNotLoseValidNutrition() throws Exception {
    try (InputStreamReader reader =
        new InputStreamReader(
            getClass().getResourceAsStream("/off_staging_v3_6_nutrition.json"),
            StandardCharsets.UTF_8)) {
      Food food = FoodApi.parse(JsonParser.parseReader(reader).getAsJsonObject(), true).get(0);
      assertEquals(539, food.kcal, 0);
      assertEquals(6.3, food.protein, 0);
      assertEquals(30.9, food.fat, 0);
      assertEquals(57.5, food.availableCarbs, 0);
      assertNull("Fibre is absent, so total carbs cannot be invented", food.carbs);
      assertEquals("g", food.unit);
    }
  }

  @Test
  public void explicitLiquidBasisScalesByVolumeWithoutAssumingDensity() {
    String json =
        "{\"product\":{\"product_name\":\"Synthetic"
            + " drink\",\"nutrition\":{\"aggregated_set\":{\"per\":\"100ml\",\"preparation\":\"as_sold\",\"nutrients\":{\"energy-kcal\":{\"unit\":\"kcal\",\"value\":50},\"proteins\":{\"unit\":\"g\",\"value\":3},\"carbohydrates\":{\"unit\":\"g\",\"value\":5},\"fiber\":{\"unit\":\"g\",\"value\":1},\"fat\":{\"unit\":\"g\",\"value\":2}}}}}}";
    Food food = FoodApi.parse(JsonParser.parseString(json).getAsJsonObject(), true).get(0);
    assertEquals("ml", food.unit);
    assertEquals(125, food.forAmount(food.kcal, 250), 0);
    assertEquals(6, food.carbs, 0);
    assertEquals(15, food.forAmount(food.carbs, 250), 0);
    food.validate(250);
  }

  @Test
  public void unknownBasisAndUnknownUnitsCannotProduceFalselyUsableCalories() {
    String json =
        "{\"product\":{\"product_name\":\"Fixture\",\"nutrition\":{\"aggregated_set\":{\"per\":\"unknown\",\"nutrients\":{\"energy-kcal\":{\"unit\":\"kcal\",\"value\":50}}}}}}";
    Food food = FoodApi.parse(JsonParser.parseString(json).getAsJsonObject(), true).get(0);
    assertNull(food.kcal);
    assertThrows(IllegalArgumentException.class, () -> food.validate(100));
  }
}
