/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;

import junit.framework.TestCase;

/**
 *
 * Test the MessageEvent extension using the low level API
 *
 * @author Gaston Dombiak
 */
public class MessageEventTest extends TestCase {

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;

    private String user1 = null;
    private String user2 = null;

    /**
     * Constructor for MessageEventTest.
     * @param name
     */
    public MessageEventTest(String name) {
        super(name);
    }

    /**
     * Low level API test.
     * This is a simple test to use with a XMPP client and check if the client receives the 
     * message
     * 1. User_1 will send a message to user_2 requesting to be notified when any of these events
     * occurs: offline, composing, displayed or delivered 
     */
    public void testSendMessageEventRequest() {
        // Create a chat for each connection
        Chat chat1 = conn1.createChat(user2);

        // Create the message to send with the roster
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody("An interesting body comes here...");
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setComposing(true);
        messageEvent.setDelivered(true);
        messageEvent.setDisplayed(true);
        messageEvent.setOffline(true);
        msg.addExtension(messageEvent);

        // Send the message that contains the notifications request
        try {
            chat1.sendMessage(msg);
            // Wait half second so that the complete test can run
            Thread.sleep(200);
        } catch (Exception e) {
            fail("An error occured sending the message");
        }
    }

    /**
     * Low level API test.
     * This is a simple test to use with a XMPP client, check if the client receives the 
     * message and display in the console any notification
     * 1. User_1 will send a message to user_2 requesting to be notified when any of these events
     * occurs: offline, composing, displayed or delivered 
     * 2. User_2 will use a XMPP client (like Exodus) to display the message and compose a reply
     * 3. User_1 will display any notification that receives
     */
    public void testSendMessageEventRequestAndDisplayNotifications() {
        // Create a chat for each connection
        Chat chat1 = conn1.createChat(user2);

        // Create a Listener that listens for Messages with the extension "jabber:x:roster"
        // This listener will listen on the conn2 and answer an ACK if everything is ok
        PacketFilter packetFilter = new PacketExtensionFilter("x", "jabber:x:event");
        PacketListener packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                try {
                    MessageEvent messageEvent =
                        (MessageEvent) message.getExtension("x", "jabber:x:event");
                    assertNotNull("Message without extension \"jabber:x:event\"", messageEvent);
                    assertTrue(
                        "Message event is a request not a notification",
                        !messageEvent.isMessageEventRequest());
                    System.out.println(messageEvent.toXML());
                } catch (ClassCastException e) {
                    fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
                }
            }
        };
        conn1.addPacketListener(packetListener, packetFilter);

        // Create the message to send with the roster
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody("An interesting body comes here...");
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setComposing(true);
        messageEvent.setDelivered(true);
        messageEvent.setDisplayed(true);
        messageEvent.setOffline(true);
        msg.addExtension(messageEvent);

        // Send the message that contains the notifications request
        try {
            chat1.sendMessage(msg);
            // Wait half second so that the complete test can run
            Thread.sleep(200);
        } catch (Exception e) {
            fail("An error occured sending the message");
        }
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

            // Create the test accounts
            if (!conn1.getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");
            conn1.getAccountManager().createAccount("gato3", "gato3");
            conn2.getAccountManager().createAccount("gato4", "gato4");

            // Login with the test accounts
            conn1.login("gato3", "gato3");
            conn2.login("gato4", "gato4");

            user1 = "gato3@" + conn1.getHost();
            user2 = "gato4@" + conn2.getHost();

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

        // Close all the connections
        conn1.close();
        conn2.close();
    }
}
