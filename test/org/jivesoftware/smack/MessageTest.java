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

import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.test.SmackTestCase;

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
     * Will a user recieve a message from another after only sending the user a directed presence,
     * or will Wildfire intercept for offline storage?
     *
     * User1 becomes lines. User0 never sent an available presence to the server but
     * instead sent one to User1. User1 sends a message to User0. Should User0 get the
     * message?
     */
    public void testDirectPresence() {
        getConnection(1).sendPacket(new Presence(Presence.Type.available));

        Presence presence = new Presence(Presence.Type.available);
        presence.setTo(getBareJID(1));
        getConnection(0).sendPacket(presence);

        PacketCollector collector = getConnection(0)
                .createPacketCollector(new MessageTypeFilter(Message.Type.chat));
        try {
            getConnection(1).getChatManager().createChat(getBareJID(0), null).sendMessage("Test 1");
        }
        catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Message message = (Message) collector.nextResult(2500);
        assertNotNull("Message not recieved from remote user", message);
    }

    /**
     * Check that when a client becomes unavailable all messages sent to the client are stored offline. So that when
     * the client becomes available again the offline messages are received.
     */
    public void testOfflineMessage() {
        getConnection(0).sendPacket(new Presence(Presence.Type.available));
        getConnection(1).sendPacket(new Presence(Presence.Type.available));
        // Make user2 unavailable
        getConnection(1).sendPacket(new Presence(Presence.Type.unavailable));

        try {
            Thread.sleep(500);

            // User1 sends some messages to User2 which is not available at the moment
            Chat chat = getConnection(0).getChatManager().createChat(getBareJID(1), null);
            PacketCollector collector = getConnection(1).createPacketCollector(
                    new MessageTypeFilter(Message.Type.chat));
            chat.sendMessage("Test 1");
            chat.sendMessage("Test 2");

            Thread.sleep(500);

            // User2 becomes available again

            getConnection(1).sendPacket(new Presence(Presence.Type.available));

            // Check that offline messages are retrieved by user2 which is now available
            Message message = (Message) collector.nextResult(2500);
            assertNotNull(message);
            message = (Message) collector.nextResult(2000);
            assertNotNull(message);
            message = (Message) collector.nextResult(1000);
            assertNull(message);

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Send messages with invalid XML characters to offline users. Check that offline users
     * are receiving them from the server.<p>
     *
     * Test case commented out since some servers may just close the connection while others
     * are more tolerant and accept stanzas with invalid XML characters.
     */
    /*public void testOfflineMessageInvalidXML() {
        // Make user2 unavailable
        getConnection(1).sendPacket(new Presence(Presence.Type.unavailable));

        try {
            Thread.sleep(500);

            // User1 sends some messages to User2 which is not available at the moment
            Chat chat = getConnection(0).getChatManager().createChat(getBareJID(1), null);
            PacketCollector collector = getConnection(1).createPacketCollector(
                    new MessageTypeFilter(Message.Type.chat));
            chat.sendMessage("Test \f 1");
            chat.sendMessage("Test \r 1");

            Thread.sleep(500);

            // User2 becomes available again

            getConnection(1).sendPacket(new Presence(Presence.Type.available));

            // Check that offline messages are retrieved by user2 which is now available
            Message message = (Message) collector.nextResult(2500);
            assertNotNull(message);
            message = (Message) collector.nextResult(2000);
            assertNotNull(message);
            message = (Message) collector.nextResult(1000);
            assertNull(message);

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }*/

    /**
     * Check that two clients are able to send messages with a body of 4K characters and their
     * connections are not being closed.
     */
    public void testHugeMessage() {
        getConnection(0).sendPacket(new Presence(Presence.Type.available));
        getConnection(1).sendPacket(new Presence(Presence.Type.available));
        // User2 becomes available again
        PacketCollector collector = getConnection(1).createPacketCollector(
                new MessageTypeFilter(Message.Type.chat));

        // Create message with a body of 4K characters
        Message msg = new Message(getFullJID(1), Message.Type.chat);
        StringBuilder sb = new StringBuilder(5000);
        for (int i = 0; i <= 4000; i++) {
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
    }


    /**
     * User0 is connected from 2 resources. User0 is available in both resources
     * but with different priority presence values. User1 sends a message to the
     * bare JID of User0. Check that the resource with highest priority will get
     * the messages.
     *
     * @throws Exception if an error occurs.
     */
    public void testHighestPriority() throws Exception {
        // Create another connection for the same user of connection 1
        ConnectionConfiguration connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        XMPPConnection conn3 = new XMPPConnection(connectionConfiguration);
        conn3.connect();
        conn3.login(getUsername(0), getPassword(0), "Home");
        // Set this connection as highest priority
        Presence presence = new Presence(Presence.Type.available);
        presence.setPriority(10);
        conn3.sendPacket(presence);
        // Set this connection as highest priority
        presence = new Presence(Presence.Type.available);
        presence.setPriority(5);
        getConnection(0).sendPacket(presence);

        // Let the server process the change in presences
        Thread.sleep(200);

        // User0 listen in both connected clients
        PacketCollector collector = getConnection(0).createPacketCollector(new MessageTypeFilter(Message.Type.chat));
        PacketCollector coll3 = conn3.createPacketCollector(new MessageTypeFilter(Message.Type.chat));

        // User1 sends a message to the bare JID of User0 
        Chat chat = getConnection(1).getChatManager().createChat(getBareJID(0), null);
        chat.sendMessage("Test 1");
        chat.sendMessage("Test 2");

        // Check that messages were sent to resource with highest priority
        Message message = (Message) collector.nextResult(2000);
        assertNull("Resource with lowest priority got the message", message);
        message = (Message) coll3.nextResult(2000);
        assertNotNull(message);
        assertEquals("Test 1", message.getBody());
        message = (Message) coll3.nextResult(1000);
        assertNotNull(message);
        assertEquals("Test 2", message.getBody());

        conn3.disconnect();
    }

    /**
     * User0 is connected from 2 resources. User0 is available in both resources
     * but with different show values. User1 sends a message to the
     * bare JID of User0. Check that the resource with highest show value will get
     * the messages.
     *
     * @throws Exception if an error occurs.
     */
    public void testHighestShow() throws Exception {
        // Create another connection for the same user of connection 1
        ConnectionConfiguration connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        XMPPConnection conn3 = new XMPPConnection(connectionConfiguration);
        conn3.connect();
        conn3.login(getUsername(0), getPassword(0), "Home");
        // Set this connection as highest priority
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.away);
        conn3.sendPacket(presence);
        // Set this connection as highest priority
        presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        getConnection(0).sendPacket(presence);

        // Let the server process the change in presences
        Thread.sleep(200);

        // User0 listen in both connected clients
        PacketCollector collector = getConnection(0).createPacketCollector(new MessageTypeFilter(Message.Type.chat));
        PacketCollector coll3 = conn3.createPacketCollector(new MessageTypeFilter(Message.Type.chat));

        // User1 sends a message to the bare JID of User0
        Chat chat = getConnection(1).getChatManager().createChat(getBareJID(0), null);
        chat.sendMessage("Test 1");
        chat.sendMessage("Test 2");

        // Check that messages were sent to resource with highest priority
        Message message = (Message) coll3.nextResult(2000);
        assertNull("Resource with lowest show value got the message", message);
        message = (Message) collector.nextResult(2000);
        assertNotNull(message);
        assertEquals("Test 1", message.getBody());
        message = (Message) collector.nextResult(1000);
        assertNotNull(message);
        assertEquals("Test 2", message.getBody());

        conn3.disconnect();
    }

    /**
     * User0 is connected from 2 resources. User0 is available in both resources
     * with same priority presence values and same show values. User1 sends a message to the
     * bare JID of User0. Check that the resource with most recent activity will get
     * the messages.
     *
     * @throws Exception if an error occurs.
     */
    public void testMostRecentActive() throws Exception {
        // Create another connection for the same user of connection 1
        ConnectionConfiguration connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        XMPPConnection conn3 = new XMPPConnection(connectionConfiguration);
        conn3.connect();
        conn3.login(getUsername(0), getPassword(0), "Home");
        // Set this connection as highest priority
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        presence.setPriority(10);
        conn3.sendPacket(presence);
        // Set this connection as highest priority
        presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        presence.setPriority(10);
        getConnection(0).sendPacket(presence);

        connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        XMPPConnection conn4 = new XMPPConnection(connectionConfiguration);
        conn4.connect();
        conn4.login(getUsername(0), getPassword(0), "Home2");
        presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        presence.setPriority(4);
        getConnection(0).sendPacket(presence);


        // Let the server process the change in presences
        Thread.sleep(200);

        // User0 listen in both connected clients
        PacketCollector collector = getConnection(0).createPacketCollector(new MessageTypeFilter(Message.Type.chat));
        PacketCollector coll3 = conn3.createPacketCollector(new MessageTypeFilter(Message.Type.chat));
        PacketCollector coll4 = conn4.createPacketCollector(new MessageTypeFilter(Message.Type.chat));

        // Send a message from this resource to indicate most recent activity 
        conn3.sendPacket(new Message("admin@" + getServiceName()));

        // User1 sends a message to the bare JID of User0
        Chat chat = getConnection(1).getChatManager().createChat(getBareJID(0), null);
        chat.sendMessage("Test 1");
        chat.sendMessage("Test 2");

        // Check that messages were sent to resource with highest priority
        Message message = (Message) collector.nextResult(2000);
        assertNull("Resource with oldest activity got the message", message);
        message = (Message) coll4.nextResult(2000);
        assertNull(message);
        message = (Message) coll3.nextResult(2000);
        assertNotNull(message);
        assertEquals("Test 1", message.getBody());
        message = (Message) coll3.nextResult(1000);
        assertNotNull(message);
        assertEquals("Test 2", message.getBody());

        conn3.disconnect();
        conn4.disconnect();
    }

    /**
     * User0 is connected from 1 resource with a negative priority presence. User1
     * sends a message to the bare JID of User0. Messages should be stored offline.
     * User0 then changes the priority presence to a positive value. Check that
     * offline messages were delivered to the user.
     *
     * @throws Exception if an error occurs.
     */
    public void testOfflineStorageWithNegativePriority() throws Exception {
        // Set this connection with negative priority
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        presence.setPriority(-1);
        getConnection(0).sendPacket(presence);

        // Let the server process the change in presences
        Thread.sleep(200);

        // User0 listen for incoming traffic
        PacketCollector collector = getConnection(0).createPacketCollector(new MessageTypeFilter(Message.Type.chat));

        // User1 sends a message to the bare JID of User0
        Chat chat = getConnection(1).getChatManager().createChat(getBareJID(0), null);
        chat.sendMessage("Test 1");
        chat.sendMessage("Test 2");

        // Check that messages were sent to resource with highest priority
        Message message = (Message) collector.nextResult(2000);
        assertNull("Messages were not stored offline", message);

        // Set this connection with positive priority
        presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        presence.setPriority(1);
        getConnection(0).sendPacket(presence);

        // Let the server process the change in presences
        Thread.sleep(200);

        message = (Message) collector.nextResult(2000);
        assertNotNull("Offline messages were not delivered", message);
        assertEquals("Test 1", message.getBody());
        message = (Message) collector.nextResult(1000);
        assertNotNull(message);
        assertEquals("Test 2", message.getBody());
    }

    protected int getMaxConnections() {
        return 2;
    }


    @Override
    protected boolean sendInitialPresence() {
        return false;
    }
}
