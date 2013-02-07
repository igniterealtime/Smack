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

import java.util.ArrayList;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 *
 * Test the MessageEvent extension using the high level API.
 *
 * @author Gaston Dombiak
 */
public class MessageEventManagerTest extends SmackTestCase {

    public MessageEventManagerTest(String name) {
        super(name);
    }

    /**
     * High level API test.
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
        // Add to the message all the notifications requests (offline, delivered, displayed,
        // composing)
        MessageEventManager.addNotificationsRequests(msg, true, true, true, true);

        // Send the message that contains the notifications request
        try {
            chat1.sendMessage(msg);
        } catch (Exception e) {
            fail("An error occured sending the message");
        }
    }

    /**
     * High level API test.
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

        MessageEventManager messageEventManager = new MessageEventManager(getConnection(0));
        messageEventManager
            .addMessageEventNotificationListener(new MessageEventNotificationListener() {
            public void deliveredNotification(String from, String packetID) {
                System.out.println("From: " + from + " PacketID: " + packetID + "(delivered)");
            }

            public void displayedNotification(String from, String packetID) {
                System.out.println("From: " + from + " PacketID: " + packetID + "(displayed)");
            }

            public void composingNotification(String from, String packetID) {
                System.out.println("From: " + from + " PacketID: " + packetID + "(composing)");
            }

            public void offlineNotification(String from, String packetID) {
                System.out.println("From: " + from + " PacketID: " + packetID + "(offline)");
            }

            public void cancelledNotification(String from, String packetID) {
                System.out.println("From: " + from + " PacketID: " + packetID + "(cancelled)");
            }
        });

        // Create the message to send with the roster
        Message msg = new Message();
        msg.setSubject("Any subject you want");
        msg.setBody("An interesting body comes here...");
        // Add to the message all the notifications requests (offline, delivered, displayed,
        // composing)
        MessageEventManager.addNotificationsRequests(msg, true, true, true, true);

        // Send the message that contains the notifications request
        try {
            chat1.sendMessage(msg);
            // Wait a few seconds so that the XMPP client can send any event
            Thread.sleep(200);
        } catch (Exception e) {
            fail("An error occured sending the message");
        }
    }

    /**
     * High level API test.
     * 1. User_1 will send a message to user_2 requesting to be notified when any of these events
     * occurs: offline, composing, displayed or delivered 
     * 2. User_2 will receive the message
     * 3. User_2 will simulate that the message was displayed 
     * 4. User_2 will simulate that he/she is composing a reply
     * 5. User_2 will simulate that he/she has cancelled the reply
     */
    public void testRequestsAndNotifications() {
        final ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> resultsExpected = new ArrayList<String>();
        resultsExpected.add("deliveredNotificationRequested");
        resultsExpected.add("composingNotificationRequested");
        resultsExpected.add("displayedNotificationRequested");
        resultsExpected.add("offlineNotificationRequested");
        resultsExpected.add("deliveredNotification");
        resultsExpected.add("displayedNotification");
        resultsExpected.add("composingNotification");
        resultsExpected.add("cancelledNotification");

        // Create a chat for each connection
        Chat chat1 = getConnection(0).getChatManager().createChat(getBareJID(1), null);

        MessageEventManager messageEventManager1 = new MessageEventManager(getConnection(0));
        messageEventManager1
            .addMessageEventNotificationListener(new MessageEventNotificationListener() {
            public void deliveredNotification(String from, String packetID) {
                results.add("deliveredNotification");
            }

            public void displayedNotification(String from, String packetID) {
                results.add("displayedNotification");
            }

            public void composingNotification(String from, String packetID) {
                results.add("composingNotification");
            }

            public void offlineNotification(String from, String packetID) {
                results.add("offlineNotification");
            }

            public void cancelledNotification(String from, String packetID) {
                results.add("cancelledNotification");
            }
        });

        MessageEventManager messageEventManager2 = new MessageEventManager(getConnection(1));
        messageEventManager2
            .addMessageEventRequestListener(new DefaultMessageEventRequestListener() {
            public void deliveredNotificationRequested(
                String from,
                String packetID,
                MessageEventManager messageEventManager) {
                super.deliveredNotificationRequested(from, packetID, messageEventManager);
                results.add("deliveredNotificationRequested");
            }

            public void displayedNotificationRequested(
                String from,
                String packetID,
                MessageEventManager messageEventManager) {
                super.displayedNotificationRequested(from, packetID, messageEventManager);
                results.add("displayedNotificationRequested");
            }

            public void composingNotificationRequested(
                String from,
                String packetID,
                MessageEventManager messageEventManager) {
                super.composingNotificationRequested(from, packetID, messageEventManager);
                results.add("composingNotificationRequested");
            }

            public void offlineNotificationRequested(
                String from,
                String packetID,
                MessageEventManager messageEventManager) {
                super.offlineNotificationRequested(from, packetID, messageEventManager);
                results.add("offlineNotificationRequested");
            }
        });

        // Create the message to send with the roster
        Message msg = new Message();
        msg.setSubject("Any subject you want");
        msg.setBody("An interesting body comes here...");
        // Add to the message all the notifications requests (offline, delivered, displayed,
        // composing)
        MessageEventManager.addNotificationsRequests(msg, true, true, true, true);

        // Send the message that contains the notifications request
        try {
            chat1.sendMessage(msg);
            messageEventManager2.sendDisplayedNotification(getBareJID(0), msg.getPacketID());
            messageEventManager2.sendComposingNotification(getBareJID(0), msg.getPacketID());
            messageEventManager2.sendCancelledNotification(getBareJID(0), msg.getPacketID());
            // Wait up to 2 seconds
            long initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 2000 &&
                    (!results.containsAll(resultsExpected))) {
                Thread.sleep(100);
            }
            assertTrue(
                "Test failed due to bad results (1)" + resultsExpected,
                resultsExpected.containsAll(results));
            assertTrue(
                "Test failed due to bad results (2)" + results,
                results.containsAll(resultsExpected));

        } catch (Exception e) {
            fail("An error occured sending the message");
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}
