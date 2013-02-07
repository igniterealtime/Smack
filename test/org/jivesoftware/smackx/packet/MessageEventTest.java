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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 *
 * Test the MessageEvent extension using the low level API
 *
 * @author Gaston Dombiak
 */
public class MessageEventTest extends SmackTestCase {

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
        Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);

        // Create the message to send with the roster
        Message msg = new Message();
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
        }
        catch (Exception e) {
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
        Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);

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
                }
                catch (ClassCastException e) {
                    fail("ClassCastException - Most probable cause is that smack providers is misconfigured");
                }
            }
        };
        getConnection(0).addPacketListener(packetListener, packetFilter);

        // Create the message to send with the roster
        Message msg = new Message();
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
        }
        catch (Exception e) {
            fail("An error occured sending the message");
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}
