/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
/*
 * Created on 01/08/2003
 *
 */
package org.jivesoftware.smackx.packet;

import java.util.Iterator;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.*;

/**
 *
 * Test the Roster Exchange extension using the low level API
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeTest extends SmackTestCase {

    public RosterExchangeTest(String arg0) {
        super(arg0);
    }

    /**
     * Low level API test.
     * This is a simple test to use with a XMPP client and check if the client receives the message
     * 1. User_1 will send his/her roster entries to user_2
     */
    public void testSendRosterEntries() {
        // Create a chat for each connection
        Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);

        // Create the message to send with the roster
        Message msg = new Message();
        msg.setSubject("Any subject you want");
        msg.setBody("This message contains roster items.");
        // Create a RosterExchange Package and add it to the message
        assertTrue("Roster has no entries", getConnection(0).getRoster().getEntryCount() > 0);
        RosterExchange rosterExchange = new RosterExchange(getConnection(0).getRoster());
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
    }

    /**
    * Low level API test.
    * 1. User_1 will send his/her roster entries to user_2
    * 2. User_2 will receive the entries and iterate over them to check if everything is fine
    * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then something is wrong
    */
    public void testSendAndReceiveRosterEntries() {
        // Create a chat for each connection
        Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);
        final PacketCollector chat2 = getConnection(1).createPacketCollector(
                new ThreadFilter(chat1.getThreadID()));

        // Create the message to send with the roster
        Message msg = new Message();
        msg.setSubject("Any subject you want");
        msg.setBody("This message contains roster items.");
        // Create a RosterExchange Package and add it to the message
        assertTrue("Roster has no entries", getConnection(0).getRoster().getEntryCount() > 0);
        RosterExchange rosterExchange = new RosterExchange(getConnection(0).getRoster());
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
        // Wait for 2 seconds for a reply
        Packet packet = chat2.nextResult(2000);
        Message message = (Message) packet;
        assertNotNull("Body is null", message.getBody());
        try {
            rosterExchange =
                    (RosterExchange) message.getExtension("x", "jabber:x:roster");
            assertNotNull("Message without extension \"jabber:x:roster\"", rosterExchange);
            assertTrue(
                    "Roster without entries",
                    rosterExchange.getRosterEntries().hasNext());
        }
        catch (ClassCastException e) {
            fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
        }
    }

    /**
     * Low level API test.
     * 1. User_1 will send his/her roster entries to user_2
     * 2. User_2 will automatically add the entries that receives to his/her roster in the corresponding group
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then something is wrong
     */
    public void testSendAndAcceptRosterEntries() {
        // Create a chat for each connection
        Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);
        final PacketCollector chat2 = getConnection(1).createPacketCollector(
                new ThreadFilter(chat1.getThreadID()));

        // Create the message to send with the roster
        Message msg = new Message();
        msg.setSubject("Any subject you want");
        msg.setBody("This message contains roster items.");
        // Create a RosterExchange Package and add it to the message
        assertTrue("Roster has no entries", getConnection(0).getRoster().getEntryCount() > 0);
        RosterExchange rosterExchange = new RosterExchange(getConnection(0).getRoster());
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
        // Wait for 10 seconds for a reply
        Packet packet = chat2.nextResult(5000);
        Message message = (Message) packet;
        assertNotNull("Body is null", message.getBody());
        try {
            rosterExchange =
                    (RosterExchange) message.getExtension("x", "jabber:x:roster");
            assertNotNull("Message without extension \"jabber:x:roster\"", rosterExchange);
            assertTrue(
                    "Roster without entries",
                    rosterExchange.getRosterEntries().hasNext());
            // Add the roster entries to user2's roster
            for (Iterator<RemoteRosterEntry> it = rosterExchange.getRosterEntries(); it.hasNext();) {
                RemoteRosterEntry remoteRosterEntry = it.next();
                getConnection(1).getRoster().createEntry(
                        remoteRosterEntry.getUser(),
                        remoteRosterEntry.getName(),
                        remoteRosterEntry.getGroupArrayNames());
            }
        }
        catch (ClassCastException e) {
            fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
        }
        catch (Exception e) {
            fail(e.toString());
        }

        assertTrue("Roster2 has no entries", getConnection(1).getRoster().getEntryCount() > 0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        try {
            getConnection(0).getRoster().createEntry(
                getBareJID(2),
                "gato5",
                new String[] { "Friends, Coworker" });
            getConnection(0).getRoster().createEntry(getBareJID(3), "gato6", null);
            Thread.sleep(300);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected int getMaxConnections() {
        return 4;
    }

}
