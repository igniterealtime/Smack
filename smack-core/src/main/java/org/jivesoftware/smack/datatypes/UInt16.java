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
 * A number representing an unsigned 16-bit integer. Can be used for values with the XML schema type "xs:unsingedShort".
 */
public final class UInt16 extends Scalar implements Comparable<UInt16> {

    private static final long serialVersionUID = 1L;

    private final int number;

    public static final int MIN_VALUE_INT = 0;
    public static final int MAX_VALUE_INT = (1 << 16) - 1;

    public static final UInt16 MIN_VALUE = UInt16.from(MIN_VALUE_INT);
    public static final UInt16 MAX_VALUE = UInt16.from(MAX_VALUE_INT);

    private UInt16(int number) {
        super(NumberUtil.requireUShort16(number));
        this.number = number;
    }

    public int nativeRepresentation() {
        return number;
    }

    public static UInt16 from(int number) {
        return new UInt16(number);
    }

    @Override
    public int hashCode() {
        return number;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof UInt16) {
            UInt16 otherUint16 = (UInt16) other;
            return number == otherUint16.number;
        }

        return super.equals(other);
    }

    @Override
    public int compareTo(UInt16 o) {
        return Integer.compare(number, o.number);
    }

    @Override
    public UInt16 getMinValue() {
        return MIN_VALUE;
    }

    @Override
    public UInt16 getMaxValue() {
        return MAX_VALUE;
    }

    @Override
    public UInt16 incrementedByOne() {
        int incrementedValue = number < MAX_VALUE_INT ? number + 1 : 0;
        return UInt16.from(incrementedValue);
    }
}
