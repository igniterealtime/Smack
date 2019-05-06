/**
 *
 * Copyright © 2018 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.colors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.colors.ConsistentColor.Deficiency;

import org.junit.jupiter.api.Test;

public class ConsistentColorsTest extends SmackTestSuite {

    // Margin of error we allow due to floating point arithmetic
    private static final float EPS = 0.001f;

    private static final ConsistentColor.ConsistentColorSettings noDeficiency = new ConsistentColor.ConsistentColorSettings(Deficiency.none);
    private static final ConsistentColor.ConsistentColorSettings redGreenDeficiency = new ConsistentColor.ConsistentColorSettings(Deficiency.redGreenBlindness);
    private static final ConsistentColor.ConsistentColorSettings blueBlindnessDeficiency = new ConsistentColor.ConsistentColorSettings(Deficiency.blueBlindness);

    /*
    Below tests check the test vectors from XEP-0392 §13.2.
     */

    @Test
    public void romeoNoDeficiencyTest() {
        String value = "Romeo";
        float[] expected = new float[] {0.281f, 0.790f, 1.000f};
        float[] actual = ConsistentColor.RGBFrom(value, noDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void romeoRedGreenBlindnessTest() {
        String value = "Romeo";
        float[] expected = new float[] {1.000f, 0.674f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void romeoBlueBlindnessTest() {
        String value = "Romeo";
        float[] expected = new float[] {1.000f, 0.674f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, blueBlindnessDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void julietNoDeficiencyTest() {
        String value = "juliet@capulet.lit";
        float[] expected = new float[] {0.337f, 1.000f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, noDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void julietRedGreenBlindnessTest() {
        String value = "juliet@capulet.lit";
        float[] expected = new float[] {1.000f, 0.359f, 1.000f};
        float[] actual = ConsistentColor.RGBFrom(value, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void julietBlueBlindnessTest() {
        String value = "juliet@capulet.lit";
        float[] expected = new float[] {0.337f, 1.000f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, blueBlindnessDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void emojiNoDeficiencyTest() {
        String value = "\uD83D\uDE3A";
        float[] expected = new float[] {0.347f, 0.756f, 1.000f};
        float[] actual = ConsistentColor.RGBFrom(value, noDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void emojiRedGreenBlindnessTest() {
        String value = "\uD83D\uDE3A";
        float[] expected = new float[] {1.000f, 0.708f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void emojiBlueBlindnessTest() {
        String value = "\uD83D\uDE3A";
        float[] expected = new float[] {1.000f, 0.708f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, blueBlindnessDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void councilNoDeficiencyTest() {
        String value = "council";
        float[] expected = new float[] {0.732f, 0.560f, 1.000f};
        float[] actual = ConsistentColor.RGBFrom(value, noDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void councilRedGreenBlindnessTest() {
        String value = "council";
        float[] expected = new float[] {0.732f, 0.904f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void councilBlueBlindnessTest() {
        String value = "council";
        float[] expected = new float[] {0.732f, 0.904f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(value, blueBlindnessDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    /**
     * Check, whether the values of two float arrays of size 3 are pairwise equal with an allowed error of eps.
     *
     * @param expected expected values
     * @param actual actual values
     * @param eps allowed error
     */
    private static void assertRGBEquals(float[] expected, float[] actual, float eps) {
        assertEquals(3, expected.length);
        assertEquals(3, actual.length);

        for (int i = 0; i < actual.length; i++) {
            assertTrue(Math.abs(expected[i] - actual[i]) < eps);
        }
    }
}
