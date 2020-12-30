/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.url_address_information.http.element;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.url_address_information.element.MetaInformationElement;

public abstract class NameValuePairElement implements MetaInformationElement {

    public static final String ATTR_NAME = "name";
    public static final String ATTR_VALUE = "value";

    private final String name;
    private final String value;

    public NameValuePairElement(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public XmlStringBuilder addCommonXml(XmlStringBuilder sb) {
        return sb.attribute(ATTR_NAME, getName())
                .attribute(ATTR_VALUE, getValue());
    }

}
