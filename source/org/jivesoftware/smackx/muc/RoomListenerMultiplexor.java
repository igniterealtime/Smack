/**
 * $RCSfile$
 * $Revision: 2779 $
 * $Date: 2005-09-05 17:00:45 -0300 (Mon, 05 Sep 2005) $
 *
 * Copyright 2003-2006 Jive Software.
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

package org.jivesoftware.smackx.muc;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A <code>RoomListenerMultiplexor</code> multiplexes incoming packets on
 * an <code>XMPPConnection</code> using a single listener/filter pair.
 * A single <code>RoomListenerMultiplexor</code> is created for each
 * {@link org.jivesoftware.smack.XMPPConnection} that has joined MUC rooms
 * within its session.
 *
 * @author Larry Kirschner
 */
class RoomListenerMultiplexor implements ConnectionListener {

    // We use a WeakHashMap so that the GC can collect the monitor when the
    // connection is no longer referenced by any object.
    private static final Map<XMPPConnection, WeakReference<RoomListenerMultiplexor>> monitors =
            new WeakHashMap<XMPPConnection, WeakReference<RoomListenerMultiplexor>>();

    private XMPPConnection connection;
    private RoomMultiplexFilter filter;
    private RoomMultiplexListener listener;

    /**
     * Returns a new or existing RoomListenerMultiplexor for a given connection.
     *
     * @param conn the connection to monitor for room invitations.
     * @return a new or existing RoomListenerMultiplexor for a given connection.
     */
    public static RoomListenerMultiplexor getRoomMultiplexor(XMPPConnection conn) {
        synchronized (monitors) {
            if (!monitors.containsKey(conn)) {
                RoomListenerMultiplexor rm = new RoomListenerMultiplexor(conn, new RoomMultiplexFilter(),
                        new RoomMultiplexListener());

                rm.init();

                // We need to use a WeakReference because the monitor references the
                // connection and this could prevent the GC from collecting the monitor
                // when no other object references the monitor
                monitors.put(conn, new WeakReference<RoomListenerMultiplexor>(rm));
            }
            // Return the InvitationsMonitor that monitors the connection
            return monitors.get(conn).get();
        }
    }

    /**
     * All access should be through
     * the static method {@link #getRoomMultiplexor(XMPPConnection)}.
     */
    private RoomListenerMultiplexor(XMPPConnection connection, RoomMultiplexFilter filter,
            RoomMultiplexListener listener) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection is null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter is null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null");
        }
        this.connection = connection;
        this.filter = filter;
        this.listener = listener;
    }

    public void addRoom(String address, PacketMultiplexListener roomListener) {
        filter.addRoom(address);
        listener.addRoom(address, roomListener);
    }

    public void connectionClosed() {
        cancel();
    }

    public void connectionClosedOnError(Exception e) {
        cancel();
    }

    public void reconnectingIn(int seconds) {
        // ignore
    }

    public void reconnectionSuccessful() {
        // ignore
    }

    public void reconnectionFailed(Exception e) {
        // ignore
    }

    /**
     * Initializes the listeners to detect received room invitations and to detect when the
     * connection gets closed. As soon as a room invitation is received the invitations
     * listeners will be fired. When the connection gets closed the monitor will remove
     * his listeners on the connection.
     */
    public void init() {
        connection.addConnectionListener(this);
        connection.addPacketListener(listener, filter);
    }

    public void removeRoom(String address) {
        filter.removeRoom(address);
        listener.removeRoom(address);
    }

    /**
     * Cancels all the listeners that this InvitationsMonitor has added to the connection.
     */
    private void cancel() {
        connection.removeConnectionListener(this);
        connection.removePacketListener(listener);
    }

    /**
     * The single <code>XMPPConnection</code>-level <code>PacketFilter</code> used by a {@link RoomListenerMultiplexor}
     * for all muc chat rooms on an <code>XMPPConnection</code>.
     * Each time a muc chat room is added to/removed from an
     * <code>XMPPConnection</code> the address for that chat room
     * is added to/removed from that <code>XMPPConnection</code>'s
     * <code>RoomMultiplexFilter</code>.
     */
    private static class RoomMultiplexFilter implements PacketFilter {

        private Map<String, String> roomAddressTable = new ConcurrentHashMap<String, String>();

        public boolean accept(Packet p) {
            String from = p.getFrom();
            if (from == null) {
                return false;
            }
            return roomAddressTable.containsKey(StringUtils.parseBareAddress(from).toLowerCase());
        }

        public void addRoom(String address) {
            if (address == null) {
                return;
            }
            roomAddressTable.put(address.toLowerCase(), address);
        }

        public void removeRoom(String address) {
            if (address == null) {
                return;
            }
            roomAddressTable.remove(address.toLowerCase());
        }
    }

    /**
     * The single <code>XMPPConnection</code>-level <code>PacketListener</code>
     * used by a {@link RoomListenerMultiplexor}
     * for all muc chat rooms on an <code>XMPPConnection</code>.
     * Each time a muc chat room is added to/removed from an
     * <code>XMPPConnection</code> the address and listener for that chat room
     * are added to/removed from that <code>XMPPConnection</code>'s
     * <code>RoomMultiplexListener</code>.
     *
     * @author Larry Kirschner
     */
    private static class RoomMultiplexListener implements PacketListener {

        private Map<String, PacketMultiplexListener> roomListenersByAddress =
                new ConcurrentHashMap<String, PacketMultiplexListener>();

        public void processPacket(Packet p) {
            String from = p.getFrom();
            if (from == null) {
                return;
            }

            PacketMultiplexListener listener =
                    roomListenersByAddress.get(StringUtils.parseBareAddress(from).toLowerCase());

            if (listener != null) {
                listener.processPacket(p);
            }
        }

        public void addRoom(String address, PacketMultiplexListener listener) {
            if (address == null) {
                return;
            }
            roomListenersByAddress.put(address.toLowerCase(), listener);
        }

        public void removeRoom(String address) {
            if (address == null) {
                return;
            }
            roomListenersByAddress.remove(address.toLowerCase());
        }
    }
}
