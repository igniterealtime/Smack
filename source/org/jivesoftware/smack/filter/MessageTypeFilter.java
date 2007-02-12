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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Filters for packets of a specific type of Message (e.g. CHAT).
 * 
 * @see org.jivesoftware.smack.packet.Message.Type
 * @author Ward Harold
 */
public class MessageTypeFilter implements PacketFilter {

    private final Message.Type type;

    /**
     * Creates a new message type filter using the specified message type.
     * 
     * @param type the message type.
     */
    public MessageTypeFilter(Message.Type type) {
        this.type = type;
    }

    public boolean accept(Packet packet) {
        if (!(packet instanceof Message)) {
            return false;
        }
        else {
            return ((Message) packet).getType().equals(this.type);
        }
    }

}
