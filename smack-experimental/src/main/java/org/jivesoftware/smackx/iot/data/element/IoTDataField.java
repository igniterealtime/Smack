/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.data.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public abstract class IoTDataField implements NamedElement {

    enum Type {
        integer("int"),
        bool("boolean"),
        ;

        Type(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        private final String stringRepresentation;
    }

    private final Type type;

    private final String name;

    protected IoTDataField(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    @Override
    public final String getElementName() {
        return type.stringRepresentation;
    }

    @Override
    public final XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("name", name).attribute("value", getValueString());
        // TODO handle 'unit' attribute as special case if <numeric/> is implemented.
        xml.closeEmptyElement();
        return xml;
    }

    private String valueString;

    public final String getValueString() {
        if (valueString == null) {
            valueString = getValueInternal();
        }
        return valueString;
    }

    protected abstract String getValueInternal();

    public static class IntField extends IoTDataField {

        private final int value;

        public IntField(String name, int value) {
            super(Type.integer, name);
            this.value = value;
        }

        @Override
        protected String getValueInternal() {
            return Integer.toString(value);
        }

        public int getValue() {
            return value;
        }
    }

    public static class BooleanField extends IoTDataField {

        private final boolean value;

        public BooleanField(String name, boolean value) {
            super(Type.bool, name);
            this.value = value;
        }

        @Override
        protected String getValueInternal() {
            return Boolean.toString(value);
        }

        public boolean getValue() {
            return value;
        }
    }
}
