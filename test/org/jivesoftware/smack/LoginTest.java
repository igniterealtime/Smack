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
import org.jivesoftware.smack.util.StringUtils;

/**
 * Includes set of login tests. 
 *
 * @author Gaston Dombiak
 */
public class LoginTest extends SmackTestCase {

    public LoginTest(String arg0) {
        super(arg0);
    }

    /**
     * Check that the server is returning the correct error when trying to login using an invalid
     * (i.e. non-existent) user.
     */
    public void testInvalidLogin() {
        try {
            XMPPConnection connection = createConnection();
            connection.connect();
            try {
                // Login with an invalid user
                connection.login("invaliduser" , "invalidpass");
                connection.disconnect();
                fail("Invalid user was able to log into the server");
            }
            catch (XMPPException e) {
                if (e.getXMPPError() != null) {
                    assertEquals("Incorrect error code while login with an invalid user", 401,
                            e.getXMPPError().getCode());
                }
            }
            // Wait here while trying tests with exodus
            //Thread.sleep(300);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Check that the server handles anonymous users correctly.
     */
    public void testSASLAnonymousLogin() {
        if (!isTestAnonymousLogin()) return;

        try {
            XMPPConnection conn1 = createConnection();
            XMPPConnection conn2 = createConnection();
            conn1.connect();
            conn2.connect();
            try {
                // Try to login anonymously
                conn1.loginAnonymously();
                conn2.loginAnonymously();

                assertNotNull("Resource is null", StringUtils.parseResource(conn1.getUser()));
                assertNotNull("Resource is null", StringUtils.parseResource(conn2.getUser()));

                assertNotNull("Username is null", StringUtils.parseName(conn1.getUser()));
                assertNotNull("Username is null", StringUtils.parseName(conn2.getUser()));
            }
            catch (XMPPException e) {
                fail(e.getMessage());
            }
            finally {
                // Close the connection
                conn1.disconnect();
                conn2.disconnect();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Check that the server handles anonymous users correctly.
     */
    public void testNonSASLAnonymousLogin() {
        if (!isTestAnonymousLogin()) return;

        try {
            ConnectionConfiguration config = new ConnectionConfiguration(getHost(), getPort());
            config.setSASLAuthenticationEnabled(false);
            XMPPConnection conn1 = new XMPPConnection(config);
            conn1.connect();

            config = new ConnectionConfiguration(getHost(), getPort());
            config.setSASLAuthenticationEnabled(false);
            XMPPConnection conn2 = new XMPPConnection(config);
            conn2.connect();
            
            try {
                // Try to login anonymously
                conn1.loginAnonymously();
                conn2.loginAnonymously();

                assertNotNull("Resource is null", StringUtils.parseResource(conn1.getUser()));
                assertNotNull("Resource is null", StringUtils.parseResource(conn2.getUser()));

                assertNotNull("Username is null", StringUtils.parseName(conn1.getUser()));
                assertNotNull("Username is null", StringUtils.parseName(conn2.getUser()));
            }
            catch (XMPPException e) {
                fail(e.getMessage());
            }
            // Close the connection
            conn1.disconnect();
            conn2.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Check that the server does not allow to log in without specifying a resource.
     */
    public void testLoginWithNoResource() {
        try {
            XMPPConnection conn = createConnection();
            conn.connect();
            try {
                conn.getAccountManager().createAccount("user_1", "user_1", getAccountCreationParameters());
            } catch (XMPPException e) {
                // Do nothing if the account already exists
                if (e.getXMPPError().getCode() != 409) {
                    throw e;
                }
                // Else recreate the connection, ins case the server closed it as
                // a result of the error, so we can login.
                conn = createConnection();
                conn.connect();
            }
            conn.login("user_1", "user_1", (String) null);
            if (conn.getSASLAuthentication().isAuthenticated()) {
                // Check that the server assigned a resource
                assertNotNull("JID assigned by server is missing", conn.getUser());
                assertNotNull("JID assigned by server does not have a resource",
                        StringUtils.parseResource(conn.getUser()));
                conn.disconnect();
            }
            else {
                fail("User with no resource was not able to log into the server");
            }

        } catch (XMPPException e) {
            if (e.getXMPPError() != null) {
                assertEquals("Wrong error code returned", 406, e.getXMPPError().getCode());
            } else {
                fail(e.getMessage());
            }
        }
    }

    protected int getMaxConnections() {
        return 0;
    }
}
