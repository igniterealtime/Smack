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

import java.util.Locale;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public abstract class SetData implements NamedElement {

    public enum Type {
        BOOL,
        INT,
        LONG,
        DOUBLE,
        ;

        private final String toStringCache;

        private Type() {
            toStringCache = this.name().toLowerCase(Locale.US);
        }

        @Override
        public String toString() {
            return toStringCache;
        }
    }

    protected SetData(String name, Type type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    private final String name;

    private final Type type;

    private final String value;

    public final String getName() {
        return name;
    }

    public final String getValue() {
        return value;
    }

    public final Type getType() {
        return type;
    }

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    @Override
    public final String getElementName() {
        return getType().toString();
    }

    /**
     * Returns the XML representation of this Element.
     *
     * @return the stanza(/packet) extension as XML.
     */
    @Override
    public final XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("name", name);
        xml.attribute("value", value);
        xml.closeEmptyElement();
        return xml;
    }
}
