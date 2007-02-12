/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.Packet;

/**
 * Filters for packets with a particular packet ID.
 *
 * @author Matt Tucker
 */
public class PacketIDFilter implements PacketFilter {

    private String packetID;

    /**
     * Creates a new packet ID filter using the specified packet ID.
     *
     * @param packetID the packet ID to filter for.
     */
    public PacketIDFilter(String packetID) {
        if (packetID == null) {
            throw new IllegalArgumentException("Packet ID cannot be null.");
        }
        this.packetID = packetID;
    }

    public boolean accept(Packet packet) {
        return packetID.equals(packet.getPacketID());
    }

    public String toString() {
        return "PacketIDFilter by id: " + packetID;
    }
}
