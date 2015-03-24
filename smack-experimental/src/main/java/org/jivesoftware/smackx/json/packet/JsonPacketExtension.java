/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.json.packet;

import org.jivesoftware.smack.packet.Stanza;

/**
 * XMPP JSON Containers as defined in XEP-0335
 *
 * @see <a href="http://xmpp.org/extensions/xep-0335.html">XEP-0335: JSON Containers</a>
 */
public class JsonPacketExtension extends AbstractJsonPacketExtension {

    public static final String ELEMENT = "json";
    public static final String NAMESPACE = "urn:xmpp:json:0";

    public JsonPacketExtension(String json) {
        super(json);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    /**
     * Retrieve the JSON stanza(/packet) extension from the packet.
     *
     * @param packet
     * @return the JSON stanza(/packet) extension or null.
     */
    public static JsonPacketExtension from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }
}
