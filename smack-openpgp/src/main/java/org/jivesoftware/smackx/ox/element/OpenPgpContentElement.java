/**
 *
 * Copyright 2017 Florian Schmaus.
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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppDateTime;

public abstract class OpenPgpContentElement implements ExtensionElement {

    private final List<Jid> to;
    private final Date timestamp;
    private final List<ExtensionElement> payload;

    private String timestampString;

    protected OpenPgpContentElement(List<Jid> to, Date timestamp, List<ExtensionElement> payload) {
        this.to = to;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public final List<Jid> getTo() {
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
            xml.halfOpenElement("to").attribute("jid", toJid).closeEmptyElement();
        }

        ensureTimestampStringSet();
        xml.halfOpenElement("time").attribute("stamp", timestampString).closeEmptyElement();

        xml.openElement("payload");
        for (ExtensionElement element : payload) {
            xml.append(element.toXML());
        }
        xml.closeElement("payload");
    }
}
