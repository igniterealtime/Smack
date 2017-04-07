/**
 *
 * Copyright 2003-2007 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.xevent;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.xevent.packet.MessageEvent;
import org.jxmpp.jid.Jid;

/**
 * 
 * Manages message events requests and notifications. A MessageEventManager provides a high
 * level access to request for notifications and send event notifications. It also provides 
 * an easy way to hook up custom logic when requests or notifications are received. 
 *
 * @author Gaston Dombiak
 * @see <a href="http://xmpp.org/extensions/xep-0022.html">XEP-22: Message Events</a>
 */
public final class MessageEventManager extends Manager {
    private static final Logger LOGGER = Logger.getLogger(MessageEventManager.class.getName());

    private static final Map<XMPPConnection, MessageEventManager> INSTANCES = new WeakHashMap<>();

    private static final StanzaFilter PACKET_FILTER = new AndFilter(new StanzaExtensionFilter(
                    new MessageEvent()), new NotFilter(MessageTypeFilter.ERROR));

    private List<MessageEventNotificationListener> messageEventNotificationListeners = new CopyOnWriteArrayList<MessageEventNotificationListener>();
    private List<MessageEventRequestListener> messageEventRequestListeners = new CopyOnWriteArrayList<MessageEventRequestListener>();

    public synchronized static MessageEventManager getInstanceFor(XMPPConnection connection) {
        MessageEventManager messageEventManager = INSTANCES.get(connection);
        if (messageEventManager == null) {
            messageEventManager = new MessageEventManager(connection);
            INSTANCES.put(connection, messageEventManager);
        }
        return messageEventManager;
    }

    /**
     * Creates a new message event manager.
     *
     * @param con an XMPPConnection to a XMPP server.
     */
    private MessageEventManager(XMPPConnection connection) {
        super(connection);
        // Listens for all message event packets and fire the proper message event listeners.
        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                Message message = (Message) packet;
                MessageEvent messageEvent =
                    (MessageEvent) message.getExtension("x", "jabber:x:event");
                if (messageEvent.isMessageEventRequest()) {
                    // Fire event for requests of message events
                    for (String eventType : messageEvent.getEventTypes())
                        fireMessageEventRequestListeners(
                            message.getFrom(),
                            message.getStanzaId(),
                            eventType.concat("NotificationRequested"));
                } else
                    // Fire event for notifications of message events
                    for (String eventType : messageEvent.getEventTypes())
                        fireMessageEventNotificationListeners(
                            message.getFrom(),
                            messageEvent.getStanzaId(),
                            eventType.concat("Notification"));
            }
        }, PACKET_FILTER);
    }

    /**
     * Adds event notification requests to a message. For each event type that
     * the user wishes event notifications from the message recepient for, <tt>true</tt>
     * should be passed in to this method.
     * 
     * @param message the message to add the requested notifications.
     * @param offline specifies if the offline event is requested.
     * @param delivered specifies if the delivered event is requested.
     * @param displayed specifies if the displayed event is requested.
     * @param composing specifies if the composing event is requested.
     */
    public static void addNotificationsRequests(Message message, boolean offline,
            boolean delivered, boolean displayed, boolean composing)
    {
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
        messageEventRequestListeners.add(messageEventRequestListener);

    }

    /**
     * Removes a message event request listener. The listener will be fired anytime a request for
     * event notification is received.
     *
     * @param messageEventRequestListener a message event request listener.
     */
    public void removeMessageEventRequestListener(MessageEventRequestListener messageEventRequestListener) {
        messageEventRequestListeners.remove(messageEventRequestListener);
    }

    /**
     * Adds a message event notification listener. The listener will be fired anytime a notification
     * event is received.
     *
     * @param messageEventNotificationListener a message event notification listener.
     */
    public void addMessageEventNotificationListener(MessageEventNotificationListener messageEventNotificationListener) {
        messageEventNotificationListeners.add(messageEventNotificationListener);
    }

    /**
     * Removes a message event notification listener. The listener will be fired anytime a notification
     * event is received.
     *
     * @param messageEventNotificationListener a message event notification listener.
     */
    public void removeMessageEventNotificationListener(MessageEventNotificationListener messageEventNotificationListener) {
        messageEventNotificationListeners.remove(messageEventNotificationListener);
    }

    /**
     * Fires message event request listeners.
     */
    private void fireMessageEventRequestListeners(
        Jid from,
        String packetID,
        String methodName) {
        try {
            Method method =
                MessageEventRequestListener.class.getDeclaredMethod(
                    methodName,
                    new Class<?>[] { Jid.class, String.class, MessageEventManager.class });
            for (MessageEventRequestListener listener : messageEventRequestListeners) {
                method.invoke(listener, new Object[] { from, packetID, this });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while invoking MessageEventRequestListener's  " + methodName, e);
        }
    }

    /**
     * Fires message event notification listeners.
     */
    private void fireMessageEventNotificationListeners(
        Jid from,
        String packetID,
        String methodName) {
        try {
            Method method =
                MessageEventNotificationListener.class.getDeclaredMethod(
                    methodName,
                    new Class<?>[] { Jid.class, String.class });
            for (MessageEventNotificationListener listener : messageEventNotificationListeners) {
                method.invoke(listener, new Object[] { from, packetID });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while invoking MessageEventNotificationListener's " + methodName, e);
        }
    }

    /**
     * Sends the notification that the message was delivered to the sender of the original message.
     * 
     * @param to the recipient of the notification.
     * @param packetID the id of the message to send.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendDeliveredNotification(Jid to, String packetID) throws NotConnectedException, InterruptedException {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setDelivered(true);
        messageEvent.setStanzaId(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        connection().sendStanza(msg);
    }

    /**
     * Sends the notification that the message was displayed to the sender of the original message.
     * 
     * @param to the recipient of the notification.
     * @param packetID the id of the message to send.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendDisplayedNotification(Jid to, String packetID) throws NotConnectedException, InterruptedException {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setDisplayed(true);
        messageEvent.setStanzaId(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        connection().sendStanza(msg);
    }

    /**
     * Sends the notification that the receiver of the message is composing a reply.
     * 
     * @param to the recipient of the notification.
     * @param packetID the id of the message to send.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendComposingNotification(Jid to, String packetID) throws NotConnectedException, InterruptedException {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setComposing(true);
        messageEvent.setStanzaId(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        connection().sendStanza(msg);
    }

    /**
     * Sends the notification that the receiver of the message has cancelled composing a reply.
     * 
     * @param to the recipient of the notification.
     * @param packetID the id of the message to send.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void sendCancelledNotification(Jid to, String packetID) throws NotConnectedException, InterruptedException {
        // Create the message to send
        Message msg = new Message(to);
        // Create a MessageEvent Package and add it to the message
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setCancelled(true);
        messageEvent.setStanzaId(packetID);
        msg.addExtension(messageEvent);
        // Send the packet
        connection().sendStanza(msg);
    }
}
