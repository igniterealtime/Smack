/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.smack;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.packet.Presence;

/**
 * Ensure that the server is delivering messages to the correct client based on the client's
 * presence priority.
 *
 * @author Gaston Dombiak
 */
public class PresencePriorityTest extends SmackTestCase {

    public PresencePriorityTest(String arg0) {
        super(arg0);
    }

    /**
     * Connection(0) will send messages to the bareJID of Connection(1) where the user of
     * Connection(1) has logged from two different places with different presence priorities.
     */
    public void testMessageToHighestPriority() {
        boolean wasFiltering = Chat.isFilteredOnThreadID();
        Chat.setFilteredOnThreadID(false);
        XMPPConnection conn = null;
        try {
            // User_1 will log in again using another resource
            conn = new XMPPConnection(getHost(), getPort());
            conn.login(getUsername(1), getUsername(1), "OtherPlace");
            // Change the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.AVAILABLE, null, 1,
                    Presence.Mode.AVAILABLE));
            conn.sendPacket(new Presence(Presence.Type.AVAILABLE, null, 2,
                    Presence.Mode.AVAILABLE));
            // Create the chats between the participants
            Chat chat0 = new Chat(getConnection(0), getBareJID(1));
            Chat chat1 = new Chat(getConnection(1), getBareJID(0));
            Chat chat2 = new Chat(conn, getBareJID(0));

            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            assertNotNull("Resource with highest priority didn't receive the message",
                    chat2.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat1.nextMessage(1000));

            // Invert the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.AVAILABLE, null, 2,
                    Presence.Mode.AVAILABLE));
            conn.sendPacket(new Presence(Presence.Type.AVAILABLE, null, 1,
                    Presence.Mode.AVAILABLE));

            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            assertNotNull("Resource with highest priority didn't receive the message",
                    chat1.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat2.nextMessage(1000));

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        finally {
            // Restore the previous filtering value so we don't affect other test cases
            Chat.setFilteredOnThreadID(wasFiltering);
            if (conn != null) {
                conn.close();
            }
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}
