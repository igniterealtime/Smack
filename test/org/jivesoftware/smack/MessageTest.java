/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.filter.MessageTypeFilter;

/**
 * Tests sending messages to other clients.
 *
 * @author Gaston Dombiak
 */
public class MessageTest extends SmackTestCase {

    public MessageTest(String arg0) {
        super(arg0);
    }

    /**
     * Check that when a client becomes unavailable all messages sent to the client are stored offline. So that when
     * the client becomes available again the offline messages are received.
     */
    public void testOfflineMessage() {
        // Make user2 unavailable
        getConnection(1).sendPacket(new Presence(Presence.Type.UNAVAILABLE));

        try {
            Thread.sleep(500);

            // User1 sends some messages to User2 which is not available at the moment
            Chat chat = getConnection(0).createChat(getBareJID(1));
            chat.sendMessage("Test 1");
            chat.sendMessage("Test 2");

            Thread.sleep(500);

            // User2 becomes available again
            PacketCollector collector = getConnection(1).createPacketCollector(new MessageTypeFilter(Message.Type.CHAT));
            getConnection(1).sendPacket(new Presence(Presence.Type.AVAILABLE));

            // Check that offline messages are retrieved by user2 which is now available
            Message message = (Message) collector.nextResult(2000);
            assertNotNull(message);
            message = (Message) collector.nextResult(2000);
            assertNotNull(message);
            message = (Message) collector.nextResult(1000);
            assertNull(message);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}
