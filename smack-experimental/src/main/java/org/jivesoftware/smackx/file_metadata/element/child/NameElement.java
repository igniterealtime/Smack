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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The name of the file.
 */
public class NameElement implements NamedElement {

    public static final String ELEMENT = "name";

    private final String name;

    public NameElement(String name) {
        this.name = StringUtils.requireNotNullNorEmpty(name, "Name MUST NOT be null nor empty");
    }

    /**
     * Return the name of the file.
     *
     * @return escaped name
     */
    public String getName() {
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e); // UTF-8 MUST be supported
        }
    }

    /**
     * Return the text value of this element in its raw form.
     * Note: Use {@link #getName()} instead when interpreting the name inside a directory structure.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0446.html#security">XEP-0446 §3. Security Considerations</a>
     * @return raw unescaped name
     */
    public String getRawName() {
        return name;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .rightAngleBracket()
                .append(getName())
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
                .append(getRawName())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder.append(getElementName(), other.getElementName())
                        .append(getRawName(), other.getRawName()));
    }

}
