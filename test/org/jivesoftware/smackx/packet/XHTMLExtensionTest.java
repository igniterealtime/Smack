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

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;

import junit.framework.TestCase;

/**
 * Test the XHTML extension using the low level API
 *
 * @author Gaston Dombiak
 */
public class XHTMLExtensionTest extends TestCase {

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;

    private String user1 = null;
    private String user2 = null;

    private int bodiesSent;
    private int bodiesReceived;

    /**
     * Constructor for XHTMLExtensionTest.
     * @param name
     */
    public XHTMLExtensionTest(String name) {
        super(name);
    }

    /**
     * Low level API test.
     * This is a simple test to use with a XMPP client and check if the client receives the message
     * 1. User_1 will send a message with formatted text (XHTML) to user_2
     */
    public void testSendSimpleXHTMLMessage() {
        // User1 creates a chat with user2
        Chat chat1 = conn1.createChat(user2);

        // User1 creates a message to send to user2
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody("Hey John, this is my new green!!!!");
        // Create a XHTMLExtension Package and add it to the message
        XHTMLExtension xhtmlExtension = new XHTMLExtension();
        xhtmlExtension.addBody(
            "<body><p style='font-size:large'>Hey John, this is my new <span style='color:green'>green</span><em>!!!!</em></p></body>");
        msg.addExtension(xhtmlExtension);

        // User1 sends the message that contains the XHTML to user2
        try {
            chat1.sendMessage(msg);
            Thread.sleep(200);
        } catch (Exception e) {
            fail("An error occured sending the message with XHTML");
        }
    }

    /**
    * Low level API test.
    * 1. User_1 will send a message with XHTML to user_2
    * 2. User_2 will receive the message and iterate over the XHTML bodies to check if everything 
    *    is fine
    * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then 
    *    something is wrong
    */
    public void testSendSimpleXHTMLMessageAndDisplayReceivedXHTMLMessage() {
        // Create a chat for each connection
        Chat chat1 = conn1.createChat(user2);
        final Chat chat2 = new Chat(conn2, user1, chat1.getThreadID());

        // Create a Listener that listens for Messages with the extension 
        //"http://jabber.org/protocol/xhtml-im"
        // This listener will listen on the conn2 and answer an ACK if everything is ok
        PacketFilter packetFilter =
            new PacketExtensionFilter("html", "http://jabber.org/protocol/xhtml-im");
        PacketListener packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                assertNotNull("Body is null", message.getBody());
                try {
                    XHTMLExtension xhtmlExtension =
                        (XHTMLExtension) message.getExtension(
                            "html",
                            "http://jabber.org/protocol/xhtml-im");
                    assertNotNull(
                        "Message without extension \"http://jabber.org/protocol/xhtml-im\"",
                        xhtmlExtension);
                    assertTrue("Message without XHTML bodies", xhtmlExtension.getBodiesCount() > 0);
                    for (Iterator it = xhtmlExtension.getBodies(); it.hasNext();) {
                        String body = (String) it.next();
                        System.out.println(body);
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

        // User1 creates a message to send to user2
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody("Hey John, this is my new green!!!!");
        // Create a XHTMLExtension Package and add it to the message
        XHTMLExtension xhtmlExtension = new XHTMLExtension();
        xhtmlExtension.addBody(
            "<body><p style='font-size:large'>Hey John, this is my new <span style='color:green'>green</span><em>!!!!</em></p></body>");
        msg.addExtension(xhtmlExtension);

        // User1 sends the message that contains the XHTML to user2
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with XHTML");
        }
        // Wait for 2 seconds for a reply
        msg = chat1.nextMessage(1000);
        assertNotNull("No reply received", msg);
    }

    /**
    * Low level API test. Test a message with two XHTML bodies and several XHTML tags.
    * 1. User_1 will send a message with XHTML to user_2
    * 2. User_2 will receive the message and iterate over the XHTML bodies to check if everything 
    *    is fine
    * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then 
    *    something is wrong
    */
    public void testSendComplexXHTMLMessageAndDisplayReceivedXHTMLMessage() {
        // Create a chat for each connection
        Chat chat1 = conn1.createChat(user2);
        final Chat chat2 = new Chat(conn2, user1, chat1.getThreadID());

        // Create a Listener that listens for Messages with the extension 
        //"http://jabber.org/protocol/xhtml-im"
        // This listener will listen on the conn2 and answer an ACK if everything is ok
        PacketFilter packetFilter =
            new PacketExtensionFilter("html", "http://jabber.org/protocol/xhtml-im");
        PacketListener packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                int received = 0;
                Message message = (Message) packet;
                assertNotNull("Body is null", message.getBody());
                try {
                    XHTMLExtension xhtmlExtension =
                        (XHTMLExtension) message.getExtension(
                            "html",
                            "http://jabber.org/protocol/xhtml-im");
                    assertNotNull(
                        "Message without extension \"http://jabber.org/protocol/xhtml-im\"",
                        xhtmlExtension);
                    assertTrue("Message without XHTML bodies", xhtmlExtension.getBodiesCount() > 0);
                    for (Iterator it = xhtmlExtension.getBodies(); it.hasNext();) {
                        received++;
                        System.out.println((String) it.next());
                    }
                    bodiesReceived = received;
                } catch (ClassCastException e) {
                    fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
                }
            }
        };
        conn2.addPacketListener(packetListener, packetFilter);

        // User1 creates a message to send to user2
        Message msg = chat1.createMessage();
        msg.setSubject("Any subject you want");
        msg.setBody(
            "awesome! As Emerson once said: A foolish consistency is the hobgoblin of little minds.");
        // Create an XHTMLExtension and add it to the message
        XHTMLExtension xhtmlExtension = new XHTMLExtension();
        xhtmlExtension.addBody(
            "<body xml:lang=\"es-ES\"><h1>impresionante!</h1><p>Como Emerson dijo una vez:</p><blockquote><p>Una consistencia ridícula es el espantajo de mentes pequeñas.</p></blockquote></body>");
        xhtmlExtension.addBody(
            "<body xml:lang=\"en-US\"><h1>awesome!</h1><p>As Emerson once said:</p><blockquote><p>A foolish consistency is the hobgoblin of little minds.</p></blockquote></body>");
        msg.addExtension(xhtmlExtension);

        // User1 sends the message that contains the XHTML to user2
        try {
            bodiesSent = xhtmlExtension.getBodiesCount();
            bodiesReceived = 0;
            chat1.sendMessage(msg);
            Thread.sleep(300);
        } catch (Exception e) {
            fail("An error occured sending the message with XHTML");
        }
        // Wait half second so that the complete test can run
        assertEquals(
            "Number of sent and received XHTMP bodies does not match",
            bodiesSent,
            bodiesReceived);
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
