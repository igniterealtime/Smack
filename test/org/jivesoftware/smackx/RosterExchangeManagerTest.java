/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx;

import java.util.Iterator;

import junit.framework.TestCase;

import org.jivesoftware.smack.*;

/**
 *
 * Test the Roster Exchange extension using the high level API
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeManagerTest extends TestCase {

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;
    private XMPPConnection conn3 = null;
    private XMPPConnection conn4 = null;

    private String user1 = null;
    private String user2 = null;
    private String user3 = null;
    private String user4 = null;

    private int entriesSent;
    private int entriesReceived;

    /**
     * Constructor for RosterExchangeManagerTest.
     * @param name
     */
    public RosterExchangeManagerTest(String name) {
        super(name);
    }

    /**
     * High level API test.
     * This is a simple test to use with a XMPP client and check if the client receives user1's roster
     * 1. User_1 will send his/her roster to user_2
     */
    public void testSendRoster() {
        // Send user1's roster to user2
        try {
            RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
            rosterExchangeManager.send(conn1.getRoster(), user2);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured sending the roster");
        }
    }

    /**
     * High level API test.
     * This is a simple test to use with a XMPP client and check if the client receives user1's roster groups
     * 1. User_1 will send his/her RosterGroups to user_2
     */
    public void testSendRosterGroup() {
        // Send user1's RosterGroups to user2
        try {
            RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
            for (Iterator it = conn1.getRoster().getGroups(); it.hasNext();)
                rosterExchangeManager.send((RosterGroup) it.next(), user2);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured sending the roster");
        }
    }

    /**
     * High level API test.
     * 1. User_1 will send his/her roster to user_2
     * 2. User_2 will receive the entries and iterate over them to check if everything is fine
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then something is wrong
     */
    public void testSendAndReceiveRoster() {
        RosterExchangeManager rosterExchangeManager1 = new RosterExchangeManager(conn1);
        RosterExchangeManager rosterExchangeManager2 = new RosterExchangeManager(conn2);

        // Create a RosterExchangeListener that will iterate over the received roster entries
        RosterExchangeListener rosterExchangeListener = new RosterExchangeListener() {
            public void entriesReceived(String from, Iterator remoteRosterEntries) {
                int received = 0;
                assertNotNull("From is null", from);
                assertNotNull("rosterEntries is null", remoteRosterEntries);
                assertTrue("Roster without entries", remoteRosterEntries.hasNext());
                while (remoteRosterEntries.hasNext()) {
                    received++;
                    RemoteRosterEntry remoteEntry = (RemoteRosterEntry) remoteRosterEntries.next();
                    System.out.println(remoteEntry);
                }
                entriesReceived = received;
            }
        };
        rosterExchangeManager2.addRosterListener(rosterExchangeListener);

        // Send user1's roster to user2
        try {
            entriesSent = conn1.getRoster().getEntryCount();
            entriesReceived = 0;
            rosterExchangeManager1.send(conn1.getRoster(), user2);
            // Wait for 1 second
            Thread.sleep(300);
        } catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
        assertEquals(
            "Number of sent and received entries does not match",
            entriesSent,
            entriesReceived);
    }

    /**
     * High level API test.
     * 1. User_1 will send his/her roster to user_2
     * 2. User_2 will automatically add the entries that receives to his/her roster in the corresponding group
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then something is wrong
     */
    public void testSendAndAcceptRoster() {
        RosterExchangeManager rosterExchangeManager1 = new RosterExchangeManager(conn1);
        RosterExchangeManager rosterExchangeManager2 = new RosterExchangeManager(conn2);

        // Create a RosterExchangeListener that will accept all the received roster entries
        RosterExchangeListener rosterExchangeListener = new RosterExchangeListener() {
            public void entriesReceived(String from, Iterator remoteRosterEntries) {
                int received = 0;
                assertNotNull("From is null", from);
                assertNotNull("remoteRosterEntries is null", remoteRosterEntries);
                assertTrue("Roster without entries", remoteRosterEntries.hasNext());
                while (remoteRosterEntries.hasNext()) {
                    received++;
                    try {
                        RemoteRosterEntry remoteRosterEntry =
                            (RemoteRosterEntry) remoteRosterEntries.next();
                        conn2.getRoster().createEntry(
                            remoteRosterEntry.getUser(),
                            remoteRosterEntry.getName(),
                            remoteRosterEntry.getGroupArrayNames());
                    } catch (Exception e) {
                        fail(e.toString());
                    }
                }
                entriesReceived = received;
            }
        };
        rosterExchangeManager2.addRosterListener(rosterExchangeListener);

        // Send user1's roster to user2
        try {
            entriesSent = conn1.getRoster().getEntryCount();
            entriesReceived = 0;
            rosterExchangeManager1.send(conn1.getRoster(), user2);
            // Wait for 1 seconds
            Thread.sleep(400);
        } catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
        assertEquals(
            "Number of sent and received entries does not match",
            entriesSent,
            entriesReceived);
        assertTrue("Roster2 has no entries", conn2.getRoster().getEntryCount() > 0);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        try {
            // Connect to the server
            conn1 = new XMPPConnection("localhost");
            conn2 = new XMPPConnection("localhost");
            conn3 = new XMPPConnection("localhost");
            conn4 = new XMPPConnection("localhost");

            // Create the test accounts
            if (!conn1.getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");
            conn1.getAccountManager().createAccount("gato3", "gato3");
            conn2.getAccountManager().createAccount("gato4", "gato4");
            conn3.getAccountManager().createAccount("gato5", "gato5");
            conn4.getAccountManager().createAccount("gato6", "gato6");

            // Login with the test accounts
            conn1.login("gato3", "gato3");
            conn2.login("gato4", "gato4");
            conn3.login("gato5", "gato5");
            conn4.login("gato6", "gato6");

            user1 = "gato3@" + conn1.getHost();
            user2 = "gato4@" + conn2.getHost();
            user3 = "gato5@" + conn3.getHost();
            user4 = "gato6@" + conn4.getHost();

            conn1.getRoster().createEntry(
                "gato5@" + conn3.getHost(),
                "gato5",
                new String[] { "Friends, Coworker" });
            conn1.getRoster().createEntry("gato6@" + conn4.getHost(), "gato6", null);
            Thread.sleep(100);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        // Delete the created accounts for the test
        conn1.getAccountManager().deleteAccount();
        conn2.getAccountManager().deleteAccount();
        conn3.getAccountManager().deleteAccount();
        conn4.getAccountManager().deleteAccount();

        // Close all the connections
        conn1.close();
        conn2.close();
        conn3.close();
        conn4.close();
    }
}
