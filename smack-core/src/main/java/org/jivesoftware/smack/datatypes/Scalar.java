/**
 *
 * Copyright 2019 Florian Schmaus
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

public abstract class Scalar extends java.lang.Number {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final java.lang.Number number;

    protected Scalar(java.lang.Number number) {
        this.number = number;
    }

    public final Number number() {
        return number;
    }

    @Override
    public final int intValue() {
        return number.intValue();
    }

    @Override
    public final long longValue() {
        return number.longValue();
    }

    @Override
    public final float floatValue() {
        return number.floatValue();
    }

    @Override
    public final double doubleValue() {
        return number.doubleValue();
    }

    @Override
    public abstract int hashCode();

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Scalar)) {
            return false;
        }

        Scalar otherScalar = (Scalar) other;

        if (longValue() == otherScalar.longValue()) {
            return true;
        }

        if (doubleValue() == otherScalar.doubleValue()) {
            return true;
        }

        if (floatValue() == otherScalar.floatValue()) {
            return true;
        }

        return false;
    }

    @Override
    public final String toString() {
        return number.toString();
    }
}
