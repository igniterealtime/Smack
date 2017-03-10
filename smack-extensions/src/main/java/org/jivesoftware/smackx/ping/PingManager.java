/**
 *
 * Copyright 2012-2017 Florian Schmaus
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

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractConnectionClosedListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.util.SmackExecutorThreadFactory;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jxmpp.jid.Jid;

/**
 * Implements the XMPP Ping as defined by XEP-0199. The XMPP Ping protocol allows one entity to
 * ping any other entity by simply sending a ping to the appropriate JID. PingManger also
 * periodically sends XMPP pings to the server to avoid NAT timeouts and to test
 * the connection status.
 * <p>
 * The default server ping interval is 30 minutes and can be modified with
 * {@link #setDefaultPingInterval(int)} and {@link #setPingInterval(int)}.
 * </p>
 * 
 * @author Florian Schmaus
 * @see <a href="http://www.xmpp.org/extensions/xep-0199.html">XEP-0199:XMPP Ping</a>
 */
public final class PingManager extends Manager {
    private static final Logger LOGGER = Logger.getLogger(PingManager.class.getName());

    private static final Map<XMPPConnection, PingManager> INSTANCES = new WeakHashMap<XMPPConnection, PingManager>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Retrieves a {@link PingManager} for the specified {@link XMPPConnection}, creating one if it doesn't already
     * exist.
     * 
     * @param connection
     * The connection the manager is attached to.
     * @return The new or existing manager.
     */
    public synchronized static PingManager getInstanceFor(XMPPConnection connection) {
        PingManager pingManager = INSTANCES.get(connection);
        if (pingManager == null) {
            pingManager = new PingManager(connection);
            INSTANCES.put(connection, pingManager);
        }
        return pingManager;
    }

    /**
     * The default ping interval in seconds used by new PingManager instances. The default is 30 minutes.
     */
    private static int defaultPingInterval = 60 * 30;

    /**
     * Set the default ping interval which will be used for new connections.
     *
     * @param interval the interval in seconds
     */
    public static void setDefaultPingInterval(int interval) {
        defaultPingInterval = interval;
    }

    private final Set<PingFailedListener> pingFailedListeners = new CopyOnWriteArraySet<>();

    private final ScheduledExecutorService executorService;

    /**
     * The interval in seconds between pings are send to the users server.
     */
    private int pingInterval = defaultPingInterval;

    private ScheduledFuture<?> nextAutomaticPing;

    private PingManager(XMPPConnection connection) {
        super(connection);
        executorService = Executors.newSingleThreadScheduledExecutor(
                        new SmackExecutorThreadFactory(connection, "Ping"));
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(Ping.NAMESPACE);

        connection.registerIQRequestHandler(new AbstractIqRequestHandler(Ping.ELEMENT, Ping.NAMESPACE, Type.get, Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                Ping ping = (Ping) iqRequest;
                return ping.getPong();
            }
        });
        connection.addConnectionListener(new AbstractConnectionClosedListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                maybeSchedulePingServerTask();
            }
            @Override
            public void connectionTerminated() {
                maybeStopPingServerTask();
            }
        });
        maybeSchedulePingServerTask();
    }

    /**
     * Pings the given jid. This method will return false if an error occurs.  The exception 
     * to this, is a server ping, which will always return true if the server is reachable, 
     * event if there is an error on the ping itself (i.e. ping not supported).
     * <p>
     * Use {@link #isPingSupported(Jid)} to determine if XMPP Ping is supported 
     * by the entity.
     * 
     * @param jid The id of the entity the ping is being sent to
     * @param pingTimeout The time to wait for a reply in milliseconds
     * @return true if a reply was received from the entity, false otherwise.
     * @throws NoResponseException if there was no response from the jid.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean ping(Jid jid, long pingTimeout) throws NotConnectedException, NoResponseException, InterruptedException {
        final XMPPConnection connection = connection();
        // Packet collector for IQs needs an connection that was at least authenticated once,
        // otherwise the client JID will be null causing an NPE
        if (!connection.isAuthenticated()) {
            throw new NotConnectedException();
        }
        Ping ping = new Ping(jid);
        try {
            connection.createStanzaCollectorAndSend(ping).nextResultOrThrow(pingTimeout);
        }
        catch (XMPPException exc) {
            return jid.equals(connection.getXMPPServiceDomain());
        }
        return true;
    }

    /**
     * Same as calling {@link #ping(Jid, long)} with the defaultpacket reply 
     * timeout.
     * 
     * @param jid The id of the entity the ping is being sent to
     * @return true if a reply was received from the entity, false otherwise.
     * @throws NotConnectedException
     * @throws NoResponseException if there was no response from the jid.
     * @throws InterruptedException 
     */
    public boolean ping(Jid jid) throws NotConnectedException, NoResponseException, InterruptedException {
        return ping(jid, connection().getReplyTimeout());
    }

    /**
     * Query the specified entity to see if it supports the Ping protocol (XEP-0199).
     * 
     * @param jid The id of the entity the query is being sent to
     * @return true if it supports ping, false otherwise.
     * @throws XMPPErrorException An XMPP related error occurred during the request 
     * @throws NoResponseException if there was no response from the jid.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean isPingSupported(Jid jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, Ping.NAMESPACE);
    }

    /**
     * Pings the server. This method will return true if the server is reachable.  It
     * is the equivalent of calling <code>ping</code> with the XMPP domain.
     * <p>
     * Unlike the {@link #ping(Jid)} case, this method will return true even if 
     * {@link #isPingSupported(Jid)} is false.
     * 
     * @return true if a reply was received from the server, false otherwise.
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public boolean pingMyServer() throws NotConnectedException, InterruptedException {
        return pingMyServer(true);
    }

    /**
     * Pings the server. This method will return true if the server is reachable.  It
     * is the equivalent of calling <code>ping</code> with the XMPP domain.
     * <p>
     * Unlike the {@link #ping(Jid)} case, this method will return true even if
     * {@link #isPingSupported(Jid)} is false.
     *
     * @param notifyListeners Notify the PingFailedListener in case of error if true
     * @return true if the user's server could be pinged.
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public boolean pingMyServer(boolean notifyListeners) throws NotConnectedException, InterruptedException {
        return pingMyServer(notifyListeners, connection().getReplyTimeout());
    }

    /**
     * Pings the server. This method will return true if the server is reachable.  It
     * is the equivalent of calling <code>ping</code> with the XMPP domain.
     * <p>
     * Unlike the {@link #ping(Jid)} case, this method will return true even if
     * {@link #isPingSupported(Jid)} is false.
     *
     * @param notifyListeners Notify the PingFailedListener in case of error if true
     * @param pingTimeout The time to wait for a reply in milliseconds
     * @return true if the user's server could be pinged.
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public boolean pingMyServer(boolean notifyListeners, long pingTimeout) throws NotConnectedException, InterruptedException {
        boolean res;
        try {
            res = ping(connection().getXMPPServiceDomain(), pingTimeout);
        }
        catch (NoResponseException e) {
            res = false;
        }
        if (!res && notifyListeners) {
            for (PingFailedListener l : pingFailedListeners)
                l.pingFailed();
        }
        return res;
    }

    /**
     * Set the interval in seconds between a automated server ping is send. A negative value disables automatic server
     * pings. All settings take effect immediately. If there is an active scheduled server ping it will be canceled and,
     * if <code>pingInterval</code> is positive, a new one will be scheduled in pingInterval seconds.
     * <p>
     * If the ping fails after 3 attempts waiting the connections reply timeout for an answer, then the ping failed
     * listeners will be invoked.
     * </p>
     *
     * @param pingInterval the interval in seconds between the automated server pings
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
     * Register a new PingFailedListener.
     *
     * @param listener the listener to invoke
     */
    public void registerPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.add(listener);
    }

    /**
     * Unregister a PingFailedListener.
     *
     * @param listener the listener to remove
     */
    public void unregisterPingFailedListener(PingFailedListener listener) {
        pingFailedListeners.remove(listener);
    }

    private void maybeSchedulePingServerTask() {
        maybeSchedulePingServerTask(0);
    }

    /**
     * Cancels any existing periodic ping task if there is one and schedules a new ping task if
     * pingInterval is greater then zero.
     *
     * @param delta the delta to the last received stanza in seconds
     */
    private synchronized void maybeSchedulePingServerTask(int delta) {
        maybeStopPingServerTask();
        if (pingInterval > 0) {
            int nextPingIn = pingInterval - delta;
            LOGGER.fine("Scheduling ServerPingTask in " + nextPingIn + " seconds (pingInterval="
                            + pingInterval + ", delta=" + delta + ")");
            nextAutomaticPing = executorService.schedule(pingServerRunnable, nextPingIn, TimeUnit.SECONDS);
        }
    }

    private void maybeStopPingServerTask() {
        if (nextAutomaticPing != null) {
            nextAutomaticPing.cancel(true);
            nextAutomaticPing = null;
        }
    }

    /**
     * Ping the server if deemed necessary because automatic server pings are
     * enabled ({@link #setPingInterval(int)}) and the ping interval has expired.
     */
    public synchronized void pingServerIfNecessary() {
        final int DELTA = 1000; // 1 seconds
        final int TRIES = 3; // 3 tries
        final XMPPConnection connection = connection();
        if (connection == null) {
            // connection has been collected by GC
            // which means we can stop the thread by breaking the loop
            return;
        }
        if (pingInterval <= 0) {
            // Ping has been disabled
            return;
        }
        long lastStanzaReceived = connection.getLastStanzaReceived();
        if (lastStanzaReceived > 0) {
            long now = System.currentTimeMillis();
            // Delta since the last stanza was received
            int deltaInSeconds = (int)  ((now - lastStanzaReceived) / 1000);
            // If the delta is small then the ping interval, then we can defer the ping
            if (deltaInSeconds < pingInterval) {
                maybeSchedulePingServerTask(deltaInSeconds);
                return;
            }
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
                try {
                    res = pingMyServer(false);
                }
                catch (InterruptedException | SmackException e) {
                    // Note that we log the connection here, so that it is not GC'ed between the call to isAuthenticated
                    // a few lines above and the usage of the connection within pingMyServer(). In order to prevent:
                    // https://community.igniterealtime.org/thread/59369
                    LOGGER.log(Level.WARNING, "Exception while pinging server of " + connection, e);
                    res = false;
                }
                // stop when we receive a pong back
                if (res) {
                    break;
                }
            }
            if (!res) {
                for (PingFailedListener l : pingFailedListeners) {
                    l.pingFailed();
                }
            } else {
                // Ping was successful, wind-up the periodic task again
                maybeSchedulePingServerTask();
            }
        } else {
            LOGGER.warning("XMPPConnection was not authenticated");
        }
    }

    private final Runnable pingServerRunnable = new Runnable() {
        @Override
        public void run() {
            LOGGER.fine("ServerPingTask run()");
            pingServerIfNecessary();
        }
    };

    @Override
    protected void finalize() throws Throwable {
        LOGGER.fine("finalizing PingManager: Shutting down executor service");
        try {
            executorService.shutdown();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "finalize() threw throwable", t);
        }
        finally {
            super.finalize();
        }
    }
}
