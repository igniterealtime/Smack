/**
 * $RCSfile$
 * $Revision: 3177 $
 * $Date:  $
 *
 * Copyright 2003-2005 Jive Software.
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

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
        XMPPConnection connection = new XMPPConnection(getHost(), getPort());

        assertTrue("Server doesn't support stream compression", connection.getServerSupportsCompression());

        assertTrue("Failed to negotiate stream compression", connection.useCompression());

        assertTrue("Connection is not using stream compression", connection.isUsingCompression());

        // Login with the test account
        connection.login("user0", "user0");
        // Close connection
        connection.close();
    }

    protected int getMaxConnections() {
        return 0;
    }

    /**
     * Just create an account.
     */
    protected void setUp() throws Exception {
        super.setUp();
        XMPPConnection setupConnection = new XMPPConnection(getHost(), getPort());
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
        XMPPConnection setupConnection = new XMPPConnection(getHost(), getPort());
        setupConnection.login("user0", "user0");
        // Delete the created account for the test
        setupConnection.getAccountManager().deleteAccount();
        // Close the setupConnection
        setupConnection.close();
    }

}
