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

import org.jivesoftware.smack.util.NumberUtil;

/**
 * A number representing an unsigned 16-bit integer. Can be used for values with the XML schema type "xs:unsingedShort".
 */
public final class UInt16 extends Scalar {

    private static final long serialVersionUID = 1L;

    private final int number;

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
}
