/**
 *
 * Copyright 2012-2014 Florian Schmaus
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
package org.jivesoftware.smackx.ping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackError;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.packet.Pong;

/**
 * Implements the XMPP Ping as defined by XEP-0199. The XMPP Ping protocol allows one entity to
 * ping any other entity by simply sending a ping to the appropriate JID. PingManger also
 * periodically sends XMPP pings to the server every 30 minutes to avoid NAT timeouts and to test
 * the connection status.
 * 
 * @author Florian Schmaus
 * @see <a href="http://www.xmpp.org/extensions/xep-0199.html">XEP-0199:XMPP Ping</a>
 */
public class PingManager extends Manager {
    public static final String NAMESPACE = "urn:xmpp:ping";

    private static final Logger LOGGER = Logger.getLogger(PingManager.class.getName());

    private static final Map<Connection, PingManager> INSTANCES = Collections
            .synchronizedMap(new WeakHashMap<Connection, PingManager>());

    private static final PacketFilter PING_PACKET_FILTER = new AndFilter(
                    new PacketTypeFilter(Ping.class), new IQTypeFilter(Type.GET));

    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Retrieves a {@link PingManager} for the specified {@link Connection}, creating one if it doesn't already
     * exist.
     * 
     * @param connection
     * The connection the manager is attached to.
     * @return The new or existing manager.
     */
    public synchronized static PingManager getInstanceFor(Connection connection) {
        PingManager pingManager = INSTANCES.get(connection);
        if (pingManager == null) {
            pingManager = new PingManager(connection);
        }
        return pingManager;
    }

    private static int defaultPingInterval = 60 * 30;

    /**
     * Set the default ping interval which will be used for new connections.
     *
     * @param interval the interval in seconds
     */
    public static void setDefaultPingInterval(int interval) {
        defaultPingInterval = interval;
    }

    private final Set<PingFailedListener> pingFailedListeners = Collections
                    .synchronizedSet(new HashSet<PingFailedListener>());

    /**
     * The interval in seconds between pings are send to the users server.
     */
    private int pingInterval = defaultPingInterval;

    /**
     * The time in milliseconds the last successful ping was send to the users server.
     */
    private volatile long lastSuccessfulAutomaticPing = -1;

    private ScheduledFuture<?> nextAutomaticPing;

    /**
     * The time in milliseconds the last manual ping as successful. 
     */
    private long lastSuccessfulManualPing = -1;

    private PingManager(Connection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(PingManager.NAMESPACE);
        INSTANCES.put(connection, this);

        connection.addPacketListener(new PacketListener() {
            // Send a Pong for every Ping
            @Override
            public void processPacket(Packet packet) {
                Pong pong = new Pong(packet);
                connection().sendPacket(pong);
            }
        }, PING_PACKET_FILTER);
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void connectionClosed() {
                maybeStopPingServerTask();
            }
            @Override
            public void connectionClosedOnError(Exception arg0) {
                maybeStopPingServerTask();
            }
            @Override
            public void reconnectionSuccessful() {
                maybeSchedulePingServerTask();
            }
            @Override
            public void reconnectingIn(int seconds) {}
            @Override
            public void reconnectionFailed(Exception e) {}
        });
        maybeSchedulePingServerTask();
    }

    /**
     * Pings the given jid. This method will return false if an error occurs.  The exception 
     * to this, is a server ping, which will always return true if the server is reachable, 
     * event if there is an error on the ping itself (i.e. ping not supported).
     * <p>
     * Use {@link #isPingSupported(String)} to determine if XMPP Ping is supported 
     * by the entity.
     * 
     * @param jid The id of the entity the ping is being sent to
     * @param pingTimeout The time to wait for a reply
     * @return true if a reply was received from the entity, false otherwise.
     */
    public boolean ping(String jid, long pingTimeout) {
        Ping ping = new Ping(jid);
        try {
            connection().createPacketCollectorAndSend(ping).nextResultOrThrow();
        }
        catch (XMPPException exc) {
            return (jid.equals(connection().getServiceName()) && (exc.getSmackError() != SmackError.NO_RESPONSE_FROM_SERVER));
        }
        return true;
    }

    /**
     * Same as calling {@link #ping(String, long)} with the defaultpacket reply 
     * timeout.
     * 
     * @param jid The id of the entity the ping is being sent to
     * @return true if a reply was received from the entity, false otherwise.
     */
    public boolean ping(String jid) {
        return ping(jid, connection().getPacketReplyTimeout());
    }

    /**
     * Query the specified entity to see if it supports the Ping protocol (XEP-0199)
     * 
     * @param jid The id of the entity the query is being sent to
     * @return true if it supports ping, false otherwise.
     * @throws XMPPException An XMPP related error occurred during the request 
     */
    public boolean isPingSupported(String jid) throws XMPPException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, PingManager.NAMESPACE);
    }

    /**
     * Pings the server. This method will return true if the server is reachable.  It
     * is the equivalent of calling <code>ping</code> with the XMPP domain.
     * <p>
     * Unlike the {@link #ping(String)} case, this method will return true even if 
     * {@link #isPingSupported(String)} is false.
     * 
     * @return true if a reply was received from the server, false otherwise.
     */
    public boolean pingMyServer() {
        return pingMyServer(true);
    }

    /**
     * Pings the server. This method will return true if the server is reachable.  It
     * is the equivalent of calling <code>ping</code> with the XMPP domain.
     * <p>
     * Unlike the {@link #ping(String)} case, this method will return true even if
     * {@link #isPingSupported(String)} is false.
     *
     * @param notifyListeners Notify the PingFailedListener in case of error if true
     * @return
     */
    public boolean pingMyServer(boolean notifyListeners) {
        boolean res = ping(connection().getServiceName());
        if (res) {
            pongReceived();
        } else if (notifyListeners) {
            for (PingFailedListener l : pingFailedListeners)
                l.pingFailed();
        }
        return res;
    }

    /**
     * Set the interval between the server is automatic pinged. A negative value disables automatic server pings.
     *
     * @param pingInterval the interval between the ping
     */
    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
        maybeSchedulePingServerTask();
    }

    /**
     * Get the current ping interval.
     *
     * @return the interval between pings in seconds
     */
    public int getPingInterval() {
        return pingInterval;
    }

    /**
     * Register a new PingFailedListener
     *
     * @param listener the listener to invoke
     */
    public void registerPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.add(listener);
    }

    /**
     * Unregister a PingFailedListener
     *
     * @param listener the listener to remove
     */
    public void unregisterPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.remove(listener);
    }

    /**
     * Returns the time of the last successful Ping with the
     * users server. If there was no successful Ping (e.g. because this
     * feature is disabled) -1 will be returned.
     * 
     * @return
     */
    public long getLastSuccessfulPing() {
        return Math.max(lastSuccessfulAutomaticPing, lastSuccessfulManualPing);
    }

    /**
     * Cancels any existing periodic ping task if there is one and schedules a new ping task if
     * pingInterval is greater then zero.
     */
    private synchronized void maybeSchedulePingServerTask() {
        maybeStopPingServerTask();
        if (pingInterval > 0) {
            LOGGER.fine("Scheduling ServerPingTask in " + pingInterval + " seconds");
            nextAutomaticPing = connection().schedule(pingServerRunnable, pingInterval, TimeUnit.SECONDS);
        }
    }

    private void maybeStopPingServerTask() {
        if (nextAutomaticPing != null) {
            nextAutomaticPing.cancel(true);
            nextAutomaticPing = null;
        }
    }

    private void pongReceived() {
        lastSuccessfulManualPing = System.currentTimeMillis();
    }

    private final Runnable pingServerRunnable = new Runnable() {
        private static final int DELTA = 1000; // 1 seconds
        private static final int TRIES = 3; // 3 tries

        public void run() {
            LOGGER.fine("ServerPingTask run()");
            Connection connection = connection();
            if (connection == null) {
                // connection has been collected by GC
                // which means we can stop the thread by breaking the loop
                return;
            }
            if (connection.isAuthenticated()) {
                boolean res = false;

                for (int i = 0; i < TRIES; i++) {
                    if (i != 0) {
                        try {
                            Thread.sleep(DELTA);
                        } catch (InterruptedException e) {
                            // We received an interrupt
                            // This only happens if we should stop pinging
                            return;
                        }
                    }
                    res = pingMyServer(false);
                    // stop when we receive a pong back
                    if (res) {
                        lastSuccessfulAutomaticPing = System.currentTimeMillis();
                        break;
                    }
                }
                LOGGER.fine("ServerPingTask res=" + res);
                if (!res) {
                    for (PingFailedListener l : pingFailedListeners) {
                        l.pingFailed();
                    }
                } else {
                    // Ping was successful, wind-up the periodic task again
                    maybeSchedulePingServerTask();
                }
            } else {
                LOGGER.warning("ServerPingTask: Connection was not authenticated");
            }
        }
    };
}
