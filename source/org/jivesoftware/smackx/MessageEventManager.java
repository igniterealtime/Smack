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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;

/**
 *
 * Manages message events requests and notifications. A MessageEventManager provides a high 
 * level access to request for notifications and send event notifications. It also provides 
 * an easy way to hook up custom logic when requests or notifications are received. 
 *
 * @author Gaston Dombiak
 */
public class MessageEventManager {

    private List messageEventNotificationListeners = new ArrayList();
    private List messageEventRequestListeners = new ArrayList();

    private XMPPConnection con;

    private PacketFilter packetFilter = new PacketExtensionFilter("x", "jabber:x:event");
    private PacketListener packetListener;

    /**
     * Creates a new roster exchange manager.
     *
     * @param con an XMPPConnection.
     */
    public MessageEventManager(XMPPConnection con) {
        this.con = con;
        init();
    }

    /**
     * Adds to the message the requests to notify to the sender of the message for certain events.
     * 
     * @param message the message to add the requested notifications
     * @param offline specifies if the offline event is requested
     * @param delivered specifies if the delivered event is requested
     * @param displayed specifies if the displayed event is requested
     * @param composing specifies if the composing event is requested
     */
    public static void addNotificationsRequests(
        Message message,
        boolean offline,
        boolean delivered,
        boolean displayed,
        boolean composing) {
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setOffline(offline);
        messageEvent.setDelivered(delivered);
        messageEvent.setDisplayed(displayed);
        messageEvent.setComposing(composing);
        message.addExtension(messageEvent);
    }

    /**
     * Adds a message event request listener. The listener will be fired anytime a request for
     * event notification is received.
     *
     * @param messageEventRequestListener a message event request listener.
     */
    public void addMessageEventRequestListener(MessageEventRequestListener messageEventRequestListener) {
        synchronized (messageEventRequestListeners) {
            if (!messageEventRequestListeners.contains(messageEventRequestListener)) {
                messageEventRequestListeners.add(messageEventRequestListener);
            }
        }
    }

    /**
     * Removes a message event request listener. The listener will be fired anytime a request for
     * event notification is received.
     *
     * @param messageEventRequestListener a message event request listener.
     */
    public void removeMessageEventRequestListener(MessageEventRequestListener messageEventRequestListener) {
        synchronized (messageEventRequestListeners) {
            messageEventRequestListeners.remove(messageEventRequestListener);
        }
    }

    /**
     * Adds a message event notification listener. The listener will be fired anytime a notification
     * event is received.
     *
     * @param messageEventNotificationListener a message event notification listener.
     */
    public void addMessageEventNotificationListener(MessageEventNotificationListener messageEventNotificationListener) {
        synchronized (messageEventNotificationListeners) {
            if (!messageEventNotificationListeners.contains(messageEventNotificationListener)) {
                messageEventNotificationListeners.add(messageEventNotificationListener);
            }
        }
    }

    /**
     * Removes a message event notification listener. The listener will be fired anytime a notification
     * event is received.
     *
     * @param messageEventNotificationListener a message event notification listener.
     */
    public void removeMessageEventNotificationListener(MessageEventNotificationListener messageEventNotificationListener) {
        synchronized (messageEventNotificationListeners) {
            messageEventNotificationListeners.remove(messageEventNotificationListener);
        }
    }

    /**
     * Fires message event request listeners.
     */
    private void fireMessageEventRequestListeners(
        String from,
        String packetID,
        String methodName) {
        MessageEventRequestListener[] listeners = null;
        Method method;
        synchronized (messageEventRequestListeners) {
            listeners = new MessageEventRequestListener[messageEventRequestListeners.size()];
            messageEventRequestListeners.toArray(listeners);
        }
        try {
            method =
                MessageEventRequestListener.class.getDeclaredMethod(
                    methodName,
                    new Class[] { String.class, String.class, MessageEventManager.class });
            for (int i = 0; i < listeners.length; i++) {
                method.invoke(listeners[i], new Object[] { from, packetID, this });
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fires message event notification listeners.
     */
    private void fireMessageEventNotificationListeners(
        String from,
        String packetID,
        String methodName) {
        MessageEventNotificationListener[] listeners = null;
        Method method;
        synchronized (messageEventNotificationListeners) {
            listeners =
                new MessageEventNotificationListener[messageEventNotificationListeners.size()];
            messageEventNotificationListeners.toArray(listeners);
        }
        try {
            method =
                MessageEventNotificationListener.class.getDeclaredMethod(
                    methodName,
                    new Class[] { String.class, String.class });
            for (int i = 0; i < listeners.length; i++) {
                method.invoke(listeners[i], new Object[] { from, packetID });
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        // Listens for all message event packets and fire the proper message event listeners.
        packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                MessageEvent messageEvent =
                    (MessageEvent) message.getExtension("x", "jabber:x:event");
                if (messageEvent.isMessageEventRequest()) {
                    // Fire event for requests of message events
                    for (Iterator it = messageEvent.getEventTypes(); it.hasNext();)
                        fireMessageEventRequestListeners(
                            message.getFrom(),
                            message.getPacketID(),
                            ((String) it.next()).concat("NotificationRequested"));
                } else
                    // Fire event for notifications of message events
                    for (Iterator it = messageEvent.getEventTypes(); it.hasNext();)
                        fireMessageEventNotificationListeners(
                            message.getFrom(),
                            messageEvent.getPacketID(),
                            ((String) it.next()).concat("Notification"));

            };

        };
        con.addPacketListener(packetListener, packetFilter);
    }

    /**
     * Sends the notification that the message was delivered to the sender of the original message
     * 
     * @param to the recipient of the notification.
     * @param packetId the id of the message to send.
     */
    public void sendDeliveredNotification(String to, String packetID) {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setDelivered(true);
        messageEvent.setPacketID(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        con.sendPacket(msg);
    }

    /**
     * Sends the notification that the message was displayed to the sender of the original message
     * 
     * @param to the recipient of the notification.
     * @param packetId the id of the message to send.
     */
    public void sendDisplayedNotification(String to, String packetID) {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setDisplayed(true);
        messageEvent.setPacketID(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        con.sendPacket(msg);
    }

    /**
     * Sends the notification that the receiver of the message is composing a reply
     * 
     * @param to the recipient of the notification.
     * @param packetId the id of the message to send.
     */
    public void sendComposingNotification(String to, String packetID) {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setComposing(true);
        messageEvent.setPacketID(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        con.sendPacket(msg);
    }

    /**
     * Sends the notification that the receiver of the message has cancelled composing a reply
     * 
     * @param to the recipient of the notification.
     * @param packetId the id of the message to send.
     */
    public void sendCancelledNotification(String to, String packetID) {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setCancelled(true);
        messageEvent.setPacketID(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        con.sendPacket(msg);
    }

    public void destroy() {
        if (con != null)
            con.removePacketListener(packetListener);

    }
    public void finalize() {
        destroy();
    }

}
