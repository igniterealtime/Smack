/**
 *
 * Copyright © 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.control.element;

public class SetIntData extends SetData {

    public SetIntData(String name, int value) {
        this(name, Integer.toString(value));
        integerCache = value;
    }

    protected SetIntData(String name, String value) {
        super(name, Type.INT, value);
    }

    private Integer integerCache;

    public Integer getIntegerValue() {
        if (integerCache != null) {
            integerCache = Integer.valueOf(getValue());
        }
        return integerCache;
    }
}
