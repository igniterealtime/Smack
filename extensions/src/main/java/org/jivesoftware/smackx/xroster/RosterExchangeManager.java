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
package org.jivesoftware.smackx.xroster;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.xroster.packet.RosterExchange;

/**
 *
 * Manages Roster exchanges. A RosterExchangeManager provides a high level access to send 
 * rosters, roster groups and roster entries to XMPP clients. It also provides an easy way
 * to hook up custom logic when entries are received from another XMPP client through 
 * RosterExchangeListeners.
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeManager {

    public final static String NAMESPACE = "jabber:x:roster";
    public final static String ELEMENT = "x";

    private final static Map<Connection, RosterExchangeManager> INSTANCES =
                    Collections.synchronizedMap(new WeakHashMap<Connection, RosterExchangeManager>());

    private final static PacketFilter PACKET_FILTER = new PacketExtensionFilter(ELEMENT, NAMESPACE);

    private final Set<RosterExchangeListener> rosterExchangeListeners = Collections.synchronizedSet(new HashSet<RosterExchangeListener>());

    private final WeakReference<Connection> weakRefConnection;
    private final PacketListener packetListener;

    public synchronized static RosterExchangeManager getInstanceFor(Connection connection) {
        RosterExchangeManager rosterExchangeManager = INSTANCES.get(connection);
        if (rosterExchangeManager == null) {
            rosterExchangeManager = new RosterExchangeManager(connection);
        }
        return rosterExchangeManager;
    }

    /**
     * Creates a new roster exchange manager.
     *
     * @param con a Connection which is used to send and receive messages.
     */
    public RosterExchangeManager(Connection connection) {
        weakRefConnection = new WeakReference<Connection>(connection);
        // Listens for all roster exchange packets and fire the roster exchange listeners.
        packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                RosterExchange rosterExchange =
                    (RosterExchange) message.getExtension(ELEMENT, NAMESPACE);
                // Fire event for roster exchange listeners
                fireRosterExchangeListeners(message.getFrom(), rosterExchange.getRosterEntries());
            };

        };
        connection.addPacketListener(packetListener, PACKET_FILTER);
    }

    /**
     * Adds a listener to roster exchanges. The listener will be fired anytime roster entries
     * are received from remote XMPP clients.
     *
     * @param rosterExchangeListener a roster exchange listener.
     */
    public void addRosterListener(RosterExchangeListener rosterExchangeListener) {
        rosterExchangeListeners.add(rosterExchangeListener);
    }

    /**
     * Removes a listener from roster exchanges. The listener will be fired anytime roster 
     * entries are received from remote XMPP clients.
     *
     * @param rosterExchangeListener a roster exchange listener..
     */
    public void removeRosterListener(RosterExchangeListener rosterExchangeListener) {
        rosterExchangeListeners.remove(rosterExchangeListener);
    }

    /**
     * Sends a roster to userID. All the entries of the roster will be sent to the
     * target user.
     * 
     * @param roster the roster to send
     * @param targetUserID the user that will receive the roster entries
     */
    public void send(Roster roster, String targetUserID) {
        // Create a new message to send the roster
        Message msg = new Message(targetUserID);
        // Create a RosterExchange Package and add it to the message
        RosterExchange rosterExchange = new RosterExchange(roster);
        msg.addExtension(rosterExchange);

        Connection connection = weakRefConnection.get();
        // Send the message that contains the roster
        connection.sendPacket(msg);
    }

    /**
     * Sends a roster entry to userID.
     * 
     * @param rosterEntry the roster entry to send
     * @param targetUserID the user that will receive the roster entries
     */
    public void send(RosterEntry rosterEntry, String targetUserID) {
        // Create a new message to send the roster
        Message msg = new Message(targetUserID);
        // Create a RosterExchange Package and add it to the message
        RosterExchange rosterExchange = new RosterExchange();
        rosterExchange.addRosterEntry(rosterEntry);
        msg.addExtension(rosterExchange);

        Connection connection = weakRefConnection.get();
        // Send the message that contains the roster
        connection.sendPacket(msg);
    }

    /**
     * Sends a roster group to userID. All the entries of the group will be sent to the 
     * target user.
     * 
     * @param rosterGroup the roster group to send
     * @param targetUserID the user that will receive the roster entries
     */
    public void send(RosterGroup rosterGroup, String targetUserID) {
        // Create a new message to send the roster
        Message msg = new Message(targetUserID);
        // Create a RosterExchange Package and add it to the message
        RosterExchange rosterExchange = new RosterExchange();
        for (RosterEntry entry : rosterGroup.getEntries()) {
            rosterExchange.addRosterEntry(entry);
        }
        msg.addExtension(rosterExchange);

        Connection connection = weakRefConnection.get();
        // Send the message that contains the roster
        connection.sendPacket(msg);
    }

    /**
     * Fires roster exchange listeners.
     */
    private void fireRosterExchangeListeners(String from, Iterator<RemoteRosterEntry> remoteRosterEntries) {
        RosterExchangeListener[] listeners = null;
        synchronized (rosterExchangeListeners) {
            listeners = new RosterExchangeListener[rosterExchangeListeners.size()];
            rosterExchangeListeners.toArray(listeners);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].entriesReceived(from, remoteRosterEntries);
        }
    }
}
