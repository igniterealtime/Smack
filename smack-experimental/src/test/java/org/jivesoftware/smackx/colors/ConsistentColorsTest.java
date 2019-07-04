/**
 *
 * Copyright © 2018-2019 Paul Schaub
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.colors.ConsistentColor.Deficiency;

import org.junit.jupiter.api.Test;

public class ConsistentColorsTest extends SmackTestSuite {

    // Margin of error we allow due to floating point arithmetic
    private static final float EPS = 0.001f;

    private static final ConsistentColor.ConsistentColorSettings noDeficiency = new ConsistentColor.ConsistentColorSettings(Deficiency.none);
    private static final ConsistentColor.ConsistentColorSettings redGreenDeficiency = new ConsistentColor.ConsistentColorSettings(Deficiency.redGreenBlindness);
    private static final ConsistentColor.ConsistentColorSettings blueBlindnessDeficiency = new ConsistentColor.ConsistentColorSettings(Deficiency.blueBlindness);

    private static final String romeo = "Romeo";
    private static final String juliet = "juliet@capulet.lit";
    private static final String emoji = "\uD83D\uDE3A";
    private static final String council = "council";

    /*
    Below tests check the test vectors from XEP-0392 §13.
     */

    @Test
    public void romeoNoDeficiencyTest() {
        float[] rgb = new float[] {0.865f, 0.000f, 0.686f};
        assertRGBEquals(rgb, ConsistentColor.RGBFrom(romeo), EPS);
    }

    @Test
    public void romeoRedGreenBlindnessTest() {
        float[] expected = new float[] {0.865f, 0.000f, 0.686f};
        float[] actual = ConsistentColor.RGBFrom(romeo, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void romeoBlueBlindnessTest() {
        float[] expected = new float[] {0.000f, 0.535f, 0.350f};
        float[] actual = ConsistentColor.RGBFrom(romeo, blueBlindnessDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void julietNoDeficiencyTest() {
        float[] expected = new float[] {0.000f, 0.515f, 0.573f};
        float[] actual = ConsistentColor.RGBFrom(juliet, noDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void julietRedGreenBlindnessTest() {
        float[] expected = new float[] {0.742f, 0.359f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(juliet, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void julietBlueBlindnessTest() {
        float[] expected = new float[] {0.742f, 0.359f, 0.000f};
        float[] actual = ConsistentColor.RGBFrom(juliet, blueBlindnessDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void emojiNoDeficiencyTest() {
        float[] expected = new float[] {0.872f, 0.000f, 0.659f};
        float[] actual = ConsistentColor.RGBFrom(emoji, noDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void emojiRedGreenBlindnessTest() {
        float[] expected = new float[] {0.872f, 0.000f, 0.659f};
        float[] actual = ConsistentColor.RGBFrom(emoji, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void emojiBlueBlindnessTest() {
        float[] expected = new float[] {0.000f, 0.533f, 0.373f};
        float[] actual = ConsistentColor.RGBFrom(emoji, blueBlindnessDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void councilNoDeficiencyTest() {
        float[] expected = new float[] {0.918f, 0.000f, 0.394f};
        float[] actual = ConsistentColor.RGBFrom(council, noDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void councilRedGreenBlindnessTest() {
        float[] expected = new float[] {0.918f, 0.000f, 0.394f};
        float[] actual = ConsistentColor.RGBFrom(council, redGreenDeficiency);
        assertRGBEquals(expected, actual, EPS);
    }

    @Test
    public void councilBlueBlindnessTest() {
        float[] expected = new float[] {0.000f, 0.524f, 0.485f};
        float[] actual = ConsistentColor.RGBFrom(council, blueBlindnessDeficiency);
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
            assertEquals(expected[i], actual[i], eps);
        }
    }
}
