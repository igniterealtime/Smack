/**
 *
 * Copyright 2012-2018 Florian Schmaus
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractConnectionClosedListener;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.ScheduledAction;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.SmackFuture.InternalProcessStanzaSmackFuture;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.ExceptionCallback;
import org.jivesoftware.smack.util.SuccessCallback;

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

    private static final Map<XMPPConnection, PingManager> INSTANCES = new WeakHashMap<>();

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
     * @param connection TODO javadoc me please
     * The connection the manager is attached to.
     * @return The new or existing manager.
     */
    public static synchronized PingManager getInstanceFor(XMPPConnection connection) {
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

    /**
     * The interval in seconds between pings are send to the users server.
     */
    private int pingInterval = defaultPingInterval;

    private ScheduledAction nextAutomaticPing;

    private PingManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(Ping.NAMESPACE);

        connection.registerIQRequestHandler(new AbstractIqRequestHandler(Ping.ELEMENT, Ping.NAMESPACE, IQ.Type.get, Mode.async) {
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

    private boolean isValidErrorPong(Jid destinationJid, XMPPErrorException xmppErrorException) {
        // If it is an error error response and the destination was our own service, then this must mean that the
        // service responded, i.e. is up and pingable.
        if (destinationJid.equals(connection().getXMPPServiceDomain())) {
            return true;
        }

        final StanzaError xmppError = xmppErrorException.getStanzaError();

        // We may received an error response from an intermediate service returning an error like
        // 'remote-server-not-found' or 'remote-server-timeout' to us (which would fake the 'from' address,
        // see RFC 6120 § 8.3.1 2.). Or the recipient could became unavailable.

        // Sticking with the current rules of RFC 6120/6121, it is undecidable at this point whether we received an
        // error response from the pinged entity or not. This is because a service-unavailable error condition is
        // *required* (as per the RFCs) to be send back in both relevant cases:
        // 1. When the receiving entity is unaware of the IQ request type. RFC 6120 § 8.4.:
        //    "If an intended recipient receives an IQ stanza of type "get" or
        //    "set" containing a child element qualified by a namespace it does
        //    not understand, then the entity MUST return an IQ stanza of type
        //    "error" with an error condition of <service-unavailable/>.
        //  2. When the receiving resource is not available. RFC 6121 § 8.5.3.2.3.

        // Some clients don't obey the first rule and instead send back a feature-not-implement condition with type 'cancel',
        // which allows us to consider this response as valid "error response" pong.
        StanzaError.Type type = xmppError.getType();
        StanzaError.Condition condition = xmppError.getCondition();
        return type == StanzaError.Type.CANCEL && condition == StanzaError.Condition.feature_not_implemented;
    }

    public SmackFuture<Boolean, Exception> pingAsync(Jid jid) {
        return pingAsync(jid, connection().getReplyTimeout());
    }

    public SmackFuture<Boolean, Exception> pingAsync(final Jid jid, long pongTimeout) {
        final InternalProcessStanzaSmackFuture<Boolean, Exception> future = new InternalProcessStanzaSmackFuture<Boolean, Exception>() {
            @Override
            public void handleStanza(Stanza packet) {
                setResult(true);
            }
            @Override
            public boolean isNonFatalException(Exception exception) {
                if (exception instanceof XMPPErrorException) {
                    XMPPErrorException xmppErrorException = (XMPPErrorException) exception;
                    if (isValidErrorPong(jid, xmppErrorException)) {
                        setResult(true);
                        return true;
                    }
                }
                return false;
            }
        };

        XMPPConnection connection = connection();
        Ping ping = new Ping(connection, jid);
        connection.sendIqRequestAsync(ping, pongTimeout)
        .onSuccess(new SuccessCallback<IQ>() {
            @Override
            public void onSuccess(IQ result) {
                future.processStanza(result);
            }
        })
        .onError(new ExceptionCallback<Exception>() {
            @Override
            public void processException(Exception exception) {
                future.processException(exception);
            }
        });

        return future;
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public boolean ping(Jid jid, long pingTimeout) throws NotConnectedException, NoResponseException, InterruptedException {
        final XMPPConnection connection = connection();
        // Packet collector for IQs needs an connection that was at least authenticated once,
        // otherwise the client JID will be null causing an NPE
        if (!connection.isAuthenticated()) {
            throw new NotConnectedException();
        }
        Ping ping = new Ping(connection, jid);
        try {
            connection.createStanzaCollectorAndSend(ping).nextResultOrThrow(pingTimeout);
        }
        catch (XMPPErrorException e) {
            return isValidErrorPong(jid, e);
        }
        return true;
    }

    /**
     * Same as calling {@link #ping(Jid, long)} with the defaultpacket reply
     * timeout.
     *
     * @param jid The id of the entity the ping is being sent to
     * @return true if a reply was received from the entity, false otherwise.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws NoResponseException if there was no response from the jid.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
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
            nextAutomaticPing = schedule(this::pingServerIfNecessary, nextPingIn, TimeUnit.SECONDS);
        }
    }

    private void maybeStopPingServerTask() {
        final ScheduledAction nextAutomaticPing = this.nextAutomaticPing;
        if (nextAutomaticPing != null) {
            nextAutomaticPing.cancel();
            this.nextAutomaticPing = null;
        }
    }

    /**
     * Ping the server if deemed necessary because automatic server pings are
     * enabled ({@link #setPingInterval(int)}) and the ping interval has expired.
     */
    public void pingServerIfNecessary() {
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
        if (!connection.isAuthenticated()) {
            LOGGER.warning(connection + " was not authenticated");
            return;
        }

        final long minimumTimeout = TimeUnit.MINUTES.toMillis(2);
        final long connectionReplyTimeout = connection.getReplyTimeout();
        final long timeout = connectionReplyTimeout > minimumTimeout ? connectionReplyTimeout : minimumTimeout;

        SmackFuture<Boolean, Exception> pingFuture = pingAsync(connection.getXMPPServiceDomain(), timeout);
        pingFuture.onSuccess(new SuccessCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Ping was successful, wind-up the periodic task again
                maybeSchedulePingServerTask();
            }
        });
        pingFuture.onError(new ExceptionCallback<Exception>() {
            @Override
            public void processException(Exception exception) {
                long lastStanzaReceived = connection.getLastStanzaReceived();
                if (lastStanzaReceived > 0) {
                    long now = System.currentTimeMillis();
                    // Delta since the last stanza was received
                    int deltaInSeconds = (int)  ((now - lastStanzaReceived) / 1000);
                    // If the delta is smaller then the ping interval, we have got an valid stanza in time
                    // So not error notification needed
                    if (deltaInSeconds < pingInterval) {
                        maybeSchedulePingServerTask(deltaInSeconds);
                        return;
                    }
                }

                for (PingFailedListener l : pingFailedListeners) {
                    l.pingFailed();
                }
            }
        });
    }
}
