/**
 *
 * Copyright 2003-2006 Jive Software.
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

package org.jivesoftware.smackx.muc;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jxmpp.util.XmppStringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A <code>RoomListenerMultiplexor</code> multiplexes incoming packets on
 * a <code>XMPPConnection</code> using a single listener/filter pair.
 * A single <code>RoomListenerMultiplexor</code> is created for each
 * {@link org.jivesoftware.smack.XMPPConnection} that has joined MUC rooms
 * within its session.
 *
 * @author Larry Kirschner
 */
class RoomListenerMultiplexor extends Manager {

    // We use a WeakHashMap so that the GC can collect the monitor when the
    // connection is no longer referenced by any object.
    private static final Map<XMPPConnection, RoomListenerMultiplexor> monitors = new WeakHashMap<XMPPConnection, RoomListenerMultiplexor>();

    private final RoomMultiplexFilter filter;
    private final RoomMultiplexListener listener;

    /**
     * Returns a new or existing RoomListenerMultiplexor for a given connection.
     *
     * @param conn the connection to monitor for room invitations.
     * @return a new or existing RoomListenerMultiplexor for a given connection.
     */
    public static synchronized RoomListenerMultiplexor getRoomMultiplexor(XMPPConnection conn) {
        RoomListenerMultiplexor rlm = monitors.get(conn);
        if (rlm == null) {
            rlm = new RoomListenerMultiplexor(conn, new RoomMultiplexFilter(),
                            new RoomMultiplexListener());
        }
        // Return the InvitationsMonitor that monitors the connection
        return rlm;
    }

    /**
     * All access should be through
     * the static method {@link #getRoomMultiplexor(XMPPConnection)}.
     */
    private RoomListenerMultiplexor(XMPPConnection connection, RoomMultiplexFilter filter,
            RoomMultiplexListener listener) {
        super(connection);
        connection.addPacketListener(listener, filter);

        this.filter = filter;
        this.listener = listener;
        monitors.put(connection, this);
    }

    public void addRoom(String address, PacketMultiplexListener roomListener) {
        filter.addRoom(address);
        listener.addRoom(address, roomListener);
    }

    public void removeRoom(String address) {
        filter.removeRoom(address);
        listener.removeRoom(address);
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
            return roomAddressTable.containsKey(XmppStringUtils.parseBareAddress(from).toLowerCase(Locale.US));
        }

        public void addRoom(String address) {
            if (address == null) {
                return;
            }
            roomAddressTable.put(address.toLowerCase(Locale.US), address);
        }

        public void removeRoom(String address) {
            if (address == null) {
                return;
            }
            roomAddressTable.remove(address.toLowerCase(Locale.US));
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

        public void processPacket(Packet p) throws NotConnectedException {
            String from = p.getFrom();
            if (from == null) {
                return;
            }

            PacketMultiplexListener listener =
                    roomListenersByAddress.get(XmppStringUtils.parseBareAddress(from).toLowerCase(Locale.US));

            if (listener != null) {
                listener.processPacket(p);
            }
        }

        public void addRoom(String address, PacketMultiplexListener listener) {
            if (address == null) {
                return;
            }
            roomListenersByAddress.put(address.toLowerCase(Locale.US), listener);
        }

        public void removeRoom(String address) {
            if (address == null) {
                return;
            }
            roomListenersByAddress.remove(address.toLowerCase(Locale.US));
        }
    }
}
