/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smack;

import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Tests the connection and reconnection mechanism
 *
 * @author Francisco Vives
 */

public class ReconnectionTest extends SmackTestCase {

    public ReconnectionTest(String arg0) {
        super(arg0);
    }

    /**
     * Tests an automatic reconnection.
     * Simulates a connection error and then waits until gets reconnected.
     */

    public void testAutomaticReconnection() throws Exception {
        XMPPConnection connection = getConnection(0);
        XMPPConnectionTestListener listener = new XMPPConnectionTestListener();
        connection.addConnectionListener(listener);

        // Simulates an error in the connection
        connection.packetReader.notifyConnectionError(new Exception("Simulated Error"));
        Thread.sleep(12000);
        // After 10 seconds, the reconnection manager must reestablishes the connection
        assertEquals("The ConnectionListener.connectionStablished() notification was not fired",
                true, listener.reconnected);
        assertEquals("The ConnectionListener.reconnectingIn() notification was not fired", 10,
                listener.attemptsNotifications);
        assertEquals("The ReconnectionManager algorithm has reconnected without waiting until 0", 0,
                listener.remainingSeconds);

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);
    }

    public void testAutomaticReconnectionWithCompression() throws Exception {
        // Create the configuration for this new connection
        ConnectionConfiguration config = new ConnectionConfiguration(getHost(), getPort());
        config.setCompressionEnabled(true);
        config.setSASLAuthenticationEnabled(true);

        XMPPConnection connection = new XMPPConnection(config);
        // Connect to the server
        connection.connect();
        // Log into the server
        connection.login(getUsername(0), getPassword(0), "MyOtherResource");

        assertTrue("Failed to use compression", connection.isUsingCompression());

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);

        XMPPConnectionTestListener listener = new XMPPConnectionTestListener();
        connection.addConnectionListener(listener);

        // Simulates an error in the connection
        connection.packetReader.notifyConnectionError(new Exception("Simulated Error"));
        Thread.sleep(12000);
        // After 10 seconds, the reconnection manager must reestablishes the connection
        assertEquals("The ConnectionListener.connectionStablished() notification was not fired",
                true, listener.reconnected);
        assertEquals("The ConnectionListener.reconnectingIn() notification was not fired", 10,
                listener.attemptsNotifications);
        assertEquals("The ReconnectionManager algorithm has reconnected without waiting until 0", 0,
                listener.remainingSeconds);

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);
    }

    /**
     * Tests a manual reconnection.
     * Simulates a connection error, disables the reconnection mechanism and then reconnects.
     */
    public void testManualReconnectionWithCancelation() throws Exception {
        XMPPConnection connection = getConnection(0);
        XMPPConnectionTestListener listener = new XMPPConnectionTestListener();
        connection.addConnectionListener(listener);

        // Produces a connection error
        connection.packetReader.notifyConnectionError(new Exception("Simulated Error"));
        assertEquals(
                "An error occurs but the ConnectionListener.connectionClosedOnError(e) was not notified",
                true, listener.connectionClosedOnError);
        Thread.sleep(1000);
        // Cancels the automatic reconnection
        connection.getConfiguration().setReconnectionAllowed(false);
        // Waits for a reconnection that must not happened.
        Thread.sleep(10500);
        // Cancels the automatic reconnection
        assertEquals("The connection was stablished but it was not allowed to", false,
                listener.reconnected);

        // Makes a manual reconnection from an error terminated connection without reconnection
        connection.connect();

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);
    }

    /**
     * Tests a manual reconnection after a login.
     * Closes the connection and then reconnects.
     */
    public void testCloseAndManualReconnection() throws Exception {
        XMPPConnection connection = getConnection(0);
        String username = connection.getConfiguration().getUsername();
        String password = connection.getConfiguration().getPassword();
        XMPPConnectionTestListener listener = new XMPPConnectionTestListener();
        connection.addConnectionListener(listener);

        // Produces a normal disconnection
        connection.disconnect();
        assertEquals("ConnectionListener.connectionClosed() was not notified",
                true, listener.connectionClosed);
        // Waits 10 seconds waiting for a reconnection that must not happened.
        Thread.sleep(12200);
        assertEquals("The connection was stablished but it was not allowed to", false,
                listener.reconnected);

        // Makes a manual reconnection
        connection.connect();
        connection.login(username, password);

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);
    }

    /**
     * Tests a reconnection in a anonymously logged connection.
     * Closes the connection and then reconnects.
     */
    public void testAnonymousReconnection() throws Exception {
        XMPPConnection connection = createConnection();
        connection.connect();
        XMPPConnectionTestListener listener = new XMPPConnectionTestListener();
        connection.addConnectionListener(listener);

        // Makes the anounymous login
        connection.loginAnonymously();

        // Produces a normal disconnection
        connection.disconnect();
        assertEquals("ConnectionListener.connectionClosed() was not notified",
                true, listener.connectionClosed);
        // Makes a manual reconnection
        connection.connect();
        connection.loginAnonymously();
        assertEquals("Failed the manual connection", true, connection.isAnonymous());
    }

    private XMPPConnection createXMPPConnection() throws Exception {
        XMPPConnection connection;
        // Create the configuration
        ConnectionConfiguration config = new ConnectionConfiguration(getHost(), getPort());
        config.setCompressionEnabled(Boolean.getBoolean("test.compressionEnabled"));
        config.setSASLAuthenticationEnabled(true);
        connection = new XMPPConnection(config);

        return connection;
    }

    /**
     * Execute some server interaction in order to test that the regenerated connection works fine.
     */
    private void executeSomeServerInteraction(XMPPConnection connection) throws XMPPException {
        PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(connection);
        privacyManager.getPrivacyLists();
    }

    protected int getMaxConnections() {
        return 1;
    }

    private class XMPPConnectionTestListener implements ConnectionListener {

        // Variables to support listener notifications verification
        private boolean connectionClosed = false;
        private boolean connectionClosedOnError = false;
        private boolean reconnected = false;
        private boolean reconnectionFailed = false;
        private int remainingSeconds = 0;
        private int attemptsNotifications = 0;
        private boolean reconnectionCanceled = false;

        /**
         * Methods to test the listener.
         */
        public void connectionClosed() {
            connectionClosed = true;
        }

        public void connectionClosedOnError(Exception e) {
            connectionClosedOnError = true;
        }

        public void reconnectionCanceled() {
            reconnectionCanceled = true;
        }

        public void reconnectingIn(int seconds) {
            attemptsNotifications = attemptsNotifications + 1;
            remainingSeconds = seconds;

        }

        public void reconnectionSuccessful() {
            reconnected = true;
        }

        public void reconnectionFailed(Exception error) {
            reconnectionFailed = true;
        }
    }

}