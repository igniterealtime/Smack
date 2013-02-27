/**
 * Copyright 2012-2013 Florian Schmaus
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

package org.jivesoftware.smackx.ping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.packet.Pong;

/**
 * Implements the XMPP Ping as defined by XEP-0199. This protocol offers an
 * alternative to the traditional 'white space ping' approach of determining the
 * availability of an entity. The XMPP Ping protocol allows ping messages to be
 * send in a more XML-friendly approach, which can be used over more than one
 * hop in the communication path.
 * 
 * @author Florian Schmaus
 * @see <a href="http://www.xmpp.org/extensions/xep-0199.html">XEP-0199:XMPP
 *      Ping</a>
 */
public class PingManager {

    public static final String NAMESPACE = "urn:xmpp:ping";
    public static final String ELEMENT = "ping";


    private static Map<Connection, PingManager> instances =
            Collections.synchronizedMap(new WeakHashMap<Connection, PingManager>());

    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new PingManager(connection);
            }
        });
    }

    private ScheduledExecutorService periodicPingExecutorService;
    private Connection connection;
    private int pingInterval = SmackConfiguration.getDefaultPingInterval();
    private Set<PingFailedListener> pingFailedListeners = Collections
            .synchronizedSet(new HashSet<PingFailedListener>());
    private ScheduledFuture<?> periodicPingTask;
    protected volatile long lastSuccessfulPingByTask = -1;


    // Ping Flood protection
    private long pingMinDelta = 100;
    private long lastPingStamp = 0; // timestamp of the last received ping

    // Timestamp of the last pong received, either from the server or another entity
    // Note, no need to synchronize this value, it will only increase over time
    private long lastSuccessfulManualPing = -1;

    private PingManager(Connection connection) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE);
        this.connection = connection;
        init();
    }

    private void init() {
        periodicPingExecutorService = new ScheduledThreadPoolExecutor(1);
        PacketFilter pingPacketFilter = new PacketTypeFilter(Ping.class);
        connection.addPacketListener(new PacketListener() {
            /**
             * Sends a Pong for every Ping
             */
            public void processPacket(Packet packet) {
                if (pingMinDelta > 0) {
                    // Ping flood protection enabled
                    long currentMillies = System.currentTimeMillis();
                    long delta = currentMillies - lastPingStamp;
                    lastPingStamp = currentMillies;
                    if (delta < pingMinDelta) {
                        return;
                    }
                }
                Pong pong = new Pong((Ping)packet);
                connection.sendPacket(pong);
            }
        }
        , pingPacketFilter);
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
            public void reconnectingIn(int seconds) {
            }

            @Override
            public void reconnectionFailed(Exception e) {
            }
        });
        instances.put(connection, this);
        maybeSchedulePingServerTask();
    }

    public static PingManager getInstanceFor(Connection connection) {
        PingManager pingManager = instances.get(connection);

        if (pingManager == null) {
            pingManager = new PingManager(connection);
        }

        return pingManager;
    }

    public void setPingIntervall(int pingIntervall) {
        this.pingInterval = pingIntervall;
    }

    public int getPingIntervall() {
        return pingInterval;
    }
    
    public void registerPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.add(listener);
    }
    
    public void unregisterPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.remove(listener);
    }
    
    public void disablePingFloodProtection() {
        setPingMinimumInterval(-1);
    }
    
    public void setPingMinimumInterval(long ms) {
        this.pingMinDelta = ms;
    }
    
    public long getPingMinimumInterval() {
        return this.pingMinDelta;
    }
    
    /**
     * Pings the given jid and returns the IQ response which is either of 
     * IQ.Type.ERROR or IQ.Type.RESULT. If we are not connected or if there was
     * no reply, null is returned.
     * 
     * You should use isPingSupported(jid) to determine if XMPP Ping is 
     * supported by the user.
     * 
     * @param jid
     * @param pingTimeout
     * @return
     */
    public IQ ping(String jid, long pingTimeout) {
        // Make sure we actually connected to the server
        if (!connection.isAuthenticated())
            return null;
        
        Ping ping = new Ping(connection.getUser(), jid);
        
        PacketCollector collector =
                connection.createPacketCollector(new PacketIDFilter(ping.getPacketID()));
        
        connection.sendPacket(ping);
        
        IQ result = (IQ) collector.nextResult(pingTimeout);
        
        collector.cancel();
        return result;
    }
    
    /**
     * Pings the given jid and returns the IQ response with the default
     * packet reply timeout
     * 
     * @param jid
     * @return
     */
    public IQ ping(String jid) {
        return ping(jid, SmackConfiguration.getPacketReplyTimeout());
    }
    
    /**
     * Pings the given Entity.
     * 
     * Note that XEP-199 shows that if we receive a error response
     * service-unavailable there is no way to determine if the response was send
     * by the entity (e.g. a user JID) or from a server in between. This is
     * intended behavior to avoid presence leaks.
     * 
     * Always use isPingSupported(jid) to determine if XMPP Ping is supported
     * by the entity.
     * 
     * @param jid
     * @return True if a pong was received, otherwise false
     */
    public boolean pingEntity(String jid, long pingTimeout) {
        IQ result = ping(jid, pingTimeout);

        if (result == null || result.getType() == IQ.Type.ERROR) {
            return false;
        }
        pongReceived();
        return true;
    }
    
    public boolean pingEntity(String jid) {
        return pingEntity(jid, SmackConfiguration.getPacketReplyTimeout());
    }
    
    /**
     * Pings the user's server. Will notify the registered 
     * pingFailedListeners in case of error.
     * 
     * If we receive as response, we can be sure that it came from the server.
     * 
     * @return true if successful, otherwise false
     */
    public boolean pingMyServer(long pingTimeout) {
        IQ result = ping(connection.getServiceName(), pingTimeout);

        if (result == null) {
            for (PingFailedListener l : pingFailedListeners) {
                l.pingFailed();
            }
            return false;
        }
        // Maybe not really a pong, but an answer is an answer
        pongReceived();
        return true;
    }
    
    /**
     * Pings the user's server with the PacketReplyTimeout as defined
     * in SmackConfiguration.
     * 
     * @return true if successful, otherwise false
     */
    public boolean pingMyServer() {
        return pingMyServer(SmackConfiguration.getPacketReplyTimeout());
    }
    
    /**
     * Returns true if XMPP Ping is supported by a given JID
     * 
     * @param jid
     * @return
     */
    public boolean isPingSupported(String jid) {
        try {
            DiscoverInfo result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(jid);
            return result.containsFeature(NAMESPACE);
        }
        catch (XMPPException e) {
            return false;
        }
    }
    
    /**
     * Returns the time of the last successful Ping Pong with the 
     * users server. If there was no successful Ping (e.g. because this
     * feature is disabled) -1 will be returned.
     *  
     * @return
     */
    public long getLastSuccessfulPing() {
        return Math.max(lastSuccessfulPingByTask, lastSuccessfulManualPing);
    }
    
    protected Set<PingFailedListener> getPingFailedListeners() {
        return pingFailedListeners;
    }

    /**
     * Cancels any existing periodic ping task if there is one and schedules a new ping task if pingInterval is greater
     * then zero.
     * 
     */
    protected synchronized void maybeSchedulePingServerTask() {
        maybeStopPingServerTask();
        if (pingInterval > 0) {
            periodicPingTask = periodicPingExecutorService.schedule(new ServerPingTask(connection), pingInterval,
                    TimeUnit.SECONDS);
        }
    }

    private void maybeStopPingServerTask() {
        if (periodicPingTask != null) {
            periodicPingTask.cancel(true);
            periodicPingTask = null;
        }
    }

    private void pongReceived() {
        lastSuccessfulManualPing = System.currentTimeMillis();
    }
}
