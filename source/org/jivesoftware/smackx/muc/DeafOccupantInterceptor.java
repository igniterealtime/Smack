/**
 * $RCSfile$
 * $Revision:  $
 * $Date$
 *
 * Copyright 2003-2006 Jive Software.
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

package org.jivesoftware.smackx.muc;

import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;

/**
 * Packet interceptor that will intercept presence packets sent to the MUC service to indicate
 * that the user wants to be a deaf occupant. A user can only indicate that he wants to be a
 * deaf occupant while joining the room. It is not possible to become deaf or stop being deaf
 * after the user joined the room.<p>
 *
 * Deaf occupants will not get messages broadcasted to all room occupants. However, they will
 * be able to get private messages, presences, IQ packets or room history. To use this
 * functionality you will need to send the message
 * {@link MultiUserChat#addPresenceInterceptor(org.jivesoftware.smack.PacketInterceptor)} and
 * pass this interceptor as the parameter.<p>
 *
 * Note that this is a custom extension to the MUC service so it may not work with other servers
 * than Wildfire.
 *
 * @author Gaston Dombiak
 */
public class DeafOccupantInterceptor implements PacketInterceptor {

    public void interceptPacket(Packet packet) {
        Presence presence = (Presence) packet;
        // Check if user is joining a room
        if (Presence.Type.available == presence.getType() &&
                presence.getExtension("x", "http://jabber.org/protocol/muc") != null) {
            // Add extension that indicates that user wants to be a deaf occupant
            packet.addExtension(new DeafExtension());
        }
    }

    private static class DeafExtension implements PacketExtension {

        public String getElementName() {
            return "x";
        }

        public String getNamespace() {
            return "http://jivesoftware.org/protocol/muc";
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace())
                    .append("\">");
            buf.append("<deaf-occupant/>");
            buf.append("</").append(getElementName()).append(">");
            return buf.toString();
        }
    }
}
