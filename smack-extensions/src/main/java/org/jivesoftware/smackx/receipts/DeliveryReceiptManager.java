/**
 *
 * Copyright 2013-2014 Georg Lukas, 2015 Florian Schmaus
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
package org.jivesoftware.smackx.receipts;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

/**
 * Manager for XEP-0184: Message Delivery Receipts. This class implements
 * the manager for {@link DeliveryReceipt} support, enabling and disabling of
 * automatic DeliveryReceipt transmission.
 *
 * <p>
 * You can send delivery receipt requests and listen for incoming delivery receipts as shown in this example:
 * </p>
 * <pre>
 * deliveryReceiptManager.addReceiptReceivedListener(new ReceiptReceivedListener() {
 *   void onReceiptReceived(String fromJid, String toJid, String receiptId, Packet receipt) {
 *     // If the receiving entity does not support delivery receipts,
 *     // then the receipt received listener may not get invoked.
 *   }
 * });
 * Message message = …
 * DeliveryReceiptRequest.addTo(message);
 * connection.sendStanza(message);
 * </pre>
 *
 * DeliveryReceiptManager can be configured to automatically add delivery receipt requests to every
 * message with {@link #autoAddDeliveryReceiptRequests()}.
 *
 * @author Georg Lukas
 * @see <a href="http://xmpp.org/extensions/xep-0184.html">XEP-0184: Message Delivery Receipts</a>
 */
public class DeliveryReceiptManager extends Manager {

    private static final StanzaFilter MESSAGES_WITH_DEVLIERY_RECEIPT_REQUEST = new AndFilter(StanzaTypeFilter.MESSAGE,
                    new StanzaExtensionFilter(new DeliveryReceiptRequest()));
    private static final StanzaFilter MESSAGES_WITH_DELIVERY_RECEIPT = new AndFilter(StanzaTypeFilter.MESSAGE,
                    new StanzaExtensionFilter(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE));

    private static Map<XMPPConnection, DeliveryReceiptManager> instances = new WeakHashMap<XMPPConnection, DeliveryReceiptManager>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Specifies when incoming message delivery receipt requests should be automatically
     * acknowledged with an receipt.
     */
    public enum AutoReceiptMode {

        /**
         * Never send deliver receipts
         */
        disabled,

        /**
         * Only send delivery receipts if the requester is subscribed to our presence.
         */
        ifIsSubscribed,

        /**
         * Always send delivery receipts. <b>Warning:</b> this may causes presence leaks. See <a
         * href="http://xmpp.org/extensions/xep-0184.html#security">XEP-0184: Message Delivery
         * Receipts § 8. Security Considerations</a>
         */
        always,
    }

    private static AutoReceiptMode defaultAutoReceiptMode = AutoReceiptMode.ifIsSubscribed;

    /**
     * Set the default automatic receipt mode for new connections.
     * 
     * @param autoReceiptMode the default automatic receipt mode.
     */
    public static void setDefaultAutoReceiptMode(AutoReceiptMode autoReceiptMode) {
        defaultAutoReceiptMode = autoReceiptMode;
    }

    private AutoReceiptMode autoReceiptMode = defaultAutoReceiptMode;

    private final Set<ReceiptReceivedListener> receiptReceivedListeners = new CopyOnWriteArraySet<ReceiptReceivedListener>();

    private DeliveryReceiptManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(DeliveryReceipt.NAMESPACE);

        // Add the packet listener to handling incoming delivery receipts
        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) throws NotConnectedException {
                DeliveryReceipt dr = DeliveryReceipt.from((Message) packet);
                // notify listeners of incoming receipt
                for (ReceiptReceivedListener l : receiptReceivedListeners) {
                    l.onReceiptReceived(packet.getFrom(), packet.getTo(), dr.getId(), packet);
                }
            }
        }, MESSAGES_WITH_DELIVERY_RECEIPT);

        // Add the packet listener to handle incoming delivery receipt requests
        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) throws NotConnectedException, InterruptedException {
                final Jid from = packet.getFrom();
                final XMPPConnection connection = connection();
                switch (autoReceiptMode) {
                case disabled:
                    return;
                case ifIsSubscribed:
                    if (!Roster.getInstanceFor(connection).isSubscribedToMyPresence(from)) {
                        return;
                    }
                    break;
                case always:
                    break;
                }

                final Message messageWithReceiptRequest = (Message) packet;
                Message ack = receiptMessageFor(messageWithReceiptRequest);
                connection.sendStanza(ack);
            }
        }, MESSAGES_WITH_DEVLIERY_RECEIPT_REQUEST);
    }

    /**
     * Obtain the DeliveryReceiptManager responsible for a connection.
     *
     * @param connection the connection object.
     *
     * @return the DeliveryReceiptManager instance for the given connection
     */
     public static synchronized DeliveryReceiptManager getInstanceFor(XMPPConnection connection) {
        DeliveryReceiptManager receiptManager = instances.get(connection);

        if (receiptManager == null) {
            receiptManager = new DeliveryReceiptManager(connection);
            instances.put(connection, receiptManager);
        }

        return receiptManager;
    }

    /**
     * Returns true if Delivery Receipts are supported by a given JID
     * 
     * @param jid
     * @return true if supported
     * @throws SmackException if there was no response from the server.
     * @throws XMPPException 
     * @throws InterruptedException 
     */
    public boolean isSupported(Jid jid) throws SmackException, XMPPException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid,
                        DeliveryReceipt.NAMESPACE);
    }

    /**
     * Configure whether the {@link DeliveryReceiptManager} should automatically
     * reply to incoming {@link DeliveryReceipt}s.
     *
     * @param autoReceiptMode the new auto receipt mode.
     * @see AutoReceiptMode
     */
    public void setAutoReceiptMode(AutoReceiptMode autoReceiptMode) {
        this.autoReceiptMode = autoReceiptMode;
    }

    /**
     * Get the currently active auto receipt mode.
     * 
     * @return the currently active auto receipt mode.
     */
    public AutoReceiptMode getAutoReceiptMode() {
        return autoReceiptMode;
    }

    /**
     * Get informed about incoming delivery receipts with a {@link ReceiptReceivedListener}.
     * 
     * @param listener the listener to be informed about new receipts
     */
    public void addReceiptReceivedListener(ReceiptReceivedListener listener) {
        receiptReceivedListeners.add(listener);
    }

    /**
     * Stop getting informed about incoming delivery receipts.
     * 
     * @param listener the listener to be removed
     */
    public void removeReceiptReceivedListener(ReceiptReceivedListener listener) {
        receiptReceivedListeners.remove(listener);
    }

    private static final StanzaListener AUTO_ADD_DELIVERY_RECEIPT_REQUESTS_LISTENER = new StanzaListener() {
        @Override
        public void processPacket(Stanza packet) throws NotConnectedException {
            Message message = (Message) packet;
            DeliveryReceiptRequest.addTo(message);
        }
    };

    /**
     * Enables automatic requests of delivery receipts for outgoing messages of type 'normal', 'chat' or 'headline.
     * 
     * @since 4.1
     * @see #dontAutoAddDeliveryReceiptRequests()
     */
    public void autoAddDeliveryReceiptRequests() {
        connection().addPacketSendingListener(AUTO_ADD_DELIVERY_RECEIPT_REQUESTS_LISTENER,
                        MessageTypeFilter.NORMAL_OR_CHAT_OR_HEADLINE);
    }

    /**
     * Disables automatically requests of delivery receipts for outgoing messages.
     * 
     * @since 4.1
     * @see #autoAddDeliveryReceiptRequests()
     */
    public void dontAutoAddDeliveryReceiptRequests() {
        connection().removePacketSendingListener(AUTO_ADD_DELIVERY_RECEIPT_REQUESTS_LISTENER);
    }

    /**
     * Test if a message requires a delivery receipt.
     *
     * @param message Packet object to check for a DeliveryReceiptRequest
     *
     * @return true if a delivery receipt was requested
     */
    public static boolean hasDeliveryReceiptRequest(Message message) {
        return (DeliveryReceiptRequest.from(message) != null);
    }

    /**
     * Add a delivery receipt request to an outgoing packet.
     *
     * Only message packets may contain receipt requests as of XEP-0184,
     * therefore only allow Message as the parameter type.
     *
     * @param m Message object to add a request to
     * @return the Message ID which will be used as receipt ID
     * @deprecated use {@link DeliveryReceiptRequest#addTo(Message)}
     */
    @Deprecated
    public static String addDeliveryReceiptRequest(Message m) {
        return DeliveryReceiptRequest.addTo(m);
    }

    /**
     * Create and return a new message including a delivery receipt extension for the given message.
     *
     * @param messageWithReceiptRequest the given message with a receipt request extension.
     * @return a new message with a receipt.
     * @since 4.1
     */
    public static Message receiptMessageFor(Message messageWithReceiptRequest) {
        Message message = new Message(messageWithReceiptRequest.getFrom(), messageWithReceiptRequest.getType());
        message.addExtension(new DeliveryReceipt(messageWithReceiptRequest.getStanzaId()));
        return message;
    }
}
