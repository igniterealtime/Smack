/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.sm;

import java.math.BigInteger;

public class SMUtils {

    private static long MASK_32_BIT = BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE).longValue();

    /**
     * Quoting XEP-198 4.:
     * "In the unlikely case that the number of stanzas handled during a stream management session exceeds the number
     * of digits that can be represented by the unsignedInt datatype as specified in XML Schema Part 2 [10]
     * (i.e., 2^32), the value of 'h' SHALL be reset from 2^32-1 back to zero (rather than being incremented to 2^32)."
     * 
     * @param height
     * @return the incremented height
     */
    public static long incrementHeight(long height) {
        return ++height & MASK_32_BIT;
    }

    /**
     * Calculates the delta of the last known stanza handled count and the new
     * reported stanza handled count while considering that the new value may be
     * wrapped after 2^32-1.
     * 
     * @param reportedHandledCount
     * @param lastKnownHandledCount
     * @return the delta
     */
    public static long calculateDelta(long reportedHandledCount, long lastKnownHandledCount) {
        return (reportedHandledCount - lastKnownHandledCount) & MASK_32_BIT;
    }
}
