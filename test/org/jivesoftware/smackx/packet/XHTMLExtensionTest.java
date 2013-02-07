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
/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
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

package org.jivesoftware.smackx.packet;

import java.util.Iterator;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Test the XHTML extension using the low level API
 *
 * @author Gaston Dombiak
 */
public class XHTMLExtensionTest extends SmackTestCase {

    private int bodiesSent;
    private int bodiesReceived;

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
	Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);

	// User1 creates a message to send to user2
	Message msg = new Message();
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
	}
	catch (Exception e) {
	    fail("An error occured sending the message with XHTML");
	}
    }

    /**
     * Low level API test.
     * 1. User_1 will send a message with XHTML to user_2
     * 2. User_2 will receive the message and iterate over the XHTML bodies to check if everything
     * is fine
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then
     * something is wrong
     */
    public void testSendSimpleXHTMLMessageAndDisplayReceivedXHTMLMessage() {
	// Create a chat for each connection
	Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);
	final PacketCollector chat2 = getConnection(1).createPacketCollector(
		new ThreadFilter(chat1.getThreadID()));

	// User1 creates a message to send to user2
	Message msg = new Message();
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
	}
	catch (Exception e) {
	    fail("An error occured sending the message with XHTML");
	}
	Packet packet = chat2.nextResult(2000);
	Message message = (Message) packet;
	assertNotNull("Body is null", message.getBody());
	try {
	    xhtmlExtension =
		(XHTMLExtension) message.getExtension(
			"html",
		"http://jabber.org/protocol/xhtml-im");
	    assertNotNull(
		    "Message without extension \"http://jabber.org/protocol/xhtml-im\"",
		    xhtmlExtension);
	    assertTrue("Message without XHTML bodies", xhtmlExtension.getBodiesCount() > 0);
	    for (Iterator<String> it = xhtmlExtension.getBodies(); it.hasNext();) {
		String body = it.next();
		System.out.println(body);
	    }
	}
	catch (ClassCastException e) {
	    fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
	}
    }

    /**
     * Low level API test. Test a message with two XHTML bodies and several XHTML tags.
     * 1. User_1 will send a message with XHTML to user_2
     * 2. User_2 will receive the message and iterate over the XHTML bodies to check if everything
     * is fine
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then
     * something is wrong
     */
    public void testSendComplexXHTMLMessageAndDisplayReceivedXHTMLMessage() {
	// Create a chat for each connection
	Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);
	final PacketCollector chat2 = getConnection(1).createPacketCollector(
		new ThreadFilter(chat1.getThreadID()));

	// Create a Listener that listens for Messages with the extension 
	//"http://jabber.org/protocol/xhtml-im"
	// This listener will listen on the conn2 and answer an ACK if everything is ok
	PacketFilter packetFilter =
	    new PacketExtensionFilter("html", "http://jabber.org/protocol/xhtml-im");
	PacketListener packetListener = new PacketListener() {
	    @Override
	    public void processPacket(Packet packet) {

	    }
	};
	getConnection(1).addPacketListener(packetListener, packetFilter);

        // User1 creates a message to send to user2
        Message msg = new Message();
        msg.setSubject("Any subject you want");
        msg.setBody(
                "awesome! As Emerson once said: A foolish consistency is the hobgoblin of little minds.");
        // Create an XHTMLExtension and add it to the message
        XHTMLExtension xhtmlExtension = new XHTMLExtension();
        xhtmlExtension.addBody(
                "<body xml:lang=\"es-ES\"><h1>impresionante!</h1><p>Como Emerson dijo una vez:</p><blockquote><p>Una consistencia ridicula es el espantajo de mentes pequenas.</p></blockquote></body>");
        xhtmlExtension.addBody(
                "<body xml:lang=\"en-US\"><h1>awesome!</h1><p>As Emerson once said:</p><blockquote><p>A foolish consistency is the hobgoblin of little minds.</p></blockquote></body>");
        msg.addExtension(xhtmlExtension);

	// User1 sends the message that contains the XHTML to user2
	try {
	    bodiesSent = xhtmlExtension.getBodiesCount();
	    bodiesReceived = 0;
	    chat1.sendMessage(msg);
	}
	catch (Exception e) {
	    fail("An error occured sending the message with XHTML");
	}
	Packet packet = chat2.nextResult(2000);
	int received = 0;
	Message message = (Message) packet;
	assertNotNull("Body is null", message.getBody());
	try {
	    xhtmlExtension =
		(XHTMLExtension) message.getExtension(
			"html",
		"http://jabber.org/protocol/xhtml-im");
	    assertNotNull(
		    "Message without extension \"http://jabber.org/protocol/xhtml-im\"",
		    xhtmlExtension);
	    assertTrue("Message without XHTML bodies", xhtmlExtension.getBodiesCount() > 0);
	    for (Iterator<String> it = xhtmlExtension.getBodies(); it.hasNext();) {
		received++;
		System.out.println(it.next());
	    }
	    bodiesReceived = received;
	}
	catch (ClassCastException e) {
	    fail("ClassCastException - Most probable cause is that smack providers is " +
	    "misconfigured");
	}
	// Wait half second so that the complete test can run
	assertEquals(
		"Number of sent and received XHTMP bodies does not match",
		bodiesSent,
		bodiesReceived);
    }

    @Override
    protected int getMaxConnections() {
	return 2;
    }

}
