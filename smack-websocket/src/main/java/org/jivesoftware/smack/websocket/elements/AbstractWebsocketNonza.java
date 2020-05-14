/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.websocket.elements;

import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.DomainBareJid;

public abstract class AbstractWebsocketNonza implements Nonza {
    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-framing";
    private static final String VERSION = "1.0";
    private final DomainBareJid to;

    public AbstractWebsocketNonza(DomainBareJid jid) {
        this.to = jid;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        xml.attribute("to", to.toString());
        xml.attribute("version", VERSION);
        xml.closeEmptyElement();
        return xml;
    }
}
