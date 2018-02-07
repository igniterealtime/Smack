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

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.sid.StableUniqueStanzaIdManager;

public class OriginIdElement extends StableAndUniqueIdElement {

    public static final String ELEMENT = "origin-id";

    public OriginIdElement() {
        super();
    }

    public OriginIdElement(String id) {
        super(id);
    }

    /**
     * Add an origin-id element to a stanza and set the stanzas id to the same id as in the origin-id element.
     *
     * @param stanza stanza.
     */
    public static void addOriginId(Stanza stanza) {
        OriginIdElement originId = new OriginIdElement();
        stanza.addExtension(originId);
        stanza.setStanzaId(originId.getId());
    }

    /**
     * Return true, if the stanza contains a origin-id element.
     *
     * @param stanza stanza
     * @return true if the stanza contains a origin-id, false otherwise.
     */
    public static boolean hasOriginId(Stanza stanza) {
        return getOriginId(stanza) != null;
    }

    /**
     * Return the origin-id element of a stanza or null, if absent.
     *
     * @param stanza stanza
     * @return origin-id element
     */
    public static OriginIdElement getOriginId(Stanza stanza) {
        return stanza.getExtension(OriginIdElement.ELEMENT, StableUniqueStanzaIdManager.NAMESPACE);
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
    public CharSequence toXML() {
        return new XmlStringBuilder(this)
                .attribute(ATTR_ID, getId())
                .closeEmptyElement();
    }
}
