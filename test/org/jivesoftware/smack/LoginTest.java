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
                assertEquals("Incorrect error code while login with an invalid user", 401,
                        e.getXMPPError().getCode());
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
    public void testAnonymousLogin() {
        try {
            XMPPConnection conn1 = new XMPPConnection(getHost(), getPort());
            XMPPConnection conn2 = new XMPPConnection(getHost(), getPort());
            try {
                // Try to login anonymously
                conn1.loginAnonymously();
                conn2.loginAnonymously();
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


    protected int getMaxConnections() {
        return 0;
    }
}
