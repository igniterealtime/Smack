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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 *
 * Test the Roster Exchange extension using the high level API
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeManagerTest extends SmackTestCase {

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
     * This is a simple test to use with a XMPP client and check if the client receives user1's 
     * roster
     * 1. User_1 will send his/her roster to user_2
     */
    public void testSendRoster() {
        // Send user1's roster to user2
        try {
            RosterExchangeManager rosterExchangeManager =
                new RosterExchangeManager(getConnection(0));
            rosterExchangeManager.send(getConnection(0).getRoster(), getBareJID(1));
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("An error occured sending the roster");
        }
    }

    /**
     * High level API test.
     * This is a simple test to use with a XMPP client and check if the client receives user1's 
     * roster groups
     * 1. User_1 will send his/her RosterGroups to user_2
     */
    public void testSendRosterGroup() {
        // Send user1's RosterGroups to user2
        try {
            RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(getConnection(0));
            for (RosterGroup rosterGroup : getConnection(0).getRoster().getGroups()) {
                rosterExchangeManager.send(rosterGroup, getBareJID(1));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("An error occured sending the roster");
        }
    }

    /**
     * High level API test.
     * 1. User_1 will send his/her roster to user_2
     * 2. User_2 will receive the entries and iterate over them to check if everything is fine
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then 
     * something is wrong
     */
    public void testSendAndReceiveRoster() {
        RosterExchangeManager rosterExchangeManager1 = new RosterExchangeManager(getConnection(0));
        RosterExchangeManager rosterExchangeManager2 = new RosterExchangeManager(getConnection(1));

        // Create a RosterExchangeListener that will iterate over the received roster entries
        RosterExchangeListener rosterExchangeListener = new RosterExchangeListener() {
            public void entriesReceived(String from, Iterator<RemoteRosterEntry> remoteRosterEntries) {
                int received = 0;
                assertNotNull("From is null", from);
                assertNotNull("rosterEntries is null", remoteRosterEntries);
                assertTrue("Roster without entries", remoteRosterEntries.hasNext());
                while (remoteRosterEntries.hasNext()) {
                    received++;
                    RemoteRosterEntry remoteEntry = remoteRosterEntries.next();
                    System.out.println(remoteEntry);
                }
                entriesReceived = received;
            }
        };
        rosterExchangeManager2.addRosterListener(rosterExchangeListener);

        // Send user1's roster to user2
        try {
            entriesSent = getConnection(0).getRoster().getEntryCount();
            entriesReceived = 0;
            rosterExchangeManager1.send(getConnection(0).getRoster(), getBareJID(1));
            // Wait up to 2 seconds
            long initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 2000 &&
                    (entriesSent != entriesReceived)) {
                Thread.sleep(100);
            }
        }
        catch (Exception e) {
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
     * 2. User_2 will automatically add the entries that receives to his/her roster in the 
     * corresponding group
     * 3. User_1 will wait several seconds for an ACK from user_2, if none is received then 
     * something is wrong
     */
    public void testSendAndAcceptRoster() {
        RosterExchangeManager rosterExchangeManager1 = new RosterExchangeManager(getConnection(0));
        RosterExchangeManager rosterExchangeManager2 = new RosterExchangeManager(getConnection(1));

        // Create a RosterExchangeListener that will accept all the received roster entries
        RosterExchangeListener rosterExchangeListener = new RosterExchangeListener() {
            public void entriesReceived(String from, Iterator<RemoteRosterEntry> remoteRosterEntries) {
                int received = 0;
                assertNotNull("From is null", from);
                assertNotNull("remoteRosterEntries is null", remoteRosterEntries);
                assertTrue("Roster without entries", remoteRosterEntries.hasNext());
                while (remoteRosterEntries.hasNext()) {
                    received++;
                    try {
                        RemoteRosterEntry remoteRosterEntry = remoteRosterEntries.next();
                        getConnection(1).getRoster().createEntry(
                            remoteRosterEntry.getUser(),
                            remoteRosterEntry.getName(),
                            remoteRosterEntry.getGroupArrayNames());
                    }
                    catch (Exception e) {
                        fail(e.toString());
                    }
                }
                entriesReceived = received;
            }
        };
        rosterExchangeManager2.addRosterListener(rosterExchangeListener);

        // Send user1's roster to user2
        try {
            entriesSent = getConnection(0).getRoster().getEntryCount();
            entriesReceived = 0;
            rosterExchangeManager1.send(getConnection(0).getRoster(), getBareJID(1));
            // Wait up to 2 seconds
            long initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 2000 &&
                    (entriesSent != entriesReceived)) {
                Thread.sleep(100);
            }
        }
        catch (Exception e) {
            fail("An error occured sending the message with the roster");
        }
        assertEquals(
            "Number of sent and received entries does not match",
            entriesSent,
            entriesReceived);
        assertTrue("Roster2 has no entries", getConnection(1).getRoster().getEntryCount() > 0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        try {
            getConnection(0).getRoster().createEntry(
                getBareJID(2),
                "gato5",
                new String[] { "Friends, Coworker" });
            getConnection(0).getRoster().createEntry(getBareJID(3), "gato6", null);
            Thread.sleep(100);

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected int getMaxConnections() {
        return 4;
    }
}
