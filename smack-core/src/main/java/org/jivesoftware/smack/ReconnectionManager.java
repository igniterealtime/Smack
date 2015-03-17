/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack;

import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.util.Async;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the automatic reconnection process. Every time a connection is dropped without
 * the application explicitly closing it, the manager automatically tries to reconnect to
 * the server.<p>
 *
 * There are two possible reconnection policies:
 *
 * {@link ReconnectionPolicy#RANDOM_INCREASING_DELAY} - The reconnection mechanism will try to reconnect periodically:
 * <ol>
 *  <li>For the first minute it will attempt to connect once every ten seconds.
 *  <li>For the next five minutes it will attempt to connect once a minute.
 *  <li>If that fails it will indefinitely try to connect once every five minutes.
 * </ol>
 *
 * {@link ReconnectionPolicy#FIXED_DELAY} - The reconnection mechanism will try to reconnect after a fixed delay 
 * independently from the number of reconnection attempts already performed
 *
 * @author Francisco Vives
 * @author Luca Stucchi
 */
public class ReconnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ReconnectionManager.class.getName());

    private static final Map<AbstractXMPPConnection, ReconnectionManager> INSTANCES = new WeakHashMap<AbstractXMPPConnection, ReconnectionManager>();

    /**
     * Get a instance of ReconnectionManager for the given connection.
     * 
     * @param connection
     * @return a ReconnectionManager for the connection.
     */
    public static synchronized ReconnectionManager getInstanceFor(AbstractXMPPConnection connection) {
        ReconnectionManager reconnectionManager = INSTANCES.get(connection);
        if (reconnectionManager == null) {
            reconnectionManager = new ReconnectionManager(connection);
            INSTANCES.put(connection, reconnectionManager);
        }
        return reconnectionManager;
    }

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                if (connection instanceof AbstractXMPPConnection) {
                    ReconnectionManager.getInstanceFor((AbstractXMPPConnection) connection);
                }
            }
        });
    }

    private static boolean enabledPerDefault = false;

    /**
     * Set if the automatic reconnection mechanism will be enabled per default for new XMPP connections. The default is
     * 'false'.
     * 
     * @param enabled
     */
    public static void setEnabledPerDefault(boolean enabled) {
        enabledPerDefault = enabled;
    }

    /**
     * Get the current default reconnection mechanism setting for new XMPP connections.
     *
     * @return true if new connection will come with an enabled reconnection mechanism
     */
    public static boolean getEnabledPerDefault() {
        return enabledPerDefault;
    }

    // Holds the connection to the server
    private final WeakReference<AbstractXMPPConnection> weakRefConnection;
    private final int randomBase = new Random().nextInt(13) + 2; // between 2 and 15 seconds
    private final Runnable reconnectionRunnable;

    private static int defaultFixedDelay = 15;
    private static ReconnectionPolicy defaultReconnectionPolicy = ReconnectionPolicy.RANDOM_INCREASING_DELAY;

    private volatile int fixedDelay = defaultFixedDelay;
    private volatile ReconnectionPolicy reconnectionPolicy = defaultReconnectionPolicy;

    /**
     * Set the default fixed delay in seconds between the reconnection attempts. Also set the
     * default connection policy to {@link ReconnectionPolicy#FIXED_DELAY}
     * 
     * @param fixedDelay Delay expressed in seconds
     */
    public static void setDefaultFixedDelay(int fixedDelay) {
        defaultFixedDelay = fixedDelay;
        setDefaultReconnectionPolicy(ReconnectionPolicy.FIXED_DELAY);
    }

    /**
     * Set the default Reconnection Policy to use
     * 
     * @param reconnectionPolicy
     */
    public static void setDefaultReconnectionPolicy(ReconnectionPolicy reconnectionPolicy) {
        defaultReconnectionPolicy = reconnectionPolicy;
    }

    /**
     * Set the fixed delay in seconds between the reconnection attempts Also set the connection
     * policy to {@link ReconnectionPolicy#FIXED_DELAY}
     * 
     * @param fixedDelay Delay expressed in seconds
     */
    public void setFixedDelay(int fixedDelay) {
        this.fixedDelay = fixedDelay;
        setReconnectionPolicy(ReconnectionPolicy.FIXED_DELAY);
    }

    /**
     * Set the Reconnection Policy to use
     * 
     * @param reconnectionPolicy
     */
    public void setReconnectionPolicy(ReconnectionPolicy reconnectionPolicy) {
        this.reconnectionPolicy = reconnectionPolicy;
    }

    /**
     * Flag that indicates if a reconnection should be attempted when abruptly disconnected
     */
    private boolean automaticReconnectEnabled = false;

    boolean done = false;

    private Thread reconnectionThread;

    private ReconnectionManager(AbstractXMPPConnection connection) {
        weakRefConnection = new WeakReference<AbstractXMPPConnection>(connection);

        reconnectionRunnable = new Thread() {

            /**
             * Holds the current number of reconnection attempts
             */
            private int attempts = 0;

            /**
             * Returns the number of seconds until the next reconnection attempt.
             *
             * @return the number of seconds until the next reconnection attempt.
             */
            private int timeDelay() {
                attempts++;

                // Delay variable to be assigned
                int delay;
                switch (reconnectionPolicy) {
                case FIXED_DELAY:
                    delay = fixedDelay;
                    break;
                case RANDOM_INCREASING_DELAY:
                    if (attempts > 13) {
                        delay = randomBase * 6 * 5; // between 2.5 and 7.5 minutes (~5 minutes)
                    }
                    if (attempts > 7) {
                        delay = randomBase * 6; // between 30 and 90 seconds (~1 minutes)
                    }
                    delay = randomBase; // 10 seconds
                    break;
                default:
                    throw new AssertionError("Unknown reconnection policy " + reconnectionPolicy);
                }

                return delay;
            }

            /**
             * The process will try the reconnection until the connection succeed or the user cancel it
             */
            public void run() {
                final AbstractXMPPConnection connection = weakRefConnection.get();
                if (connection == null) {
                    return;
                }
                // The process will try to reconnect until the connection is established or
                // the user cancel the reconnection process AbstractXMPPConnection.disconnect().
                while (isReconnectionPossible(connection)) {
                    // Find how much time we should wait until the next reconnection
                    int remainingSeconds = timeDelay();
                    // Sleep until we're ready for the next reconnection attempt. Notify
                    // listeners once per second about how much time remains before the next
                    // reconnection attempt.
                    while (isReconnectionPossible(connection) && remainingSeconds > 0) {
                        try {
                            Thread.sleep(1000);
                            remainingSeconds--;
                            for (ConnectionListener listener : connection.connectionListeners) {
                                listener.reconnectingIn(remainingSeconds);
                            }
                        }
                        catch (InterruptedException e) {
                            LOGGER.log(Level.FINE, "waiting for reconnection interrupted", e);
                            break;
                        }
                    }

                    for (ConnectionListener listener : connection.connectionListeners) {
                        listener.reconnectingIn(0);
                    }

                    // Makes a reconnection attempt
                    try {
                        if (isReconnectionPossible(connection)) {
                            connection.connect();
                        }
                    }
                    catch (Exception e) {
                        // Fires the failed reconnection notification
                        for (ConnectionListener listener : connection.connectionListeners) {
                            listener.reconnectionFailed(e);
                        }
                    }
                }
            }
        };

        // If the reconnection mechanism is enable per default, enable it for this ReconnectionManager instance
        if (getEnabledPerDefault()) {
            enableAutomaticReconnection();
        }
    }

    /**
     * Enable the automatic reconnection mechanism. Does nothing if already enabled.
     */
    public synchronized void enableAutomaticReconnection() {
        if (automaticReconnectEnabled) {
            return;
        }
        XMPPConnection connection = weakRefConnection.get();
        if (connection == null) {
            throw new IllegalStateException("Connection instance no longer available");
        }
        connection.addConnectionListener(connectionListener);
        automaticReconnectEnabled = true;
    }

    /**
     * Disable the automatic reconnection mechanism. Does nothing if already disabled.
     */
    public synchronized void disableAutomaticReconnection() {
        if (!automaticReconnectEnabled) {
            return;
        }
        XMPPConnection connection = weakRefConnection.get();
        if (connection == null) {
            throw new IllegalStateException("Connection instance no longer available");
        }
        connection.removeConnectionListener(connectionListener);
        automaticReconnectEnabled = false;
    }

    /**
     * Returns if the automatic reconnection mechanism is enabled. You can disable the reconnection mechanism with
     * {@link #disableAutomaticReconnection} and enable the mechanism with {@link #enableAutomaticReconnection()}.
     *
     * @return true, if the reconnection mechanism is enabled.
     */
    public boolean isAutomaticReconnectEnabled() {
        return automaticReconnectEnabled;
    }

    /**
     * Returns true if the reconnection mechanism is enabled.
     *
     * @return true if automatic reconnection is allowed.
     */
    private boolean isReconnectionPossible(XMPPConnection connection) {
        return !done && !connection.isConnected()
                && isAutomaticReconnectEnabled();
    }

    /**
     * Starts a reconnection mechanism if it was configured to do that.
     * The algorithm is been executed when the first connection error is detected.
     */
    private synchronized void reconnect() {
        XMPPConnection connection = this.weakRefConnection.get();
        if (connection == null) {
            LOGGER.fine("Connection is null, will not reconnect");
            return;
        }
        // Since there is no thread running, creates a new one to attempt
        // the reconnection.
        // avoid to run duplicated reconnectionThread -- fd: 16/09/2010
        if (reconnectionThread != null && reconnectionThread.isAlive())
            return;

        reconnectionThread = Async.go(reconnectionRunnable,
                        "Smack Reconnection Manager (" + connection.getConnectionCounter() + ')');
    }

    private final ConnectionListener connectionListener = new AbstractConnectionListener() {

        @Override
        public void connectionClosed() {
            done = true;
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            done = false;
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            done = false;
            if (!isAutomaticReconnectEnabled()) {
                return;
            }
            if (e instanceof StreamErrorException) {
                StreamErrorException xmppEx = (StreamErrorException) e;
                StreamError error = xmppEx.getStreamError();

                if (StreamError.Condition.conflict == error.getCondition()) {
                    return;
                }
            }

            reconnect();
        }
    };

    /**
     * Reconnection Policy, where {@link ReconnectionPolicy#RANDOM_INCREASING_DELAY} is the default policy used by smack and {@link ReconnectionPolicy#FIXED_DELAY} implies
     * a fixed amount of time between reconnection attempts
     */
    public enum ReconnectionPolicy {
        /**
         * Default policy classically used by smack, having an increasing delay related to the
         * overall number of attempts
         */
        RANDOM_INCREASING_DELAY,

        /**
         * Policy using fixed amount of time between reconnection attempts
         */
        FIXED_DELAY,
        ;
    }
}
