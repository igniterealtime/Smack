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

package org.jivesoftware.smackx;

import java.util.ArrayList;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;

import junit.framework.TestCase;

/**
 *
 * Test the MessageEvent extension using the high level API.
 *
 * @author Gaston Dombiak
 */
public class MessageEventManagerTest extends TestCase {

    /**
     * Constructor for MessageEventManagerTest.
     * @param name
     */
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
         String host = "localhost";
         String server_user1 = "gato3";
         String user1 = "gato3@localhost";
         String pass1 = "gato3";
    
         String user2 = "gato4@localhost";
    
         XMPPConnection conn1 = null;
    
         try {
             // Connect to the server and log in the users
             conn1 = new XMPPConnection(host);
             conn1.login(server_user1, pass1);
    
             // Create a chat for each connection
             Chat chat1 = conn1.createChat(user2);
    
             // Create the message to send with the roster
             Message msg = chat1.createMessage();
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
         } catch (Exception e) {
             fail(e.toString());
         } finally {
             if (conn1 != null)
                 conn1.close();
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
        String host = "localhost";
        String server_user1 = "gato3";
        String user1 = "gato3@localhost";
        String pass1 = "gato3";
    
        String user2 = "gato4@localhost";
    
        XMPPConnection conn1 = null;
    
        try {
            // Connect to the server and log in the users
            conn1 = new XMPPConnection(host);
            conn1.login(server_user1, pass1);
    
            // Create a chat for each connection
            Chat chat1 = conn1.createChat(user2);
    
            MessageEventManager messageEventManager = new MessageEventManager(conn1);
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
            Message msg = chat1.createMessage();
            msg.setSubject("Any subject you want");
            msg.setBody("An interesting body comes here...");
            // Add to the message all the notifications requests (offline, delivered, displayed,
            // composing)
            MessageEventManager.addNotificationsRequests(msg, true, true, true, true);
    
            // Send the message that contains the notifications request
            try {
                chat1.sendMessage(msg);
                // Wait a few seconds so that the XMPP client can send any event
                Thread.sleep(5000);
            } catch (Exception e) {
                fail("An error occured sending the message");
            }
        } catch (Exception e) {
            fail(e.toString());
        } finally {
            if (conn1 != null)
                conn1.close();
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
        String host = "localhost";
        String server_user1 = "gato3";
        String user1 = "gato3@localhost";
        String pass1 = "gato3";

        String server_user2 = "gato4";
        String user2 = "gato4@localhost";
        String pass2 = "gato4";

        XMPPConnection conn1 = null;
        XMPPConnection conn2 = null;

        final ArrayList results = new ArrayList();
        ArrayList resultsExpected = new ArrayList();
        resultsExpected.add("deliveredNotificationRequested");
        resultsExpected.add("composingNotificationRequested");
        resultsExpected.add("displayedNotificationRequested");
        resultsExpected.add("offlineNotificationRequested");
        resultsExpected.add("deliveredNotification");
        resultsExpected.add("displayedNotification");
        resultsExpected.add("composingNotification");
        resultsExpected.add("cancelledNotification");

        try {
            // Connect to the server and log in the users
            conn1 = new XMPPConnection(host);
            conn1.login(server_user1, pass1);
            conn2 = new XMPPConnection(host);
            conn2.login(server_user2, pass2);

            // Create a chat for each connection
            Chat chat1 = conn1.createChat(user2);

            MessageEventManager messageEventManager1 = new MessageEventManager(conn1);
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

            MessageEventManager messageEventManager2 = new MessageEventManager(conn2);
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
            Message msg = chat1.createMessage();
            msg.setSubject("Any subject you want");
            msg.setBody("An interesting body comes here...");
            // Add to the message all the notifications requests (offline, delivered, displayed,
            // composing)
            MessageEventManager.addNotificationsRequests(msg, true, true, true, true);

            // Send the message that contains the notifications request
            try {
                chat1.sendMessage(msg);
                messageEventManager2.sendDisplayedNotification(user1, msg.getPacketID());
                messageEventManager2.sendComposingNotification(user1, msg.getPacketID());
                messageEventManager2.sendCancelledNotification(user1, msg.getPacketID());
                // Wait half second so that the complete test can run
                Thread.sleep(500);
                assertTrue("Test failed due to bad results (1)" + resultsExpected, resultsExpected.containsAll(results));
                assertTrue("Test failed due to bad results (2)" + results, results.containsAll(resultsExpected));

            } catch (Exception e) {
                fail("An error occured sending the message");
            }
        } catch (Exception e) {
            fail(e.toString());
        } finally {
            if (conn1 != null)
                conn1.close();
            if (conn2 != null)
                conn2.close();
        }
    }

}
