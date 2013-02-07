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

import java.util.Iterator;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Ensure that the server is delivering messages to the correct client based on the client's
 * presence priority.
 *
 * @author Gaston Dombiak
 */
public class PresenceTest extends SmackTestCase {

    public PresenceTest(String arg0) {
        super(arg0);
    }

    /**
     * Connection(0) will send messages to the bareJID of Connection(1) where the user of
     * Connection(1) has logged from two different places with different presence priorities.
     */
    public void testMessageToHighestPriority() {
        XMPPConnection conn = null;
        try {
            // User_1 will log in again using another resource
            conn = createConnection();
            conn.connect();
            conn.login(getUsername(1), getUsername(1), "OtherPlace");
            // Change the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));
            conn.sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));
            Thread.sleep(150);
            // Create the chats between the participants
            Chat chat0 = getConnection(0).getChatManager().createChat(getBareJID(1), null);
            Chat chat1 = getConnection(1).getChatManager().createChat(getBareJID(0), chat0.getThreadID(), null);
            Chat chat2 = conn.getChatManager().createChat(getBareJID(0), chat0.getThreadID(), null);

            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            /*assertNotNull("Resource with highest priority didn't receive the message",
                    chat2.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat1.nextMessage(1000));*/

            // Invert the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));
            conn.sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));

            Thread.sleep(150);
            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            /*assertNotNull("Resource with highest priority didn't receive the message",
                    chat1.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat2.nextMessage(1000));*/

            // User_1 closes his connection
            conn.disconnect();
            Thread.sleep(150);

            // Test delivery of message to the unique presence of the user_1
            chat0.sendMessage("Hello");
            /*assertNotNull("Resource with highest priority didn't receive the message",
                    chat1.nextMessage(2000));*/

            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));

            // User_1 will log in again using another resource
            conn = createConnection();
            conn.connect();
            conn.login(getUsername(1), getPassword(1), "OtherPlace");
            conn.sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));
            chat2 = conn.getChatManager().createChat(getBareJID(0), chat0.getThreadID(), null);

            Thread.sleep(150);
            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            /*assertNotNull("Resource with highest priority didn't receive the message",
                    chat1.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat2.nextMessage(1000));*/

            // Invert the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));
            conn.sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));

            Thread.sleep(150);
            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            /*assertNotNull("Resource with highest priority didn't receive the message",
                    chat2.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat1.nextMessage(1000));*/

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * User1 logs from 2 resources but only one is available. User0 sends a message
     * to the full JID of the unavailable resource. User1 in the not available resource
     * should receive the message.
     * TODO Fix this in Wildfire but before check if XMPP spec requests this feature
     */
    public void testNotAvailablePresence() throws XMPPException {
        // Change the presence to unavailable of User_1
        getConnection(1).sendPacket(new Presence(Presence.Type.unavailable));

        // User_1 will log in again using another resource (that is going to be available)
        XMPPConnection conn = createConnection();
        conn.connect();
        conn.login(getUsername(1), getPassword(1), "OtherPlace");

        // Create chats between participants
        Chat chat0 = getConnection(0).getChatManager().createChat(getFullJID(1), null);
        Chat chat1 = getConnection(1).getChatManager().createChat(getBareJID(0), chat0.getThreadID(), null);

        // Test delivery of message to the presence with highest priority
        chat0.sendMessage("Hello");
        /*assertNotNull("Not available connection didn't receive message sent to full JID",
                chat1.nextMessage(2000));
        assertNull("Not available connection received an unknown message",
                chat1.nextMessage(1000));*/
        conn.disconnect();

    }

    /**
     * User1 is connected from 2 resources. User1 adds User0 to his roster. Ensure
     * that presences are correctly retrieved for User1. User1 logs off from one resource
     * and ensure that presences are still correct for User1.
     */
    public void testMultipleResources() throws Exception {
        // Create another connection for the same user of connection 1
        ConnectionConfiguration connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        XMPPConnection conn4 = new XMPPConnection(connectionConfiguration);
        conn4.connect();
        conn4.login(getUsername(1), getPassword(1), "Home");

        // Add a new roster entry
        Roster roster = getConnection(0).getRoster();
        roster.createEntry(getBareJID(1), "gato1", null);

        // Wait up to 2 seconds
        long initial = System.currentTimeMillis();
        while (System.currentTimeMillis() - initial < 2000 && (
                !roster.getPresence(getBareJID(1)).isAvailable()))
        {
            Thread.sleep(100);
        }

        // Check that a presence is returned for the new contact
        Presence presence = roster.getPresence(getBareJID(1));
        assertTrue("Returned an offline Presence for an existing user", presence.isAvailable());

        presence = roster.getPresenceResource(getBareJID(1) + "/Home");
        assertTrue("Returned an offline Presence for Home resource", presence.isAvailable());

        presence = roster.getPresenceResource(getFullJID(1));
        assertTrue("Returned an offline Presence for Smack resource", presence.isAvailable());

        Iterator<Presence> presences = roster.getPresences(getBareJID(1));
        assertTrue("Returned an offline Presence for an existing user", presence.isAvailable());
        assertNotNull("No presence was found for user1", presences);
        assertTrue("No presence was found for user1", presences.hasNext());
        presences.next();
        assertTrue("Only one presence was found for user1", presences.hasNext());

        // User1 logs out from one resource
        conn4.disconnect();

        // Wait up to 1 second
        Thread.sleep(700);

        // Check that a presence is returned for the new contact
        presence = roster.getPresence(getBareJID(1));
        assertTrue("Returned a null Presence for an existing user", presence.isAvailable());

        presence = roster.getPresenceResource(getFullJID(1));
        assertTrue("Returned a null Presence for Smack resource", presence.isAvailable());

        presence = roster.getPresenceResource(getBareJID(1) + "/Home");
        assertTrue("Returned a Presence for no longer connected resource", !presence.isAvailable());

        presences = roster.getPresences(getBareJID(1));
        assertNotNull("No presence was found for user1", presences);
        Presence value = presences.next();
        assertTrue("No presence was found for user1", value.isAvailable());
        assertFalse("More than one presence was found for user1", presences.hasNext());
    }

    /**
     * User1 logs in, then sets offline presence information (presence with status text). User2
     * logs in and checks to see if offline presence is returned.
     *
     * @throws Exception if an exception occurs.
     */
    public void testOfflineStatusPresence() throws Exception {
        // Add a new roster entry for other user.
        Roster roster = getConnection(0).getRoster();
        roster.createEntry(getBareJID(1), "gato1", null);

        // Wait up to 2 seconds
        long initial = System.currentTimeMillis();
        while (System.currentTimeMillis() - initial < 2000 && (
                roster.getPresence(getBareJID(1)).getType().equals(Presence.Type.unavailable))) {
            Thread.sleep(100);
        }

        // Sign out of conn1 with status
        Presence offlinePresence = new Presence(Presence.Type.unavailable);
        offlinePresence.setStatus("Offline test");
        getConnection(1).disconnect(offlinePresence);

        // Wait 500 ms
        Thread.sleep(500);
        Presence presence = getConnection(0).getRoster().getPresence(getBareJID(1));
        assertEquals("Offline presence status not received.", "Offline test", presence.getStatus());

        // Sign out of conn0.
        getConnection(0).disconnect();

        // See if conneciton 0 can get offline status.
        XMPPConnection con0 = getConnection(0);
        con0.connect();
        con0.login(getUsername(0), getUsername(0));

        // Wait 500 ms
        Thread.sleep(500);
        presence = con0.getRoster().getPresence(getBareJID(1));
        assertTrue("Offline presence status not received after logout.",
                "Offline test".equals(presence.getStatus()));
    }

    protected int getMaxConnections() {
        return 2;
    }
}
