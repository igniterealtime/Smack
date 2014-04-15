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

import java.util.Random;
import java.util.logging.Logger;
/**
 * Handles the automatic reconnection process. Every time a connection is dropped without
 * the application explicitly closing it, the manager automatically tries to reconnect to
 * the server.<p>
 *
 * The reconnection mechanism will try to reconnect periodically:
 * <ol>
 *  <li>For the first minute it will attempt to connect once every ten seconds.
 *  <li>For the next five minutes it will attempt to connect once a minute.
 *  <li>If that fails it will indefinitely try to connect once every five minutes.
 * </ol>
 *
 * @author Francisco Vives
 */
public class ReconnectionManager extends AbstractConnectionListener {
    private static final Logger LOGGER = Logger.getLogger(ReconnectionManager.class.getName());
    
    // Holds the connection to the server
    private XMPPConnection connection;
    private Thread reconnectionThread;
    private int randomBase = new Random().nextInt(11) + 5; // between 5 and 15 seconds
    
    // Holds the state of the reconnection
    boolean done = false;

    static {
        // Create a new PrivacyListManager on every established connection. In the init()
        // method of PrivacyListManager, we'll add a listener that will delete the
        // instance when the connection is closed.
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                connection.addConnectionListener(new ReconnectionManager(connection));
            }
        });
    }

    private ReconnectionManager(XMPPConnection connection) {
        this.connection = connection;
    }


    /**
     * Returns true if the reconnection mechanism is enabled.
     *
     * @return true if automatic reconnections are allowed.
     */
    private boolean isReconnectionAllowed() {
        return !done && !connection.isConnected()
                && connection.getConfiguration().isReconnectionAllowed();
    }

    /**
     * Starts a reconnection mechanism if it was configured to do that.
     * The algorithm is been executed when the first connection error is detected.
     * <p/>
     * The reconnection mechanism will try to reconnect periodically in this way:
     * <ol>
     * <li>First it will try 6 times every 10 seconds.
     * <li>Then it will try 10 times every 1 minute.
     * <li>Finally it will try indefinitely every 5 minutes.
     * </ol>
     */
    synchronized protected void reconnect() {
        if (this.isReconnectionAllowed()) {
            // Since there is no thread running, creates a new one to attempt
            // the reconnection.
            // avoid to run duplicated reconnectionThread -- fd: 16/09/2010
            if (reconnectionThread!=null && reconnectionThread.isAlive()) return;
            
            reconnectionThread = new Thread() {
             			
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
                    if (attempts > 13) {
                	return randomBase*6*5;      // between 2.5 and 7.5 minutes (~5 minutes)
                    }
                    if (attempts > 7) {
                	return randomBase*6;       // between 30 and 90 seconds (~1 minutes)
                    }
                    return randomBase;       // 10 seconds
                }

                /**
                 * The process will try the reconnection until the connection succeed or the user
                 * cancel it
                 */
                public void run() {
                    // The process will try to reconnect until the connection is established or
                    // the user cancel the reconnection process {@link XMPPConnection#disconnect()}
                    while (ReconnectionManager.this.isReconnectionAllowed()) {
                        // Find how much time we should wait until the next reconnection
                        int remainingSeconds = timeDelay();
                        // Sleep until we're ready for the next reconnection attempt. Notify
                        // listeners once per second about how much time remains before the next
                        // reconnection attempt.
                        while (ReconnectionManager.this.isReconnectionAllowed() &&
                                remainingSeconds > 0)
                        {
                            try {
                                Thread.sleep(1000);
                                remainingSeconds--;
                                ReconnectionManager.this
                                        .notifyAttemptToReconnectIn(remainingSeconds);
                            }
                            catch (InterruptedException e1) {
                                LOGGER.warning("Sleeping thread interrupted");
                                // Notify the reconnection has failed
                                ReconnectionManager.this.notifyReconnectionFailed(e1);
                            }
                        }

                        // Makes a reconnection attempt
                        try {
                            if (ReconnectionManager.this.isReconnectionAllowed()) {
                                connection.connect();
                            }
                        }
                        catch (Exception e) {
                            // Fires the failed reconnection notification
                            ReconnectionManager.this.notifyReconnectionFailed(e);
                        }
                    }
                }
            };
            reconnectionThread.setName("Smack Reconnection Manager");
            reconnectionThread.setDaemon(true);
            reconnectionThread.start();
        }
    }

    /**
     * Fires listeners when a reconnection attempt has failed.
     *
     * @param exception the exception that occured.
     */
    protected void notifyReconnectionFailed(Exception exception) {
        if (isReconnectionAllowed()) {
            for (ConnectionListener listener : connection.connectionListeners) {
                listener.reconnectionFailed(exception);
            }
        }
    }

    /**
     * Fires listeners when The XMPPConnection will retry a reconnection. Expressed in seconds.
     *
     * @param seconds the number of seconds that a reconnection will be attempted in.
     */
    protected void notifyAttemptToReconnectIn(int seconds) {
        if (isReconnectionAllowed()) {
            for (ConnectionListener listener : connection.connectionListeners) {
                listener.reconnectingIn(seconds);
            }
        }
    }

    @Override
    public void connectionClosed() {
        done = true;
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        done = false;
        if (e instanceof StreamErrorException) {
            StreamErrorException xmppEx = (StreamErrorException) e;
            StreamError error = xmppEx.getStreamError();
            String reason = error.getCode();

            if ("conflict".equals(reason)) {
                return;
            }
        }

        if (this.isReconnectionAllowed()) {
            this.reconnect();
        }
    }
}
