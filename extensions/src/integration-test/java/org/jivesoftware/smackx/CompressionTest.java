/**
 *
 * Copyright 2003-2005 Jive Software.
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.Version;

/**
 * Ensure that stream compression (JEP-138) is correctly supported by Smack.
 *
 * @author Gaston Dombiak
 */
public class CompressionTest extends SmackTestCase {

    public CompressionTest(String arg0) {
        super(arg0);
    }

    /**
     * Test that stream compression works fine. It is assumed that the server supports and has
     * stream compression enabled.
     */
    public void testSuccessCompression() throws XMPPException {

        // Create the configuration for this new connection
        ConnectionConfiguration config = new ConnectionConfiguration(getHost(), getPort());
        config.setCompressionEnabled(true);
        config.setSASLAuthenticationEnabled(true);

        TCPConnection connection = new XMPPConnection(config);
        connection.connect();

        // Login with the test account
        connection.login("user0", "user0");

        assertTrue("Connection is not using stream compression", connection.isUsingCompression());

        // Request the version of the server
        Version version = new Version();
        version.setType(IQ.Type.GET);
        version.setTo(getServiceName());

        // Create a packet collector to listen for a response.
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(version.getPacketID()));

        connection.sendPacket(version);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Close the collector
        collector.cancel();

        assertNotNull("No reply was received from the server", result);
        assertEquals("Incorrect IQ type from server", IQ.Type.RESULT, result.getType());

        // Close connection
        connection.disconnect();
    }

    protected int getMaxConnections() {
        return 0;
    }

    /**
     * Just create an account.
     */
    protected void setUp() throws Exception {
        super.setUp();
        TCPConnection setupConnection = new XMPPConnection(getServiceName());
        setupConnection.connect();
        if (!setupConnection.getAccountManager().supportsAccountCreation())
            fail("Server does not support account creation");

        // Create the test account
        try {
            setupConnection.getAccountManager().createAccount("user0", "user0");
        } catch (XMPPException e) {
            // Do nothing if the accout already exists
            if (e.getXMPPError().getCode() != 409) {
                throw e;
            }
        }
    }

    /**
     * Deletes the created account for the test.
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        TCPConnection setupConnection = createConnection();
        setupConnection.connect();
        setupConnection.login("user0", "user0");
        // Delete the created account for the test
        setupConnection.getAccountManager().deleteAccount();
        // Close the setupConnection
        setupConnection.disconnect();
    }
}
