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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.caps.EntityCapsManager;

public class CapsExtension implements PacketExtension {

    private final String node, ver, hash;

    public CapsExtension(String node, String version, String hash) {
        this.node = node;
        this.ver = version;
        this.hash = hash;
    }

    public String getElementName() {
        return EntityCapsManager.ELEMENT;
    }

    public String getNamespace() {
        return EntityCapsManager.NAMESPACE;
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

    /*
     *  <c xmlns='http://jabber.org/protocol/caps'
     *  hash='sha-1'
     *  node='http://code.google.com/p/exodus'
     *  ver='QgayPKawpkPSDYmwT/WM94uAlu0='/>
     *
     */
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("hash", hash).attribute("node", node).attribute("ver", ver);
        xml.closeEmptyElement();
        return xml;
    }
}
