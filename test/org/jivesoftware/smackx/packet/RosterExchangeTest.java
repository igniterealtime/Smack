/*
 * Created on 01/08/2003
 *
 */
package org.jivesoftware.smackx.packet;

import java.util.Iterator;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.RosterExchange;

import junit.framework.TestCase;

/**
 *
 * Test the Roster Exchange extension
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeTest extends TestCase {

    /**
     * Constructor for RosterExchangeTest.
     * @param arg0
     */
    public RosterExchangeTest(String arg0) {
        super(arg0);
    }

	/**
	 * 1. User_1 will send his/her roster entries to user_2
	 * 2. User_2 will receives the entries and iterate over them to check if everything is fine
	 * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then something is wrong
	 */
    public void testSendAndReceiveRosterEntries() {
        String host = "localhost";
        String server_user1 = "gato3";
        String user1 = "gato3@gato.home";
        String pass1 = "gato3";

        String server_user2 = "gato1";
        String user2 = "gato1@gato.home";
        String pass2 = "gato1";

        XMPPConnection conn1 = null;
        XMPPConnection conn2 = null;

        try {
            // Connect to the server and log in the users
            conn1 = new XMPPConnection(host);
            conn1.login(server_user1, pass1);
            conn2 = new XMPPConnection(host);
            conn2.login(server_user2, pass2);

            // Create a chat for each connection
            Chat chat1 = conn1.createChat(user2);
            final Chat chat2 = new Chat(conn2, user1, chat1.getChatID());

            // Create a Listener that listens for Messages with the extension "jabber:x:roster"
            // This listener will listen on the conn2 and answer an ACK if everything is ok
            PacketFilter packetFilter = new PacketExtensionFilter("x", "jabber:x:roster");
            PacketListener packetListener = new PacketListener() {
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    assertNotNull("Body is null", message.getBody());
                    try {
                        RosterExchange rosterExchange = (RosterExchange) message.getExtension("x","jabber:x:roster");
                        assertNotNull("Message without extension \"jabber:x:roster\"", rosterExchange);
                        assertTrue("Roster without entries", rosterExchange.getRosterItems().hasNext());
                        for (Iterator it = rosterExchange.getRosterItems(); it.hasNext();) {
                            RosterExchange.Item item = (RosterExchange.Item) it.next();
                        }
                    }
                    catch (ClassCastException e){
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
            Message msg = new Message(user2);
            msg.setSubject("Any subject you want");
            msg.setBody("This message contains roster items.");
            // Create a RosterExchange Package and add it to the message
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
        catch (Exception e) {
            fail(e.toString());
        }
        finally {
            if (conn1 != null)
                conn1.close();
            if (conn2 != null)
                conn2.close();
        }

    }

	/**
	 * 1. User_1 will send his/her roster entries to user_2
	 * 2. User_2 will automatically add the entries that receives to his/her roster in the corresponding group
	 * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then something is wrong
	 */
    public void testSendAndAcceptRosterEntries() {
        String host = "localhost";
        String server_user1 = "gato3";
        String user1 = "gato3@gato.home";
        String pass1 = "gato3";

        String server_user2 = "gato4";
        String user2 = "gato4@gato.home";
        String pass2 = "gato4";

        XMPPConnection conn1 = null;
        XMPPConnection conn2 = null;

        try {
            // Connect to the server and log in the users
            conn1 = new XMPPConnection(host);
            conn1.login(server_user1, pass1);
            conn2 = new XMPPConnection(host);
            conn2.login(server_user2, pass2);
            final Roster user2_roster = conn2.getRoster();

            // Create a chat for each connection
            Chat chat1 = conn1.createChat(user2);
            final Chat chat2 = new Chat(conn2, user1, chat1.getChatID());

            // Create a Listener that listens for Messages with the extension "jabber:x:roster"
            // This listener will listen on the conn2, save the roster entries and answer an ACK if everything is ok
            PacketFilter packetFilter = new PacketExtensionFilter("x", "jabber:x:roster");
            PacketListener packetListener = new PacketListener() {
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    assertNotNull("Body is null", message.getBody());
                    try {
                        RosterExchange rosterExchange = (RosterExchange) message.getExtension("x","jabber:x:roster");
                        assertNotNull("Message without extension \"jabber:x:roster\"", rosterExchange);
                        assertTrue("Roster without entries", rosterExchange.getRosterItems().hasNext());
                        // Add the roster entries to user2's roster
                        for (Iterator it = rosterExchange.getRosterItems(); it.hasNext();) {
                            RosterExchange.Item item = (RosterExchange.Item) it.next();
                            user2_roster.createEntry(item.getUser(), item.getName(), item.getGroupArrayNames());
                        }
                    }
                    catch (ClassCastException e){
                        fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
                    }
                    catch (Exception e) {
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
            Message msg = new Message(user2);
            msg.setSubject("Any subject you want");
            msg.setBody("This message contains roster items.");
            // Create a RosterExchange Package and add it to the message
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
        }
        catch (Exception e) {
            fail(e.toString());
        }
        finally {
            if (conn1 != null)
                conn1.close();
            if (conn2 != null)
                conn2.close();
        }

    }

}
