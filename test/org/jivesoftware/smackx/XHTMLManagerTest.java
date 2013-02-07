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

package org.jivesoftware.smackx;

import java.util.Iterator;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Test the XHTML extension using the high level API
 *
 * @author Gaston Dombiak
 */
public class XHTMLManagerTest extends SmackTestCase {

    private int bodiesSent;
    private int bodiesReceived;

    /**
     * Constructor for XHTMLManagerTest.
     * @param name
     */
    public XHTMLManagerTest(String name) {
        super(name);
    }

    /**
     * High level API test.
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

        // Create an XHTMLText to send with the message
        XHTMLText xhtmlText = new XHTMLText(null, null);
        xhtmlText.appendOpenParagraphTag("font-size:large");
        xhtmlText.append("Hey John, this is my new ");
        xhtmlText.appendOpenSpanTag("color:green");
        xhtmlText.append("green");
        xhtmlText.appendCloseSpanTag();
        xhtmlText.appendOpenEmTag();
        xhtmlText.append("!!!!");
        xhtmlText.appendCloseEmTag();
        xhtmlText.appendCloseParagraphTag();
        // Add the XHTML text to the message
        XHTMLManager.addBody(msg, xhtmlText.toString());

        // User1 sends the message that contains the XHTML to user2
        try {
            chat1.sendMessage(msg);
            Thread.sleep(200);
        } catch (Exception e) {
            fail("An error occured sending the message with XHTML");
        }
    }

    /**
    * High level API test.
    * 1. User_1 will send a message with XHTML to user_2
    * 2. User_2 will receive the message and iterate over the XHTML bodies to check if everything 
    *    is fine
    * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then 
    *    something is wrong
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

        // Create an XHTMLText to send with the message
        XHTMLText xhtmlText = new XHTMLText(null, null);
        xhtmlText.appendOpenParagraphTag("font-size:large");
        xhtmlText.append("Hey John, this is my new ");
        xhtmlText.appendOpenSpanTag("color:green");
        xhtmlText.append("green");
        xhtmlText.appendCloseSpanTag();
        xhtmlText.appendOpenEmTag();
        xhtmlText.append("!!!!");
        xhtmlText.appendCloseEmTag();
        xhtmlText.appendCloseParagraphTag();
        // Add the XHTML text to the message
        XHTMLManager.addBody(msg, xhtmlText.toString());

        // User1 sends the message that contains the XHTML to user2
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with XHTML");
        }

        Packet packet = chat2.nextResult(2000);
        Message message = (Message) packet;
        assertTrue(
                "The received message is not an XHTML Message",
                XHTMLManager.isXHTMLMessage(message));
        try {
            assertTrue(
                    "Message without XHTML bodies",
                    XHTMLManager.getBodies(message).hasNext());
            for (Iterator<String> it = XHTMLManager.getBodies(message); it.hasNext();) {
                String body = it.next();
                System.out.println(body);
            }
        }
        catch (ClassCastException e) {
            fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
        }
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
        Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);
        final PacketCollector chat2 = getConnection(1).createPacketCollector(
                    new ThreadFilter(chat1.getThreadID()));

        // User1 creates a message to send to user2
        Message msg = new Message();
        msg.setSubject("Any subject you want");
        msg.setBody(
            "awesome! As Emerson once said: A foolish consistency is the hobgoblin of little minds.");

        // Create an XHTMLText to send with the message (in Spanish)
        XHTMLText xhtmlText = new XHTMLText(null, "es-ES");
        xhtmlText.appendOpenHeaderTag(1, null);
        xhtmlText.append("impresionante!");
        xhtmlText.appendCloseHeaderTag(1);
        xhtmlText.appendOpenParagraphTag(null);
        xhtmlText.append("Como Emerson dijo una vez:");
        xhtmlText.appendCloseParagraphTag();
        xhtmlText.appendOpenBlockQuoteTag(null);
        xhtmlText.appendOpenParagraphTag(null);
        xhtmlText.append("Una consistencia rid&#237;cula es el espantajo de mentes peque&#241;as.");
        xhtmlText.appendCloseParagraphTag();
        xhtmlText.appendCloseBlockQuoteTag();
        // Add the XHTML text to the message
        XHTMLManager.addBody(msg, xhtmlText.toString());

        // Create an XHTMLText to send with the message (in English)
        xhtmlText = new XHTMLText(null, "en-US");
        xhtmlText.appendOpenHeaderTag(1, null);
        xhtmlText.append("awesome!");
        xhtmlText.appendCloseHeaderTag(1);
        xhtmlText.appendOpenParagraphTag(null);
        xhtmlText.append("As Emerson once said:");
        xhtmlText.appendCloseParagraphTag();
        xhtmlText.appendOpenBlockQuoteTag(null);
        xhtmlText.appendOpenParagraphTag(null);
        xhtmlText.append("A foolish consistency is the hobgoblin of little minds.");
        xhtmlText.appendCloseParagraphTag();
        xhtmlText.appendCloseBlockQuoteTag();
        // Add the XHTML text to the message
        XHTMLManager.addBody(msg, xhtmlText.toString());

        // User1 sends the message that contains the XHTML to user2
        try {
            bodiesSent = 2;
            bodiesReceived = 0;
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message with XHTML");
        }

        Packet packet = chat2.nextResult(2000);
        int received = 0;
        Message message = (Message) packet;
        assertTrue(
                "The received message is not an XHTML Message",
                XHTMLManager.isXHTMLMessage(message));
        try {
            assertTrue(
                    "Message without XHTML bodies",
                    XHTMLManager.getBodies(message).hasNext());
            for (Iterator<String> it = XHTMLManager.getBodies(message); it.hasNext();) {
                received++;
                String body = it.next();
                System.out.println(body);
            }
            bodiesReceived = received;
        }
        catch (ClassCastException e) {
            fail("ClassCastException - Most probable cause is that smack providers" +
                    "is misconfigured");
        }
        assertEquals(
            "Number of sent and received XHTMP bodies does not match",
            bodiesSent,
            bodiesReceived);
    }

    protected int getMaxConnections() {
        return 2;
    }

}
