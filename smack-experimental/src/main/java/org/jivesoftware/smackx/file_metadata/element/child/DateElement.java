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

import java.util.Date;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.util.XmppDateTime;

/**
 * Timestamp specifying the last modified time of the file.
 */
public class DateElement implements NamedElement {

    public static final String ELEMENT = "date";

    private final Date date;

    public DateElement(Date date) {
        this.date = date;
    }

    /**
     * Return the last modification date of the file.
     *
     * @return last modification date
     */
    public Date getDate() {
        return date;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();
        sb.append(XmppDateTime.formatXEP0082Date(getDate()));
        return sb.closeElement(this);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getDate())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder.append(getElementName(), other.getElementName())
                        .append(getDate(), other.getDate()));
    }
}
