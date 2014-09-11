/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.tcp.sm.predicates;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;

public class OnceForThisStanza implements PacketFilter {

    private final String id;
    private final XMPPTCPConnection connection;

    public static void setup(XMPPTCPConnection connection, Packet packet) {
        PacketFilter packetFilter = new OnceForThisStanza(connection, packet);
        connection.addRequestAckPredicate(packetFilter);
    }

    private OnceForThisStanza(XMPPTCPConnection connection, Packet packet) {
        this.connection = connection;
        this.id = packet.getPacketID();
        if (StringUtils.isNullOrEmpty(id)) {
            throw new IllegalArgumentException("Stanza ID must be set");
        }
    }

    @Override
    public boolean accept(Packet packet) {
        String otherId = packet.getPacketID();
        if (StringUtils.isNullOrEmpty(otherId)) {
            return false;
        }
        if (id.equals(otherId)) {
            connection.removeRequestAckPredicate(this);
            return true;
        }
        return false;
    }

}
