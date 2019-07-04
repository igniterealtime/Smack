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

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.SHA1;

import org.hsluv.HUSLColorConverter;

/**
 * Implementation of XEP-0392: Consistent Color Generation version 0.6.0.
 *
 * @author Paul Schaub
 */
public class ConsistentColor {

    private static final ConsistentColorSettings DEFAULT_SETTINGS = new ConsistentColorSettings();

    public enum Deficiency {
        /**
         * Do not apply measurements for color vision deficiency correction.
         */
        none,

        /**
         * Activate color correction for users suffering from red-green-blindness.
         */
        redGreenBlindness,

        /**
         * Activate color correction for users suffering from blue-blindness.
         */
        blueBlindness
    }

    /**
     * Generate an angle in the HSLuv color space from the input string.
     * @see <a href="https://xmpp.org/extensions/xep-0392.html#algorithm-angle">§5.1: Angle generation</a>
     *
     * @param input input string
     * @return output angle in degrees
     */
    private static double createAngle(CharSequence input) {
        byte[] h = SHA1.bytes(input.toString());
        double v = u(h[0]) + (256 * u(h[1]));
        double d = v / 65536;
        return d * 360;
    }

    /**
     * Apply correction for color vision deficiencies to an angle in the CbCr plane.
     * @see <a href="https://xmpp.org/extensions/xep-0392.html#algorithm-cvd">§5.2: Corrections for Color Vision Deficiencies</a>
     *
     * @param angle angle in CbCr plane
     * @param deficiency type of vision deficiency
     * @return corrected angle in CbCr plane
     */
    private static double applyColorDeficiencyCorrection(double angle, Deficiency deficiency) {
        switch (deficiency) {
            case none:
                break;
            case redGreenBlindness:
                angle += 90;
                angle %= 180;
                angle += 270; // equivalent to -90 % 360, but eliminates negative results
                angle %= 360;
                break;
            case blueBlindness:
                angle %= 180;
                break;
        }
        return angle;
    }

    /**
     * Converting a HSLuv angle to RGB.
     * Saturation is set to 100 and lightness to 50, according to the XEP.
     *
     * @param hue angle
     * @return rgb values between 0 and 1
     *
     * @see <a href="https://xmpp.org/extensions/xep-0392.html#algorithm-rgb">XEP-0392 §5.4: RGB generation</a>
     */
    private static double[] hsluvToRgb(double hue) {
        return hsluvToRgb(hue, 100, 50);
    }

    /**
     * Converting a HSLuv angle to RGB.
     *
     * @param hue angle 0 <= hue < 360
     * @param saturation saturation 0 <= saturation <= 100
     * @param lightness lightness 0 <= lightness <= 100
     * @return rbg array with values 0 <= (r,g,b) <= 1
     *
     * @see <a href="https://www.rapidtables.com/convert/color/hsl-to-rgb.html">HSL to RGB conversion</a>
     */
    private static double[] hsluvToRgb(double hue, double saturation, double lightness) {
        return HUSLColorConverter.hsluvToRgb(new double[] {hue, saturation, lightness});
    }

    private static double[] mixWithBackground(double[] rgbi, float[] rgbb) {
        return new double[] {
                0.2 * (1 - rgbb[0]) + 0.8 * rgbi[0],
                0.2 * (1 - rgbb[1]) + 0.8 * rgbi[1],
                0.2 * (1 - rgbb[2]) + 0.8 * rgbi[2]
        };
    }

    /**
     * Treat a signed java byte as unsigned to get its numerical value.
     *
     * @param b signed java byte
     * @return integer value of its unsigned representation
     */
    private static int u(byte b) {
        // Get unsigned value of signed byte as an integer.
        return b & 0xFF;
    }

    /**
     * Return the consistent RGB color value of the input.
     * This method uses the default {@link ConsistentColorSettings}.
     *
     * @param input input string (for example username)
     * @return consistent color of that username as RGB values in range [0,1].
     * @see #RGBFrom(CharSequence, ConsistentColorSettings)
     */
    public static float[] RGBFrom(CharSequence input) {
        return RGBFrom(input, DEFAULT_SETTINGS);
    }

    /**
     * Return the consistent RGB color value for the input.
     * This method respects the color vision deficiency mode set by the user.
     *
     * @param input input string (for example username)
     * @param settings the settings for consistent color creation.
     * @return consistent color of that username as RGB values in range [0,1].
     */
    public static float[] RGBFrom(CharSequence input, ConsistentColorSettings settings) {
        double angle = createAngle(input);
        double correctedAngle = applyColorDeficiencyCorrection(angle, settings.getDeficiency());
        double[] rgb = hsluvToRgb(correctedAngle);
        if (settings.backgroundRGB != null) {
            rgb = mixWithBackground(rgb, settings.backgroundRGB);
        }

        return new float[] {(float) rgb[0], (float) rgb[1], (float) rgb[2]};
    }

    public static int[] floatRgbToInts(float[] floats) {
        return new int[] {
                (int) (floats[0] * 255),
                (int) (floats[1] * 255),
                (int) (floats[2] * 255)
        };
    }

    public static class ConsistentColorSettings {

        private final Deficiency deficiency;
        private final float[] backgroundRGB;

        public ConsistentColorSettings() {
            this.deficiency = Deficiency.none;
            this.backgroundRGB = null;
        }

        public ConsistentColorSettings(Deficiency deficiency) {
            this.deficiency = Objects.requireNonNull(deficiency, "Deficiency must be given");
            this.backgroundRGB = null;
        }

        public ConsistentColorSettings(Deficiency deficiency,
                                       float[] backgroundRGB) {
            this.deficiency = Objects.requireNonNull(deficiency, "Deficiency must be given");
            if (backgroundRGB.length != 3) {
                throw new IllegalArgumentException("Background RGB value array must have length 3.");
            }

            for (float f : backgroundRGB) {
                checkRange(f, 0, 1);
            }
            this.backgroundRGB = backgroundRGB;
        }

        private static void checkRange(float value, float lower, float upper) {
            if (lower > value || upper < value) {
                throw new IllegalArgumentException("Value out of range.");
            }
        }

        /**
         * Return the deficiency setting.
         *
         * @return deficiency setting.
         */
        public Deficiency getDeficiency() {
            return deficiency;
        }
    }
}
