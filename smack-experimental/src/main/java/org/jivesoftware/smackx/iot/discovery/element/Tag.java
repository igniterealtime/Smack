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
package org.jivesoftware.smackx.iot.discovery.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class Tag implements NamedElement {

    public enum Type {
        str,
        num
    }

    private final String name;
    private final Type type;
    private final String value;

    public Tag(String name, Type type, String value) {
        // TODO According to XEP-0347 § 5.2 names are case insensitive. Uppercase them all?
        this.name = StringUtils.requireNotNullNorEmpty(name, "name must not be null nor empty");
        this.type = Objects.requireNonNull(type);
        this.value =  StringUtils.requireNotNullNorEmpty(value, "value must not be null nor empty");
        if (this.name.length() > 32) {
            throw new IllegalArgumentException("Meta Tag names must not be longer then 32 characters (XEP-0347 § 5.2");
        }
        if (this.type == Type.str && this.value.length() > 128) {
            throw new IllegalArgumentException("Meta Tag string values must not be longer then 128 characters (XEP-0347 § 5.2");
        }
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("name", name);
        xml.attribute("value", value);
        xml.closeEmptyElement();
        return xml;
    }

    @Override
    public String getElementName() {
        return getType().toString();
    }

    @Override
    public String toString() {
        return name + '(' + type + "):" + value;
    }
}
