/**
 * Copyright 2013 Georg Lukas
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

package org.jivesoftware.smackx.receipts;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;

/**
 * Packet extension for XEP-0184: Message Delivery Receipts. This class implements
 * the manager for {@link DeliveryReceipt} support, enabling and disabling of
 * automatic DeliveryReceipt transmission.
 *
 * @author Georg Lukas
 */
public class DeliveryReceiptManager implements PacketListener {

    private static Map<Connection, DeliveryReceiptManager> instances =
            Collections.synchronizedMap(new WeakHashMap<Connection, DeliveryReceiptManager>());

    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new DeliveryReceiptManager(connection);
            }
        });
    }

    private Connection connection;
    private boolean auto_receipts_enabled = false;
    private Set<ReceiptReceivedListener> receiptReceivedListeners = Collections
            .synchronizedSet(new HashSet<ReceiptReceivedListener>());

    private DeliveryReceiptManager(Connection connection) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(DeliveryReceipt.NAMESPACE);
        this.connection = connection;
        instances.put(connection, this);

        // register listener for delivery receipts and requests
        connection.addPacketListener(this, new PacketExtensionFilter(DeliveryReceipt.NAMESPACE));
    }

    /**
     * Obtain the DeliveryReceiptManager responsible for a connection.
     *
     * @param connection the connection object.
     *
     * @return the DeliveryReceiptManager instance for the given connection
     */
    synchronized public static DeliveryReceiptManager getInstanceFor(Connection connection) {
        DeliveryReceiptManager receiptManager = instances.get(connection);

        if (receiptManager == null) {
            receiptManager = new DeliveryReceiptManager(connection);
        }

        return receiptManager;
    }

    /**
     * Returns true if Delivery Receipts are supported by a given JID
     * 
     * @param jid
     * @return true if supported
     */
    public boolean isSupported(String jid) {
        try {
            DiscoverInfo result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(jid);
            return result.containsFeature(DeliveryReceipt.NAMESPACE);
        }
        catch (XMPPException e) {
            return false;
        }
    }

    // handle incoming receipts and receipt requests
    @Override
    public void processPacket(Packet packet) {
        DeliveryReceipt dr = (DeliveryReceipt)packet.getExtension(
                DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE);
        if (dr != null) {
            // notify listeners of incoming receipt
            for (ReceiptReceivedListener l : receiptReceivedListeners) {
                l.onReceiptReceived(packet.getFrom(), packet.getTo(), dr.getId());
            }

        }

        // if enabled, automatically send a receipt
        if (auto_receipts_enabled) {
            DeliveryReceiptRequest drr = (DeliveryReceiptRequest)packet.getExtension(
                    DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE);
            if (drr != null) {
                Message ack = new Message(packet.getFrom(), Message.Type.normal);
                ack.addExtension(new DeliveryReceipt(packet.getPacketID()));
                connection.sendPacket(ack);
            }
        }
    }

    /**
     * Configure whether the {@link DeliveryReceiptManager} should automatically
     * reply to incoming {@link DeliveryReceipt}s. By default, this feature is off.
     *
     * @param new_state whether automatic transmission of
     *                  DeliveryReceipts should be enabled or disabled
     */
    public void setAutoReceiptsEnabled(boolean new_state) {
        auto_receipts_enabled = new_state;
    }

    /**
     * Helper method to enable automatic DeliveryReceipt transmission.
     */
    public void enableAutoReceipts() {
        setAutoReceiptsEnabled(true);
    }

    /**
     * Helper method to disable automatic DeliveryReceipt transmission.
     */
    public void disableAutoReceipts() {
        setAutoReceiptsEnabled(false);
    }

    /**
     * Check if AutoReceipts are enabled on this connection.
     */
    public boolean getAutoReceiptsEnabled() {
        return this.auto_receipts_enabled;
    }

    /**
     * Get informed about incoming delivery receipts with a {@link ReceiptReceivedListener}.
     * 
     * @param listener the listener to be informed about new receipts
     */
    public void registerReceiptReceivedListener(ReceiptReceivedListener listener) {
        receiptReceivedListeners.add(listener);
    }

    /**
     * Stop getting informed about incoming delivery receipts.
     * 
     * @param listener the listener to be removed
     */
    public void unregisterReceiptReceivedListener(ReceiptReceivedListener listener) {
        receiptReceivedListeners.remove(listener);
    }

    /**
     * Interface for received receipt notifications.
     * 
     * Implement this and add a listener to get notified. 
     */
    public static interface ReceiptReceivedListener {
        void onReceiptReceived(String fromJid, String toJid, String receiptId);
    }


    /**
     * Test if a packet requires a delivery receipt.
     *
     * @param p Packet object to check for a DeliveryReceiptRequest
     *
     * @return true if a delivery receipt was requested
     */
    public static boolean hasDeliveryReceiptRequest(Packet p) {
        return (p.getExtension(DeliveryReceiptRequest.ELEMENT,
                    DeliveryReceipt.NAMESPACE) != null);
    }

    /**
     * Add a delivery receipt request to an outgoing packet.
     *
     * Only message packets may contain receipt requests as of XEP-0184,
     * therefore only allow Message as the parameter type.
     *
     * @param m Message object to add a request to
     */
    public static void addDeliveryReceiptRequest(Message m) {
        m.addExtension(new DeliveryReceiptRequest());
    }
}
