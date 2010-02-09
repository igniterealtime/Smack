package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.StreamError;

/**
 * Handles the automatic reconnection process. Every time a connection is dropped without
 * the application explictly closing it, the manager automatically tries to reconnect to
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
public class ReconnectionManager implements ConnectionListener {

    // Holds the connection to the server
    private Connection connection;

    // Holds the state of the reconnection
    boolean done = false;

    static {
        // Create a new PrivacyListManager on every established connection. In the init()
        // method of PrivacyListManager, we'll add a listener that will delete the
        // instance when the connection is closed.
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                connection.addConnectionListener(new ReconnectionManager(connection));
            }
        });
    }

    private ReconnectionManager(Connection connection) {
        this.connection = connection;
    }


    /**
     * Returns true if the reconnection mechanism is enabled.
     *
     * @return true if automatic reconnections are allowed.
     */
    private boolean isReconnectionAllowed() {
        return !done && !connection.isConnected()
                && connection.isReconnectionAllowed();
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
    protected void reconnect() {
        if (this.isReconnectionAllowed()) {
            // Since there is no thread running, creates a new one to attempt
            // the reconnection.
            Thread reconnectionThread = new Thread() {

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
                    if (attempts > 13) {
                        return 60 * 5;      // 5 minutes
                    }
                    if (attempts > 7) {
                        return 60;          // 1 minute
                    }
                    return 10;              // 10 seconds
                }

                /**
                 * The process will try the reconnection until the connection succeed or the user
                 * cancell it
                 */
                public void run() {
                    // The process will try to reconnect until the connection is established or
                    // the user cancel the reconnection process {@link Connection#disconnect()}
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
                                e1.printStackTrace();
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
                        catch (XMPPException e) {
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
     * Fires listeners when The Connection will retry a reconnection. Expressed in seconds.
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

    public void connectionClosed() {
        done = true;
    }

    public void connectionClosedOnError(Exception e) {
        done = false;
        if (e instanceof XMPPException) {
            XMPPException xmppEx = (XMPPException) e;
            StreamError error = xmppEx.getStreamError();

            // Make sure the error is not null
            if (error != null) {
                String reason = error.getCode();

                if ("conflict".equals(reason)) {
                    return;
                }
            }
        }

        if (this.isReconnectionAllowed()) {
            this.reconnect();
        }
    }

    public void reconnectingIn(int seconds) {
        // ignore
    }

    public void reconnectionFailed(Exception e) {
        // ignore
    }

    /**
     * The connection has successfull gotten connected.
     */
    public void reconnectionSuccessful() {
        // ignore
    }

}