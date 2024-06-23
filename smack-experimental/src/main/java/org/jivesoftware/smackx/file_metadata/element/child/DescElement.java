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
package org.jivesoftware.smackx.file_metadata.element.child;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * A human readable description of the file. Multiple {@link DescElement DescElements} MAY be included
 * if different xml:lang values are specified.
 */
public class DescElement implements NamedElement {

    public static final String ELEMENT = "desc";

    private final String description;
    private final String lang;

    public DescElement(String description) {
        this(description, null);
    }

    public DescElement(String description, String lang) {
        this.description = StringUtils.requireNotNullNorEmpty(description, "Description MUST NOT be null nor empty");
        this.lang = lang;
    }

    /**
     * Return the description of the file.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return the language of the description or null.
     *
     * @return language or null
     */
    public String getLanguage() {
        return lang;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .optXmlLangAttribute(getLanguage())
                .rightAngleBracket()
                .append(getDescription())
                .closeElement(this);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getLanguage())
                .append(getDescription())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder.append(getElementName(), other.getElementName())
                        .append(getLanguage(), other.getLanguage())
                        .append(getDescription(), other.getDescription()));
    }
}
