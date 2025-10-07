/*
 *
 * Copyright 2018 Paul Schaub, 2021 Florian Schmaus
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
package org.jivesoftware.smackx.sid.element;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.sid.StableUniqueStanzaIdManager;

public class OriginIdElement extends StableAndUniqueIdElement {

    public static final String ELEMENT = "origin-id";
    public static final String NAMESPACE = StableUniqueStanzaIdManager.NAMESPACE;
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    public OriginIdElement() {
        super();
    }

    public OriginIdElement(String id) {
        super(id);
    }

    /**
     * Add an origin-id element to a message and set the stanzas id to the same id as in the origin-id element.
     *
     * @param messageBuilder the message builder to add an origin ID to.
     * @return the added origin-id element.
     */
    public static OriginIdElement addTo(MessageBuilder messageBuilder) {
        OriginIdElement originId = messageBuilder.getExtension(OriginIdElement.class);
        if (originId != null) {
            return originId;
        }

        originId = new OriginIdElement();
        messageBuilder.addExtension(originId);
        // TODO: Find solution to have both the originIds stanzaId and a nice to look at incremental stanzaID.
        // message.setStanzaId(originId.getId());
        return originId;
    }

    /**
     * Return true, if the message contains a origin-id element.
     *
     * @param message message
     * @return true if the message contains a origin-id, false otherwise.
     */
    public static boolean hasOriginId(Message message) {
        return getOriginId(message) != null;
    }

    /**
     * Return the origin-id element of a message or null, if absent.
     *
     * @param message message
     * @return origin-id element
     */
    public static OriginIdElement getOriginId(Message message) {
        return (OriginIdElement) message.getExtensionElement(OriginIdElement.ELEMENT, StableUniqueStanzaIdManager.NAMESPACE);
    }

    @Override
    public String getNamespace() {
        return StableUniqueStanzaIdManager.NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_ID, getId())
                .closeEmptyElement();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof OriginIdElement)) {
            return false;
        }

        OriginIdElement otherId = (OriginIdElement) other;
        return getId().equals(otherId.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
