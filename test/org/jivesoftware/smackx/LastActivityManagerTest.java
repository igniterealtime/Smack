/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2006 Jive Software.
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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.LastActivity;

public class LastActivityManagerTest extends SmackTestCase {

	/**
	 * This is a test to check if a LastActivity request for idle time is
	 * answered and correct.
	 */
	public void testOnline() {
		XMPPConnection conn0 = getConnection(0);
		XMPPConnection conn1 = getConnection(1);

		// Send a message as the last activity action from connection 1 to
		// connection 0
		conn1.sendPacket(new Message(getBareJID(0)));

		// Wait 1 seconds to have some idle time
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("Thread sleep interrupted");
		}

		LastActivity lastActivity = null;
		try {
			lastActivity = LastActivityManager.getLastActivity(conn0, getFullJID(1));
		} catch (XMPPException e) {
			e.printStackTrace();
			fail("An error occurred requesting the Last Activity");
		}

		// Asserts that the last activity packet was received
		assertNotNull("No last activity packet", lastActivity);
		// Asserts that there is at least a 1 second of idle time
        assertTrue(
                "The last activity idle time is less than expected: " + lastActivity.getIdleTime(),
                lastActivity.getIdleTime() >= 1);
    }

	/**
	 * This is a test to check if a denied LastActivity response is handled correctly.
	 */
	public void testOnlinePermisionDenied() {
		XMPPConnection conn0 = getConnection(0);
		XMPPConnection conn2 = getConnection(2);

		// Send a message as the last activity action from connection 2 to
		// connection 0
		conn2.sendPacket(new Message(getBareJID(0)));

		// Wait 1 seconds to have some idle time
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("Thread sleep interrupted");
		}

		try {
			LastActivityManager.getLastActivity(conn0, getFullJID(2));
            fail("No error was received from the server. User was able to get info of other user not in his roster.");
        } catch (XMPPException e) {
            assertNotNull("No error was returned from the server", e.getXMPPError());
            assertEquals("Forbidden error was not returned from the server", 403,
                    e.getXMPPError().getCode());
        }
	}

	/**
	 * This is a test to check if a LastActivity request for last logged out
	 * lapsed time is answered and correct
	 */
	public void testLastLoggedOut() {
		XMPPConnection conn0 = getConnection(0);

		LastActivity lastActivity = null;
		try {
			lastActivity = LastActivityManager.getLastActivity(conn0, getBareJID(1));
		} catch (XMPPException e) {
			e.printStackTrace();
			fail("An error occurred requesting the Last Activity");
		}

		assertNotNull("No last activity packet", lastActivity);
        assertTrue("The last activity idle time should be 0 since the user is logged in: " +
                lastActivity.getIdleTime(), lastActivity.getIdleTime() == 0);
    }

	/**
	 * This is a test to check if a LastActivity request for server uptime
	 * is answered and correct
	 */
	public void testServerUptime() {
		XMPPConnection conn0 = getConnection(0);

		LastActivity lastActivity = null;
		try {
			lastActivity = LastActivityManager.getLastActivity(conn0, getHost());
		} catch (XMPPException e) {
			if (e.getXMPPError().getCode() == 403) {
				//The test can not be done since the host do not allow this kind of request
				return;
			}
			e.printStackTrace();
			fail("An error occurred requesting the Last Activity");
		}

		assertNotNull("No last activity packet", lastActivity);
        assertTrue("The last activity idle time should be greater than 0 : " +
                lastActivity.getIdleTime(), lastActivity.getIdleTime() > 0);
    }

	public LastActivityManagerTest(String name) {
		super(name);
	}

	@Override
	protected int getMaxConnections() {
		return 3;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		try {
			getConnection(0).getRoster().createEntry(getBareJID(1), "User1", null);
			Thread.sleep(300);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
