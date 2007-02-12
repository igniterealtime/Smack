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
 * Defines a way to filter packets for particular attributes. Packet filters are
 * used when constructing packet listeners or collectors -- the filter defines
 * what packets match the criteria of the collector or listener for further
 * packet processing.<p>
 *
 * Several pre-defined filters are defined. These filters can be logically combined
 * for more complex packet filtering by using the
 * {@link org.jivesoftware.smack.filter.AndFilter AndFilter} and
 * {@link org.jivesoftware.smack.filter.OrFilter OrFilter} filters. It's also possible
 * to define your own filters by implementing this interface. The code example below
 * creates a trivial filter for packets with a specific ID.
 *
 * <pre>
 * // Use an anonymous inner class to define a packet filter that returns
 * // all packets that have a packet ID of "RS145".
 * PacketFilter myFilter = new PacketFilter() {
 *     public boolean accept(Packet packet) {
 *         return "RS145".equals(packet.getPacketID());
 *     }
 * };
 * // Create a new packet collector using the filter we created.
 * PacketCollector myCollector = packetReader.createPacketCollector(myFilter);
 * </pre>
 *
 * @see org.jivesoftware.smack.PacketCollector
 * @see org.jivesoftware.smack.PacketListener
 * @author Matt Tucker
 */
public interface PacketFilter {

    /**
     * Tests whether or not the specified packet should pass the filter.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    public boolean accept(Packet packet);
}
