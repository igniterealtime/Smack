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

package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Packet;

/**
 * Provides a mechanism to listen for packets that pass a specified filter.
 * This allows event-style programming -- every time a new packet is found,
 * the {@link #processPacket(Packet)} method will be called. This is the
 * opposite approach to the functionality provided by a {@link PacketCollector}
 * which lets you block while waiting for results.
 *
 * @see Connection#addPacketListener(PacketListener, org.jivesoftware.smack.filter.PacketFilter)
 * @author Matt Tucker
 */
public interface PacketListener {

    /**
     * Process the next packet sent to this packet listener.<p>
     *
     * A single thread is responsible for invoking all listeners, so
     * it's very important that implementations of this method not block
     * for any extended period of time.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet);

}
