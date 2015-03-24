/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Filters for packets with a particular type of packet extension.
 *
 * @author Matt Tucker
 * @deprecated use {@link StanzaExtensionFilter} instead.
 */
@Deprecated
public class PacketExtensionFilter implements StanzaFilter {

    private final String elementName;
    private final String namespace;

    /**
     * Creates a new packet extension filter. Packets will pass the filter if
     * they have a packet extension that matches the specified element name
     * and namespace.
     *
     * @param elementName the XML element name of the packet extension.
     * @param namespace the XML namespace of the packet extension.
     */
    public PacketExtensionFilter(String elementName, String namespace) {
        StringUtils.requireNotNullOrEmpty(namespace, "namespace must not be null or empty");

        this.elementName = elementName;
        this.namespace = namespace;
    }

    /**
     * Creates a new packet extension filter. Packets will pass the filter if they have a packet
     * extension that matches the specified namespace.
     *
     * @param namespace the XML namespace of the packet extension.
     */
    public PacketExtensionFilter(String namespace) {
        this(null, namespace);
    }

    /**
     * Creates a new packet extension filter for the given packet extension.
     *
     * @param packetExtension
     */
    public PacketExtensionFilter(ExtensionElement packetExtension) {
        this(packetExtension.getElementName(), packetExtension.getNamespace());
    }

    public boolean accept(Stanza packet) {
        return packet.hasExtension(elementName, namespace);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": element=" + elementName + " namespace=" + namespace;
    }
}
