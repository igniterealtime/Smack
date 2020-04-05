/**
 *
 * Copyright 2018 Paul Schaub
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
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.sid.StableUniqueStanzaIdManager;

public class StanzaIdElement extends StableAndUniqueIdElement {

    public static final String ELEMENT = "stanza-id";
    public static final String NAMESPACE = StableUniqueStanzaIdManager.NAMESPACE;
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);
    public static final String ATTR_BY = "by";

    private final String by;

    public StanzaIdElement(String by) {
        super();
        this.by = by;
    }

    public StanzaIdElement(String id, String by) {
        super(id);
        this.by = by;
    }

    /**
     * Return true, if a message contains a stanza-id element.
     *
     * @param message message
     * @return true if message contains stanza-id element, otherwise false.
     */
    public static boolean hasStanzaId(Message message) {
        return getStanzaId(message) != null;
    }

    /**
     * Return the stanza-id element of a message.
     *
     * @param message message
     * @return stanza-id element of a jid, or null if absent.
     */
    public static StanzaIdElement getStanzaId(Message message) {
        return message.getExtension(StanzaIdElement.class);
    }

    public String getBy() {
        return by;
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
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_ID, getId())
                .attribute(ATTR_BY, getBy())
                .closeEmptyElement();
    }
}
