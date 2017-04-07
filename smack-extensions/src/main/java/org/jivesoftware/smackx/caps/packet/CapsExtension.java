/**
 *
 * Copyright © 2009 Jonas Ådahl, 2011-2014 Florian Schmaus
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
package org.jivesoftware.smackx.caps.packet;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * A XEP-0115 Entity Capabilities extension.
 */
public class CapsExtension implements ExtensionElement {
    public static final String NAMESPACE = "http://jabber.org/protocol/caps";
    public static final String ELEMENT = "c";

    private final String node, ver, hash;

    public CapsExtension(String node, String version, String hash) {
        this.node = node;
        this.ver = version;
        this.hash = hash;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public String getNode() {
        return node;
    }

    public String getVer() {
        return ver;
    }

    public String getHash() {
        return hash;
    }

    /**
     * {@inheritDoc}.
     *
     * <pre>
     *  <c xmlns='http://jabber.org/protocol/caps'
     *     hash='sha-1'
     *     node='http://code.google.com/p/exodus'
     *     ver='QgayPKawpkPSDYmwT/WM94uAlu0='/>
     * </pre>
     *
     */
    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("hash", hash).attribute("node", node).attribute("ver", ver);
        xml.closeEmptyElement();
        return xml;
    }

    public static CapsExtension from(Stanza stanza) {
        return stanza.getExtension(ELEMENT, NAMESPACE);
    }
}
