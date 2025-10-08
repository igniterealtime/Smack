/*
 *
 * Copyright © 2015-2024 Florian Schmaus
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
package org.jivesoftware.smack.util;

public class NumberUtil {

    /**
     * Checks if the given long is within the range of an unsigned 32-bit integer, the XML type "xs:unsignedInt".
     *
     * @param value the long to check.
     * @return the input value.
     */
    public static long requireUInt32(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("unsigned 32-bit integers can't be negative: " + value);
        }
        if (value > ((1L << 32) - 1)) {
            throw new IllegalArgumentException("unsigned 32-bit integers can't be greater than 2^32 - 1: " + value);
        }
        return value;
    }

    /**
     * Checks if the given int is within the range of an unsigned 16-bit integer, the XML type "xs:unsignedShort".
     *
     * @param value the int to check.
     * @return the input value.
     */
    public static int requireUShort16(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("unsigned 16-bit integers can't be negative: " + value);
        }
        if (value > ((1 << 16) - 1)) {
            throw new IllegalArgumentException("unsigned 16-bit integers can't be greater than 2^16 - 1: " + value);
        }
        return value;
    }
}
