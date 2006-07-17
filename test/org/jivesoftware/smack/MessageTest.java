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
        getConnection(1).sendPacket(new Presence(Presence.Type.unavailable));

        try {
            Thread.sleep(500);

            // User1 sends some messages to User2 which is not available at the moment
            Chat chat = getConnection(0).createChat(getBareJID(1));
            chat.sendMessage("Test 1");
            chat.sendMessage("Test 2");

            Thread.sleep(500);

            // User2 becomes available again
            PacketCollector collector = getConnection(1).createPacketCollector(new MessageTypeFilter(Message.Type.CHAT));
            getConnection(1).sendPacket(new Presence(Presence.Type.available));

            // Check that offline messages are retrieved by user2 which is now available
            Message message = (Message) collector.nextResult(2500);
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

    /**
     * Check that two clients are able to send messages with a body of 4K characters and their
     * connections are not being closed.
     */
    public void testHugeMessage() {
        // User2 becomes available again
        PacketCollector collector = getConnection(1).createPacketCollector(new MessageTypeFilter(Message.Type.CHAT));

        // Create message with a body of 4K characters
        Message msg = new Message(getFullJID(1), Message.Type.CHAT);
        StringBuffer sb = new StringBuffer(5000);
        for (int i=0; i<=4000; i++) {
            sb.append("X");
        }
        msg.setBody(sb.toString());

        // Send the first message
        getConnection(0).sendPacket(msg);
        // Check that the connection that sent the message is still connected
        assertTrue("Connection was closed", getConnection(0).isConnected());
        // Check that the message was received
        Message rcv = (Message) collector.nextResult(1000);
        assertNotNull("No Message was received", rcv);

        // Send the second message
        getConnection(0).sendPacket(msg);
        // Check that the connection that sent the message is still connected
        assertTrue("Connection was closed", getConnection(0).isConnected());
        // Check that the second message was received
        rcv = (Message) collector.nextResult(1000);
        assertNotNull("No Message was received", rcv);

        // Try now sending huge messages over an SSL connection
        XMPPConnection conn = null;
        try {
            conn = new SSLXMPPConnection(getServiceName());
            conn.login(getUsername(0), getUsername(0), "Other resource");

            // Send the first message
            conn.sendPacket(msg);
            // Check that the connection that sent the message is still connected
            assertTrue("Connection was closed", conn.isConnected());
            // Check that the message was received
            rcv = (Message) collector.nextResult(1000);
            assertNotNull("No Message was received", rcv);
        } catch (XMPPException e) {
            fail(e.getMessage());
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }

    }

    protected int getMaxConnections() {
        return 2;
    }
}
