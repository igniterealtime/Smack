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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.sid.StableUniqueStanzaIdManager;

public class StanzaIdElement implements ExtensionElement {

    public static final String ELEMENT = "stanza-id";
    public static final String ATTR_ID = "id";
    public static final String ATTR_BY = "by";

    private final String id;
    private final String by;

    public StanzaIdElement(String id, String by) {
        this.id = id;
        this.by = by;
    }

    public String getId() {
        return id;
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
    public XmlStringBuilder toXML() {
        return new XmlStringBuilder(this)
                .attribute(ATTR_ID, getId())
                .attribute(ATTR_BY, getBy())
                .closeEmptyElement();
    }
}
