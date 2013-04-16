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

package org.jivesoftware.smack.keepalive;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.ping.PingFailedListener;
import org.jivesoftware.smack.ping.packet.Ping;

/**
 * Using an implementation of <a href="http://www.xmpp.org/extensions/xep-0199.html">XMPP Ping (XEP-0199)</a>. This
 * class provides keepalive functionality with the server that will periodically "ping" the server to maintain and/or
 * verify that the connection still exists.
 * <p>
 * The ping is done at the application level and is therefore protocol agnostic. It will thus work for both standard TCP
 * connections as well as BOSH or any other transport protocol. It will also work regardless of whether the server
 * supports the Ping extension, since an error response to the ping serves the same purpose as a pong.
 * 
 * @author Florian Schmaus
 */
public class KeepAliveManager {
    private static Map<Connection, KeepAliveManager> instances = new HashMap<Connection, KeepAliveManager>();
    private static volatile ScheduledExecutorService periodicPingExecutorService;
    
    static {
        if (SmackConfiguration.getKeepAliveInterval() > 0) {
            Connection.addConnectionCreationListener(new ConnectionCreationListener() {
                public void connectionCreated(Connection connection) {
                    new KeepAliveManager(connection);
                }
            });
        }
    }

    private Connection connection;
    private long pingInterval = SmackConfiguration.getKeepAliveInterval();
    private Set<PingFailedListener> pingFailedListeners = Collections.synchronizedSet(new HashSet<PingFailedListener>());
    private volatile ScheduledFuture<?> periodicPingTask;
    private volatile long lastSuccessfulContact = -1;

    /**
     * Retrieves a {@link KeepAliveManager} for the specified {@link Connection}, creating one if it doesn't already
     * exist.
     * 
     * @param connection
     * The connection the manager is attached to.
     * @return The new or existing manager.
     */
    public synchronized static KeepAliveManager getInstanceFor(Connection connection) {
        KeepAliveManager pingManager = instances.get(connection);

        if (pingManager == null) {
            pingManager = new KeepAliveManager(connection);
            instances.put(connection, pingManager);
        }
        return pingManager;
    }

    /*
     * Start the executor service if it hasn't been started yet.
     */
    private synchronized static void enableExecutorService() {
        if (periodicPingExecutorService == null) {
            periodicPingExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread pingThread = new Thread(runnable, "Smack Keepalive");
                    pingThread.setDaemon(true);
                    return pingThread;
                }
            });
        }
    }

    /*
     * Stop the executor service if all monitored connections are disconnected.
     */
    private synchronized static void handleDisconnect(Connection con) {
        if (periodicPingExecutorService != null) {
            instances.remove(con);
            
            if (instances.isEmpty()) {
                periodicPingExecutorService.shutdownNow();
                periodicPingExecutorService = null;
            }
        }
    }
    
    private KeepAliveManager(Connection connection) {
        this.connection = connection;
        init();
        handleConnect();
    }

    /*
     * Call after every connection to add the packet listener.
     */
    private void handleConnect() {
        // Listen for all incoming packets and reset the scheduled ping whenever
        // one arrives.
        connection.addPacketListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                // reschedule the ping based on this last server contact
                lastSuccessfulContact = System.currentTimeMillis();
                schedulePingServerTask();
            }
        }, null);
    }

    private void init() {
        connection.addConnectionListener(new ConnectionListener() {

            @Override
            public void connectionClosed() {
                stopPingServerTask();
                handleDisconnect(connection);
            }

            @Override
            public void connectionClosedOnError(Exception arg0) {
                stopPingServerTask();
                handleDisconnect(connection);
            }

            @Override
            public void reconnectionSuccessful() {
                handleConnect();
                schedulePingServerTask();
            }

            @Override
            public void reconnectingIn(int seconds) {
            }

            @Override
            public void reconnectionFailed(Exception e) {
            }
        });

        instances.put(connection, this);
        schedulePingServerTask();
    }

    /**
     * Sets the ping interval.
     * 
     * @param pingInterval
     * The new ping time interval in milliseconds.
     */
    public void setPingInterval(long newPingInterval) {
        if (pingInterval == newPingInterval) 
            return;

        // Enable the executor service
        if (newPingInterval > 0)
            enableExecutorService();
        
        pingInterval = newPingInterval;
            
        if (pingInterval < 0) {
            stopPinging();
        }
        else {
            schedulePingServerTask();
        }
    }

    /**
     * Stops pinging the server.  This cannot stop a ping that has already started, but will prevent another from being triggered.
     * <p>
     * To restart, call {@link #setPingInterval(long)}.
     */
    public void stopPinging() {
        pingInterval = -1;
        stopPingServerTask();
    }
    
    /**
     * Gets the ping interval.
     * 
     * @return The ping interval in milliseconds.
     */
    public long getPingInterval() {
        return pingInterval;
    }

    /**
     * Add listener for notification when a server ping fails.
     * 
     * <p>
     * Please note that this doesn't necessarily mean that the connection is lost, a slow to respond server could also
     * cause a failure due to taking too long to respond and thus causing a reply timeout.
     * 
     * @param listener
     * The listener to be called
     */
    public void addPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.add(listener);
    }

    /**
     * Remove the listener.
     * 
     * @param listener
     * The listener to be removed.
     */
    public void removePingFailedListener(PingFailedListener listener) {
        pingFailedListeners.remove(listener);
    }

    /**
     * Returns the elapsed time (in milliseconds) since the last successful contact with the server 
     * (i.e. the last time any message was received).
     * <p>
     * <b>Note</b>: Result is -1 if no message has been received since manager was created and 
     * 0 if the elapsed time is negative due to a clock reset. 
     * 
     * @return Elapsed time since last message was received.  
     */
    public long getTimeSinceLastContact() {
        if (lastSuccessfulContact == -1)
            return lastSuccessfulContact;
        long delta = System.currentTimeMillis() - lastSuccessfulContact;
        
        return (delta < 0) ? 0 : delta;
    }

    /**
     * Cancels any existing periodic ping task if there is one and schedules a new ping task if pingInterval is greater
     * then zero.
     * 
     * This is designed so only one executor is used for scheduling all pings on all connections.  This results in only 1 thread used for pinging.
     */
    private synchronized void schedulePingServerTask() {
        enableExecutorService();
        stopPingServerTask();
        
        if (pingInterval > 0) {
            periodicPingTask = periodicPingExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    Ping ping = new Ping();
                    PacketFilter responseFilter = new PacketIDFilter(ping.getPacketID());
                    final PacketCollector response = connection.createPacketCollector(responseFilter);
                    connection.sendPacket(ping);
        
                    if (!pingFailedListeners.isEmpty()) {
                        // Schedule a collector for the ping reply, notify listeners if none is received.
                        periodicPingExecutorService.schedule(new Runnable() {
                            @Override
                            public void run() {
                                Packet result = response.nextResult(1);
                
                                // Stop queuing results
                                response.cancel();
                
                                // The actual result of the reply can be ignored since we only care if we actually got one.
                                if (result == null) {
                                    for (PingFailedListener listener : pingFailedListeners) {
                                        listener.pingFailed();
                                    }
                                }
                            }
                        }, SmackConfiguration.getPacketReplyTimeout(), TimeUnit.MILLISECONDS);
                    }
                }
            }, getPingInterval(), TimeUnit.MILLISECONDS);
        }
    }

    private void stopPingServerTask() {
        if (periodicPingTask != null) {
            periodicPingTask.cancel(true);
            periodicPingTask = null;
        }
    }
}
