/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
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
            XMPPConnection connection = new XMPPConnection(getHost(), getPort());
            try {
                // Login with an invalid user
                connection.login("invaliduser" , "invalidpass");
                connection.close();
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
        try {
            XMPPConnection conn1 = new XMPPConnection(getHost(), getPort());
            XMPPConnection conn2 = new XMPPConnection(getHost(), getPort());
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
                e.printStackTrace();
                fail(e.getMessage());
            }
            // Close the connection
            conn1.close();
            conn2.close();
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
        try {
            ConnectionConfiguration config = new ConnectionConfiguration(getHost(), getPort());
            config.setSASLAuthenticationEnabled(false);
            XMPPConnection conn1 = new XMPPConnection(config);

            config = new ConnectionConfiguration(getHost(), getPort());
            config.setSASLAuthenticationEnabled(false);
            XMPPConnection conn2 = new XMPPConnection(config);
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
                e.printStackTrace();
                fail(e.getMessage());
            }
            // Close the connection
            conn1.close();
            conn2.close();
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
            XMPPConnection conn = new XMPPConnection(getHost(), getPort());
            try {
                conn.getAccountManager().createAccount("user_1", "user_1");
            } catch (XMPPException e) {
                // Do nothing if the accout already exists
                if (e.getXMPPError().getCode() != 409) {
                    throw e;
                }
            }
            conn.login("user_1", "user_1", null);
            if (conn.getSASLAuthentication().isAuthenticated()) {
                // Check that the server assigned a resource
                assertNotNull("JID assigned by server is missing", conn.getUser());
                assertNotNull("JID assigned by server does not have a resource",
                        StringUtils.parseResource(conn.getUser()));
                conn.close();
            }
            else {
                fail("User with no resource was able to log into the server");
            }

        } catch (XMPPException e) {
            assertEquals("Wrong error code returned", 406, e.getXMPPError().getCode());
        }
    }

    protected int getMaxConnections() {
        return 0;
    }
}
