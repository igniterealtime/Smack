/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.element;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppDateTime;

/**
 * This class describes an OpenPGP content element. It defines the elements and fields that OpenPGP content elements
 * do have in common.
 */
public abstract class OpenPgpContentElement implements ExtensionElement {

    public static final String ELEM_TO = "to";
    public static final String ATTR_JID = "jid";
    public static final String ELEM_TIME = "time";
    public static final String ATTR_STAMP = "stamp";
    public static final String ELEM_PAYLOAD = "payload";

    private final Set<Jid> to;
    private final Date timestamp;
    private final List<ExtensionElement> payload;

    private String timestampString;

    protected OpenPgpContentElement(Set<Jid> to, Date timestamp, List<ExtensionElement> payload) {
        this.to = to;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public final Set<Jid> getTo() {
        return to;
    }

    public final Date getTimestamp() {
        return timestamp;
    }

    public final List<ExtensionElement> getPayload() {
        return payload;
    }

    @Override
    public String getNamespace() {
        return OpenPgpElement.NAMESPACE;
    }

    protected void ensureTimestampStringSet() {
        if (timestampString != null) return;

        timestampString = XmppDateTime.formatXEP0082Date(timestamp);
    }

    protected void addCommonXml(XmlStringBuilder xml) {
        for (Jid toJid : to) {
            xml.halfOpenElement(ELEM_TO).attribute(ATTR_JID, toJid).closeEmptyElement();
        }

        ensureTimestampStringSet();
        xml.halfOpenElement(ELEM_TIME).attribute(ATTR_STAMP, timestampString).closeEmptyElement();

        xml.openElement(ELEM_PAYLOAD);
        for (ExtensionElement element : payload) {
            xml.append(element.toXML(getNamespace()));
        }
        xml.closeElement(ELEM_PAYLOAD);
    }
}
