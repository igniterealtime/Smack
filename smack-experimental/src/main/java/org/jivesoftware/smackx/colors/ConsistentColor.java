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

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.SHA1;

public class ConsistentColor {

    private static final ConsistentColorSettings DEFAULT_SETTINGS = new ConsistentColorSettings();

    // See XEP-0392 §13.1 Constants for YCbCr (BT.601)
    private static final double KR = 0.299;
    private static final double KG = 0.587;
    private static final double KB = 0.114;

    // See XEP-0392 §5.4 CbCr to RGB
    private static final double Y = 0.732;

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
     * Generate an angle in the CbCr plane from the input string.
     * @see <a href="https://xmpp.org/extensions/xep-0392.html#algorithm-angle">§5.1: Angle generation</a>
     *
     * @param input input string
     * @return output angle
     */
    private static double createAngle(CharSequence input) {
        byte[] h = SHA1.bytes(input.toString());
        double v = u(h[0]) + (256 * u(h[1]));
        double d = v / 65536;
        return d * 2 * Math.PI;
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
                angle %= Math.PI;
                break;
            case blueBlindness:
                angle -= Math.PI / 2;
                angle %= Math.PI;
                angle += Math.PI / 2;
                break;
        }
        return angle;
    }

    /**
     * Convert an angle in the CbCr plane to values cb, cr in the YCbCr color space.
     * @see <a href="https://xmpp.org/extensions/xep-0392.html#algorithm-cbcr">§5.3: CbCr generation</a>
     *
     * @param angle angel in CbCr plane.
     * @return value pair cb, cr
     */
    private static double[] angleToCbCr(double angle) {
        double cb = Math.cos(angle);
        double cr = Math.sin(angle);

        double acb = Math.abs(cb);
        double acr = Math.abs(cr);
        double factor;
        if (acr > acb) {
            factor = 0.5 / acr;
        } else {
            factor = 0.5 / acb;
        }

        cb *= factor;
        cr *= factor;

        return new double[] {cb, cr};
    }

    /**
     * Convert a value pair cb, cr in the YCbCr color space to RGB.
     * @see <a href="https://xmpp.org/extensions/xep-0392.html#algorithm-rgb">§5.4: CbCr to RGB</a>
     *
     * @param cbcr value pair from the YCbCr color space
     * @return RGB value triple (R, G, B in [0,1])
     */
    private static float[] CbCrToRGB(double[] cbcr, double y) {
        double cb = cbcr[0];
        double cr = cbcr[1];

        double r = 2 * (1 - KR) * cr + y;
        double b = 2 * (1 - KB) * cb + y;
        double g = (y - KR * r - KB * b) / KG;

        // Clip values to [0,1]
        r = clip(r);
        g = clip(g);
        b = clip(b);

        return new float[] {(float) r, (float) g, (float) b};
    }

    /**
     * Clip values to stay in range(0,1).
     *
     * @param value input
     * @return input clipped to stay in boundaries from 0 to 1.
     */
    private static double clip(double value) {
        double out = value;

        if (value < 0) {
            out = 0;
        }

        if (value > 1) {
            out = 1;
        }

        return out;
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
        double[] CbCr = angleToCbCr(correctedAngle);
        float[] rgb = CbCrToRGB(CbCr, Y);
        return rgb;
    }

    public static class ConsistentColorSettings {

        private final Deficiency deficiency;

        public ConsistentColorSettings() {
            this(Deficiency.none);
        }

        public ConsistentColorSettings(Deficiency deficiency) {
            this.deficiency = Objects.requireNonNull(deficiency, "Deficiency must be given");
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
