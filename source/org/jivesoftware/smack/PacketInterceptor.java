/**
 * $Revision: 2408 $
 * $Date: 2004-11-02 20:53:30 -0300 (Tue, 02 Nov 2004) $
 *
 * Copyright 2003-2005 Jive Software.
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
 * Provides a mechanism to intercept and modify packets that are going to be
 * sent to the server. PacketInterceptors are added to the {@link XMPPConnection}
 * together with a {@link org.jivesoftware.smack.filter.PacketFilter} so that only
 * certain packets are intercepted and processed by the interceptor.<p>
 *
 * This allows event-style programming -- every time a new packet is found,
 * the {@link #interceptPacket(Packet)} method will be called.
 *
 * @see XMPPConnection#addPacketWriterInterceptor(PacketInterceptor, org.jivesoftware.smack.filter.PacketFilter)
 * @author Gaston Dombiak
 */
public interface PacketInterceptor {

    /**
     * Process the packet that is about to be sent to the server. The intercepted
     * packet can be modified by the interceptor.<p>
     *
     * Interceptors are invoked using the same thread that requested the packet
     * to be sent, so it's very important that implementations of this method
     * not block for any extended period of time.
     *
     * @param packet the packet to is going to be sent to the server.
     */
    public void interceptPacket(Packet packet);
}
