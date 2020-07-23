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
package org.jivesoftware.smackx.stanza_content_encryption.element;

import java.util.Date;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class TimestampAffixElement implements NamedElement, AffixElement {

    public static final String ELEMENT = "time";
    public static final String ATTR_STAMP = "stamp";

    private final Date timestamp;

    public TimestampAffixElement(Date timestamp) {
        this.timestamp = Objects.requireNonNull(timestamp, "Date must not be null.");
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_STAMP, getTimestamp())
                .closeEmptyElement();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (e, o) -> e.append(getTimestamp(), o.getTimestamp()));
    }

    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }
}
