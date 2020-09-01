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

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.AbstractStreamOpen;
import org.jivesoftware.smack.packet.StreamOpen.StreamContentNamespace;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.DomainBareJid;

public final class WebSocketOpenElement extends AbstractStreamOpen {
    public static final String ELEMENT = "open";
    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-framing";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    public WebSocketOpenElement(DomainBareJid to) {
        super(to, null, null, null, StreamContentNamespace.client);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public CharSequence toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        addCommonAttributes(xml);
        xml.closeEmptyElement();
        return xml;
    }
}
