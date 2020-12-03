/**
 *
 * Copyright 2019-2020 Florian Schmaus
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
package org.jivesoftware.smack.datatypes;

import org.jivesoftware.smack.util.NumberUtil;

/**
 * A number representing an unsigned 32-bit integer. Can be used for values with the XML schema type "xs:unsignedInt".
 */
public final class UInt32 extends Scalar {

    private static final long serialVersionUID = 1L;

    private final long number;

    public static final long MIN_VALUE_LONG = 0;
    public static final long MAX_VALUE_LONG = (1L << 32) - 1;

    public static final UInt32 MIN_VALUE = UInt32.from(MAX_VALUE_LONG);
    public static final UInt32 MAX_VALUE = UInt32.from(MAX_VALUE_LONG);

    private UInt32(long number) {
        super(NumberUtil.requireUInt32(number));
        this.number = number;
    }

    public long nativeRepresentation() {
        return number;
    }

    public static UInt32 from(long number) {
        return new UInt32(number);
    }

    @Override
    public int hashCode() {
        // TODO: Use Long.hashCode(number) once Smack's minimum Android SDK level is 24 or higher.
        return (int) (number ^ (number >>> 32));
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof UInt32) {
            UInt32 otherUint32 = (UInt32) other;
            return number == otherUint32.number;
        }

        return super.equals(other);
    }

    @Override
    public UInt32 getMinValue() {
        return MIN_VALUE;
    }

    @Override
    public UInt32 getMaxValue() {
        return MAX_VALUE;
    }

    @Override
    public UInt32 incrementedByOne() {
        long incrementedValue = number < MAX_VALUE_LONG ? number + 1 : 0;
        return UInt32.from(incrementedValue);
    }
}
