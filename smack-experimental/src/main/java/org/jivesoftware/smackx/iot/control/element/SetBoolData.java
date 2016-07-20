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

public class SetBoolData extends SetData {

    public SetBoolData(String name, boolean value) {
        this(name, Boolean.toString(value));
        booleanCache = value;
    }

    protected SetBoolData(String name, String value) {
        super(name, Type.BOOL, value);
    }

    private Boolean booleanCache;

    public Boolean getBooleanValue() {
        if (booleanCache != null) {
            booleanCache = Boolean.valueOf(getValue());
        }
        return booleanCache;
    }
}
