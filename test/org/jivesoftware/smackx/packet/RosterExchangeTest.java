/*
 * Created on 01/08/2003
 *
 */
package org.jivesoftware.smackx.packet;

import java.util.Iterator;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.*;

import junit.framework.TestCase;

/**
 *
 * Test the Roster Exchange extension using the low level API
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeTest extends TestCase {

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;
    private XMPPConnection conn3 = null;
    private XMPPConnection conn4 = null;

    private String user1 = null;
    private String user2 = null;
    private String user3 = null;
    private String user4 = null;

    /**
     * Constructor for RosterExchangeTest.
     * @param arg0
     */
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
        Chat chat1 = conn1.createChat(user2);

        // Create the message to send with the roster
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody("This message contains roster items.");
        // Create a RosterExchange Package and add it to the message
        assertTrue("Roster has no entries", conn1.getRoster().getEntryCount() > 0);
        RosterExchange rosterExchange = new RosterExchange(conn1.getRoster());
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
        Chat chat1 = conn1.createChat(user2);
        final Chat chat2 = new Chat(conn2, user1, chat1.getThreadID());

        // Create a Listener that listens for Messages with the extension "jabber:x:roster"
        // This listener will listen on the conn2 and answer an ACK if everything is ok
        PacketFilter packetFilter = new PacketExtensionFilter("x", "jabber:x:roster");
        PacketListener packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                assertNotNull("Body is null", message.getBody());
                try {
                    RosterExchange rosterExchange =
                        (RosterExchange) message.getExtension("x", "jabber:x:roster");
                    assertNotNull("Message without extension \"jabber:x:roster\"", rosterExchange);
                    assertTrue(
                        "Roster without entries",
                        rosterExchange.getRosterEntries().hasNext());
                    for (Iterator it = rosterExchange.getRosterEntries(); it.hasNext();) {
                        RemoteRosterEntry remoteRosterEntry = (RemoteRosterEntry) it.next();
                    }
                } catch (ClassCastException e) {
                    fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
                }
                try {
                    chat2.sendMessage("ok");
                } catch (Exception e) {
                    fail("An error occured sending ack " + e.getMessage());
                }
            }
        };
        conn2.addPacketListener(packetListener, packetFilter);

        // Create the message to send with the roster
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody("This message contains roster items.");
        // Create a RosterExchange Package and add it to the message
        assertTrue("Roster has no entries", conn1.getRoster().getEntryCount() > 0);
        RosterExchange rosterExchange = new RosterExchange(conn1.getRoster());
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
        // Wait for 2 seconds for a reply
        msg = chat1.nextMessage(2000);
        assertNotNull("No reply received", msg);
    }

    /**
     * Low level API test.
     * 1. User_1 will send his/her roster entries to user_2
     * 2. User_2 will automatically add the entries that receives to his/her roster in the corresponding group
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then something is wrong
     */
    public void testSendAndAcceptRosterEntries() {
        // Create a chat for each connection
        Chat chat1 = conn1.createChat(user2);
        final Chat chat2 = new Chat(conn2, user1, chat1.getThreadID());

        // Create a Listener that listens for Messages with the extension "jabber:x:roster"
        // This listener will listen on the conn2, save the roster entries and answer an ACK if everything is ok
        PacketFilter packetFilter = new PacketExtensionFilter("x", "jabber:x:roster");
        PacketListener packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                assertNotNull("Body is null", message.getBody());
                try {
                    RosterExchange rosterExchange =
                        (RosterExchange) message.getExtension("x", "jabber:x:roster");
                    assertNotNull("Message without extension \"jabber:x:roster\"", rosterExchange);
                    assertTrue(
                        "Roster without entries",
                        rosterExchange.getRosterEntries().hasNext());
                    // Add the roster entries to user2's roster
                    for (Iterator it = rosterExchange.getRosterEntries(); it.hasNext();) {
                        RemoteRosterEntry remoteRosterEntry = (RemoteRosterEntry) it.next();
                        conn2.getRoster().createEntry(
                            remoteRosterEntry.getUser(),
                            remoteRosterEntry.getName(),
                            remoteRosterEntry.getGroupArrayNames());
                    }
                } catch (ClassCastException e) {
                    fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
                } catch (Exception e) {
                    fail(e.toString());
                }
                try {
                    chat2.sendMessage("ok");
                } catch (Exception e) {
                    fail("An error occured sending ack " + e.getMessage());
                }
            }
        };
        conn2.addPacketListener(packetListener, packetFilter);

        // Create the message to send with the roster
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody("This message contains roster items.");
        // Create a RosterExchange Package and add it to the message
        assertTrue("Roster has no entries", conn1.getRoster().getEntryCount() > 0);
        RosterExchange rosterExchange = new RosterExchange(conn1.getRoster());
        msg.addExtension(rosterExchange);

        // Send the message that contains the roster
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
        // Wait for 10 seconds for a reply
        msg = chat1.nextMessage(5000);
        assertNotNull("No reply received", msg);
        assertTrue("Roster2 has no entries", conn2.getRoster().getEntryCount() > 0);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        try {
            // Connect to the server
            conn1 = new XMPPConnection("localhost");
            conn2 = new XMPPConnection("localhost");
            conn3 = new XMPPConnection("localhost");
            conn4 = new XMPPConnection("localhost");

            // Create the test accounts
            if (!conn1.getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");
            conn1.getAccountManager().createAccount("gato3", "gato3");
            conn2.getAccountManager().createAccount("gato4", "gato4");
            conn3.getAccountManager().createAccount("gato5", "gato5");
            conn4.getAccountManager().createAccount("gato6", "gato6");

            // Login with the test accounts
            conn1.login("gato3", "gato3");
            conn2.login("gato4", "gato4");
            conn3.login("gato5", "gato5");
            conn4.login("gato6", "gato6");

            user1 = "gato3@" + conn1.getHost();
            user2 = "gato4@" + conn2.getHost();
            user3 = "gato5@" + conn3.getHost();
            user4 = "gato6@" + conn4.getHost();

            conn1.getRoster().createEntry(
                "gato5@" + conn3.getHost(),
                "gato5",
                new String[] { "Friends, Coworker" });
            conn1.getRoster().createEntry("gato6@" + conn4.getHost(), "gato6", null);
            Thread.sleep(300);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        // Delete the created accounts for the test
        conn1.getAccountManager().deleteAccount();
        conn2.getAccountManager().deleteAccount();
        conn3.getAccountManager().deleteAccount();
        conn4.getAccountManager().deleteAccount();

        // Close all the connections
        conn1.close();
        conn2.close();
        conn3.close();
        conn4.close();
    }

}
