package com.example.dietpal;

import static org.junit.Assert.*;
import org.junit.Test;

public class BmiCalculatorTest {
    @Test public void calculatesFromKilogramsAndCentimetres() {
        assertEquals(22.8571428571, BmiCalculator.calculate("70", "175"), 0.00000001);
    }

    @Test public void acceptsDecimalsAndWhitespace() {
        assertEquals(60.5 / Math.pow(1.655, 2), BmiCalculator.calculate(" 60.5 ", "165.5"), 0.00000001);
    }

    @Test public void rejectsInvalidInputsWithoutNonFiniteResults() {
        String[][] invalid = {{"70", "0"}, {"-1", "170"}, {"0", "170"}, {"abc", "170"},
                {"70", ""}, {"NaN", "170"}, {"70", "Infinity"}, {"1e300", "1e-300"},
                {null, "170"}, {"70", "-170"}};
        for (String[] pair : invalid) {
            try {
                BmiCalculator.calculate(pair[0], pair[1]);
                fail("Invalid input should be rejected");
            } catch (IllegalArgumentException expected) { }
        }
    }
}
