package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.StreamError;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the automatic reconnection process. Every time a connection is dropped without
 * the application explicitly closing it, the manager automatically tries to reconnect to
 * the server.<p>
 * <p/>
 * The reconnection mechanism will try to reconnect periodically:
 * <ol>
 * <li>First it will try 6 times every 10 seconds.
 * <li>Then it will try 10 times every 1 minute.
 * <li>Finally it will try indefinitely every 5 minutes.
 * </ol>
 *
 * @author Francisco Vives
 */
public class ReconnectionManager implements ConnectionListener {

    // Holds the time elapsed between each reconnection attempt
    private int secondBetweenReconnection = 5 * 60; // 5 minutes

    // Holds the thread that produces a periodical reconnection.
    private Thread reconnectionThread;

    // Holds the connection to the server
    private XMPPConnection connection;

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
                && connection.getConfiguration().isReconnectionAllowed()
                && connection.packetReader != null;
    }

    /**
     * Returns the time elapsed between each reconnection attempt.
     * By default it will try to reconnect every 5 minutes.
     * It is used when the client has lost the server connection and the XMPPConnection
     * automatically tries to reconnect.
     *
     * @return Returns the number of seconds between reconnection.
     */
    private int getSecondBetweenReconnection() {
        return secondBetweenReconnection;
    }

    /**
     * Sets the time elapsed between each reconnection attempt.
     * It is used when the client has lost the server connection and the XMPPConnection
     * automatically tries to reconnect.
     *
     * @param secondBetweenReconnection The number of seconds between reconnection.
     */
    protected void setSecondBetweenReconnection(
            int secondBetweenReconnection) {
        this.secondBetweenReconnection = secondBetweenReconnection;
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
            reconnectionThread = new Thread() {
                /**
                 * Holds the number of reconnection attempts
                 */
                private int attempts = 0;
                private int firstReconnectionPeriod = 7; // 6 attempts
                private int secondReconnectionPeriod = 10 + firstReconnectionPeriod; // 16 attempts
                private int firstReconnectionTime = 10; // 10 seconds
                private int secondReconnectionTime = 60; // 1 minute
                private int lastReconnectionTime =
                        getSecondBetweenReconnection(); // user defined in seconds
                private int remainingSeconds = 0; // The seconds remaining to a reconnection
                private int notificationPeriod = 1000; // 1 second

                /**
                 * Returns the amount of time until the next reconnection attempt.
                 *
                 * @return the amount of time until the next reconnection attempt.
                 */
                private int timeDelay() {
                    if (attempts > secondReconnectionPeriod) {
                        return lastReconnectionTime; // 5 minutes
                    }
                    if (attempts > firstReconnectionPeriod) {
                        return secondReconnectionTime; // 1 minute
                    }
                    return firstReconnectionTime; // 10 seconds
                }

                /**
                 * The process will try the reconnection until the connection succeed or the user
                 * cancell it
                 */
                public void run() {
                    // The process will try to reconnect until the connection is established or
                    // the user cancel the reconnection process {@link XMPPConnection#disconnect()}
                    while (ReconnectionManager.this.isReconnectionAllowed()) {
                        // Indicate how much time will wait until next reconnection
                        remainingSeconds = timeDelay();
                        // Notifies the remaining time until the next reconnection attempt
                        // every 1 second.
                        while (ReconnectionManager.this.isReconnectionAllowed() &&
                                remainingSeconds > 0) {
                            try {
                                Thread.sleep(notificationPeriod);
                                remainingSeconds = remainingSeconds - 1;
                                ReconnectionManager.this
                                        .notifyAttemptToReconnectIn(remainingSeconds);
                            }
                            catch (InterruptedException e1) {
                                e1.printStackTrace();
                                // Notify the reconnection has failed
                                ReconnectionManager.this.notifyReconnectionFailed(e1);
                            }
                        }
                        // Waiting time have finished

                        // Makes the reconnection attempt
                        try {
                            if (ReconnectionManager.this.isReconnectionAllowed()) {
                                // Attempts to reconnect.
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
        List<ConnectionListener> listenersCopy;
        if (isReconnectionAllowed()) {
            for (ConnectionListener listener : connection.packetReader.connectionListeners) {
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
            for (ConnectionListener listener : connection.packetReader.connectionListeners) {
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
