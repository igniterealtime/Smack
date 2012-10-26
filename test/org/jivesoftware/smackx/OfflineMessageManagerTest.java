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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests handling of offline messaging using OfflineMessageManager. This server requires the
 * server to support JEP-0013: Flexible Offline Message Retrieval.
 *
 * @author Gaston Dombiak
 */
public class OfflineMessageManagerTest extends SmackTestCase {

    public OfflineMessageManagerTest(String arg0) {
        super(arg0);
    }

    public void testDiscoverFlexibleRetrievalSupport() throws XMPPException {
        OfflineMessageManager offlineManager = new OfflineMessageManager(getConnection(1));
        assertTrue("Server does not support JEP-13", offlineManager.supportsFlexibleRetrieval());
    }

    /**
     * While user2 is connected but unavailable, user1 sends 2 messages to user1. User2 then
     * performs some "Flexible Offline Message Retrieval" checking the number of offline messages,
     * retriving the headers, then the real messages of the headers and finally removing the
     * loaded messages.
     */
    public void testReadAndDelete() {
        // Make user2 unavailable
        getConnection(1).sendPacket(new Presence(Presence.Type.unavailable));

        try {
            Thread.sleep(500);

            // User1 sends some messages to User2 which is not available at the moment
            Chat chat = getConnection(0).getChatManager().createChat(getBareJID(1), null);
            chat.sendMessage("Test 1");
            chat.sendMessage("Test 2");

            Thread.sleep(500);

            // User2 checks the number of offline messages
            OfflineMessageManager offlineManager = new OfflineMessageManager(getConnection(1));
            assertEquals("Wrong number of offline messages", 2, offlineManager.getMessageCount());
            // Check the message headers
            Iterator<OfflineMessageHeader> headers = offlineManager.getHeaders();
            assertTrue("No message header was found", headers.hasNext());
            List<String> stamps = new ArrayList<String>();
            while (headers.hasNext()) {
                OfflineMessageHeader header = headers.next();
                assertEquals("Incorrect sender", getFullJID(0), header.getJid());
                assertNotNull("No stamp was found in the message header", header.getStamp());
                stamps.add(header.getStamp());
            }
            assertEquals("Wrong number of headers", 2, stamps.size());
            // Get the offline messages
            Iterator<Message> messages = offlineManager.getMessages(stamps);
            assertTrue("No message was found", messages.hasNext());
            stamps = new ArrayList<String>();
            while (messages.hasNext()) {
                Message message = messages.next();
                OfflineMessageInfo info = (OfflineMessageInfo) message.getExtension("offline",
                        "http://jabber.org/protocol/offline");
                assertNotNull("No offline information was included in the offline message", info);
                assertNotNull("No stamp was found in the message header", info.getNode());
                stamps.add(info.getNode());
            }
            assertEquals("Wrong number of messages", 2, stamps.size());
            // Check that the offline messages have not been deleted
            assertEquals("Wrong number of offline messages", 2, offlineManager.getMessageCount());

            // User2 becomes available again
            PacketCollector collector = getConnection(1).createPacketCollector(
                    new MessageTypeFilter(Message.Type.chat));
            getConnection(1).sendPacket(new Presence(Presence.Type.available));

            // Check that no offline messages was sent to the user
            Message message = (Message) collector.nextResult(2500);
            assertNull("An offline message was sent from the server", message);

            // Delete the retrieved offline messages
            offlineManager.deleteMessages(stamps);
            // Check that there are no offline message for this user
            assertEquals("Wrong number of offline messages", 0, offlineManager.getMessageCount());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * While user2 is connected but unavailable, user1 sends 2 messages to user1. User2 then
     * performs some "Flexible Offline Message Retrieval" by fetching all the offline messages
     * and then removing all the offline messages.
     */
    public void testFetchAndPurge() {
        // Make user2 unavailable
        getConnection(1).sendPacket(new Presence(Presence.Type.unavailable));

        try {
            Thread.sleep(500);

            // User1 sends some messages to User2 which is not available at the moment
            Chat chat = getConnection(0).getChatManager().createChat(getBareJID(1), null);
            chat.sendMessage("Test 1");
            chat.sendMessage("Test 2");

            Thread.sleep(500);

            // User2 checks the number of offline messages
            OfflineMessageManager offlineManager = new OfflineMessageManager(getConnection(1));
            assertEquals("Wrong number of offline messages", 2, offlineManager.getMessageCount());
            // Get all offline messages
            Iterator<Message> messages = offlineManager.getMessages();
            assertTrue("No message was found", messages.hasNext());
            List<String> stamps = new ArrayList<String>();
            while (messages.hasNext()) {
                Message message = messages.next();
                OfflineMessageInfo info = (OfflineMessageInfo) message.getExtension("offline",
                        "http://jabber.org/protocol/offline");
                assertNotNull("No offline information was included in the offline message", info);
                assertNotNull("No stamp was found in the message header", info.getNode());
                stamps.add(info.getNode());
            }
            assertEquals("Wrong number of messages", 2, stamps.size());
            // Check that the offline messages have not been deleted
            assertEquals("Wrong number of offline messages", 2, offlineManager.getMessageCount());

            // User2 becomes available again
            PacketCollector collector = getConnection(1).createPacketCollector(
                    new MessageTypeFilter(Message.Type.chat));
            getConnection(1).sendPacket(new Presence(Presence.Type.available));

            // Check that no offline messages was sent to the user
            Message message = (Message) collector.nextResult(2500);
            assertNull("An offline message was sent from the server", message);

            // Delete all offline messages
            offlineManager.deleteMessages();
            // Check that there are no offline message for this user
            assertEquals("Wrong number of offline messages", 0, offlineManager.getMessageCount());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}
