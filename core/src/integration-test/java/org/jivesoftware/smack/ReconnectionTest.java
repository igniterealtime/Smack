/**
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.ping.PingManager;

/**
 * Tests the connection and reconnection mechanism
 *
 * @author Francisco Vives
 */

public class ReconnectionTest extends SmackTestCase {

    private static final long MIN_RECONNECT_WAIT = 17;  // Seconds
    
    public ReconnectionTest(String arg0) {
        super(arg0);
    }

    /**
     * Tests an automatic reconnection.
     * Simulates a connection error and then waits until gets reconnected.
     */

    public void testAutomaticReconnection() throws Exception {
        TCPConnection connection = getConnection(0);
        CountDownLatch latch = new CountDownLatch(1);
        TCPConnectionTestListener listener = new XMPPConnectionTestListener(latch);
        connection.addConnectionListener(listener);

        // Simulates an error in the connection
        connection.notifyConnectionError(new Exception("Simulated Error"));
        latch.await(MIN_RECONNECT_WAIT, TimeUnit.SECONDS);
        
        // After 10 seconds, the reconnection manager must reestablishes the connection
        assertEquals("The ConnectionListener.connectionStablished() notification was not fired", true, listener.reconnected);
        assertTrue("The ReconnectionManager algorithm has reconnected without waiting at least 5 seconds", listener.attemptsNotifications > 0);

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);
    }

    public void testAutomaticReconnectionWithCompression() throws Exception {
        // Create the configuration for this new connection
        ConnectionConfiguration config = new ConnectionConfiguration(getHost(), getPort());
        config.setCompressionEnabled(true);
        config.setSASLAuthenticationEnabled(true);

        TCPConnection connection = new XMPPConnection(config);
        // Connect to the server
        connection.connect();
        // Log into the server
        connection.login(getUsername(0), getPassword(0), "MyOtherResource");

        assertTrue("Failed to use compression", connection.isUsingCompression());

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);

        CountDownLatch latch = new CountDownLatch(1);
        TCPConnectionTestListener listener = new XMPPConnectionTestListener(latch);
        connection.addConnectionListener(listener);

        // Simulates an error in the connection
        connection.notifyConnectionError(new Exception("Simulated Error"));
        latch.await(MIN_RECONNECT_WAIT, TimeUnit.SECONDS);
        
        // After 10 seconds, the reconnection manager must reestablishes the connection
        assertEquals("The ConnectionListener.connectionEstablished() notification was not fired", true, listener.reconnected);
        assertTrue("The ReconnectionManager algorithm has reconnected without waiting at least 5 seconds", listener.attemptsNotifications > 0);

        // Executes some server interaction testing the connection
        executeSomeServerInteraction(connection);
    }

    /**
     * Tests a manual reconnection.
     * Simulates a connection error, disables the reconnection mechanism and then reconnects.
     */
    public void testManualReconnectionWithCancelation() throws Exception {
        TCPConnection connection = getConnection(0);
        CountDownLatch latch = new CountDownLatch(1);
        TCPConnectionTestListener listener = new XMPPConnectionTestListener(latch);
        connection.addConnectionListener(listener);

        // Produces a connection error
        connection.notifyConnectionError(new Exception("Simulated Error"));
        assertEquals(
                "An error occurs but the ConnectionListener.connectionClosedOnError(e) was not notified",
                true, listener.connectionClosedOnError);
//        Thread.sleep(1000);
        
        // Cancels the automatic reconnection
        connection.getConfiguration().setReconnectionAllowed(false);
        // Waits for a reconnection that must not happened.
        Thread.sleep(MIN_RECONNECT_WAIT * 1000);
        // Cancels the automatic reconnection
        assertEquals(false, listener.reconnected);

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
        TCPConnection connection = getConnection(0);
        String username = connection.getConfiguration().getUsername();
        String password = connection.getConfiguration().getPassword();
        TCPConnectionTestListener listener = new XMPPConnectionTestListener();
        connection.addConnectionListener(listener);

        // Produces a normal disconnection
        connection.disconnect();
        assertEquals("ConnectionListener.connectionClosed() was not notified",
                true, listener.connectionClosed);
        // Waits 10 seconds waiting for a reconnection that must not happened.
        Thread.sleep(MIN_RECONNECT_WAIT * 1000);
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
        TCPConnection connection = createConnection();
        connection.connect();
        TCPConnectionTestListener listener = new XMPPConnectionTestListener();
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

    private TCPConnection createXMPPConnection() throws Exception {
        TCPConnection connection;
        // Create the configuration
        ConnectionConfiguration config = new ConnectionConfiguration(getHost(), getPort());
        config.setCompressionEnabled(Boolean.getBoolean("test.compressionEnabled"));
        config.setSASLAuthenticationEnabled(true);
        connection = new TCPConnection(config);

        return connection;
    }

    /**
     * Execute some server interaction in order to test that the regenerated connection works fine.
     */
    private void executeSomeServerInteraction(TCPConnection connection) throws XMPPException {
        PingManager pingManager = PingManager.getInstanceFor(connection);
        pingManager.pingMyServer();
    }

    protected int getMaxConnections() {
        return 1;
    }

    private class TCPConnectionTestListener implements ConnectionListener {

        // Variables to support listener notifications verification
        private volatile boolean connectionClosed = false;
        private volatile boolean connectionClosedOnError = false;
        private volatile boolean reconnected = false;
        private volatile boolean reconnectionFailed = false;
        private volatile int remainingSeconds = 0;
        private volatile int attemptsNotifications = 0;
        private volatile boolean reconnectionCanceled = false;
        private CountDownLatch countDownLatch;

        private TCPConnectionTestListener(CountDownLatch latch) {
            countDownLatch = latch; 
        }

        private TCPConnectionTestListener() {
        }
        /**
         * Methods to test the listener.
         */
        public void connectionClosed() {
            connectionClosed = true;
            
            if (countDownLatch != null)
                countDownLatch.countDown();
        }

        public void connectionClosedOnError(Exception e) {
            connectionClosedOnError = true;
        }

        public void reconnectionCanceled() {
            reconnectionCanceled = true;

            if (countDownLatch != null)
                countDownLatch.countDown();
        }

        public void reconnectingIn(int seconds) {
            attemptsNotifications = attemptsNotifications + 1;
            remainingSeconds = seconds;
        }

        public void reconnectionSuccessful() {
            reconnected = true;

            if (countDownLatch != null)
                countDownLatch.countDown();
        }

        public void reconnectionFailed(Exception error) {
            reconnectionFailed = true;

            if (countDownLatch != null)
                countDownLatch.countDown();
        }
    }

}
