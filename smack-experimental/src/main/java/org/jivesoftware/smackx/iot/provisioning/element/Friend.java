/**
 *
 * Copyright © 2016-2020 Florian Schmaus
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
package org.jivesoftware.smackx.iot.provisioning.element;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.BareJid;

public class Friend implements ExtensionElement {

    public static final String ELEMENT = "friend";
    public static final String NAMESPACE = Constants.IOT_PROVISIONING_NAMESPACE;
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final BareJid friend;

    public Friend(BareJid friend) {
        this.friend = Objects.requireNonNull(friend, "Friend must not be null");
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
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("jid", friend);
        xml.closeEmptyElement();
        return xml;
    }

    public BareJid getFriend() {
        return friend;
    }

    public static Friend from(Message message) {
        return message.getExtension(Friend.class);
    }
}
