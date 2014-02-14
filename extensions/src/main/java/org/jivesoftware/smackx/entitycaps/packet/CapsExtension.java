/**
 * Copyright 2009 Jonas Ådahl.
 * Copyright 2011-2013 Florian Schmaus
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.entitycaps.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;

public class CapsExtension implements PacketExtension {

    private String node, ver, hash;

    public CapsExtension() {
    }

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

    public void setNode(String node) {
        this.node = node;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    /*
     *  <c xmlns='http://jabber.org/protocol/caps'
     *  hash='sha-1'
     *  node='http://code.google.com/p/exodus'
     *  ver='QgayPKawpkPSDYmwT/WM94uAlu0='/>
     *
     */
    public String toXML() {
        String xml = "<" + EntityCapsManager.ELEMENT + " xmlns=\"" + EntityCapsManager.NAMESPACE + "\" " +
            "hash=\"" + hash + "\" " +
            "node=\"" + node + "\" " +
            "ver=\"" + ver + "\"/>";

        return xml;
    }
}
