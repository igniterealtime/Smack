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
 * Filters for packets of a particular type. The type is given as a Class object, so
 * example types would:
 * <ul>
 *      <li><tt>Message.class</tt>
 *      <li><tt>IQ.class</tt>
 *      <li><tt>Presence.class</tt>
 * </ul>
 *
 * @author Matt Tucker
 */
public class PacketTypeFilter implements PacketFilter {

    Class packetType;

    /**
     * Creates a new packet type filter that will filter for packets that are the
     * same type as <tt>packetType</tt>.
     *
     * @param packetType the Class type.
     */
    public PacketTypeFilter(Class packetType) {
        // Ensure the packet type is a sub-class of Packet.
        if (!Packet.class.isAssignableFrom(packetType)) {
            throw new IllegalArgumentException("Packet type must be a sub-class of Packet.");
        }
        this.packetType = packetType;
    }

    public boolean accept(Packet packet) {
        return packetType.isInstance(packet);
    }

    public String toString() {
        return "PacketTypeFilter: " + packetType.getName();
    }
}
