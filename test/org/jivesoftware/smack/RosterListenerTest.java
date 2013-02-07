/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Test cases for adding the {@link RosterListener} in different connection states.
 * 
 * @author Henning Staib
 */
public class RosterListenerTest extends SmackTestCase {

    public RosterListenerTest(String arg0) {
        super(arg0);
    }

    public void testAddingRosterListenerBeforeConnect() throws Exception {
        int inviterIndex = 0;
        int inviteeIndex = 1;
        XMPPConnection inviterConnection = getConnection(inviterIndex);
        connectAndLogin(inviterIndex);

        assertTrue("Inviter is not online", inviterConnection.isConnected());

        Roster inviterRoster = inviterConnection.getRoster();

        // add user1 to roster to create roster events stored at XMPP server
        inviterRoster.createEntry(getBareJID(inviteeIndex), getUsername(inviteeIndex), null);

        Thread.sleep(500); // wait for XMPP server

        XMPPConnection inviteeConnection = getConnection(inviteeIndex);
        assertFalse("Invitee is already online", inviteeConnection.isConnected());

        // collector for added entries
        final List<String> addedEntries = new ArrayList<String>();

        // register roster listener before login
        Roster inviteeRoster = inviteeConnection.getRoster();
        inviteeRoster.addRosterListener(new RosterListener() {

            public void presenceChanged(Presence presence) {
                // ignore
            }

            public void entriesUpdated(Collection<String> addresses) {
                // ignore
            }

            public void entriesDeleted(Collection<String> addresses) {
                // ignore
            }

            public void entriesAdded(Collection<String> addresses) {
                addedEntries.addAll(addresses);
            }
        });

        // connect after adding the listener
        connectAndLogin(inviteeIndex);

        Thread.sleep(500); // wait for packets to be processed

        assertNotNull("inviter is not in roster", inviteeRoster.getEntry(getBareJID(inviterIndex)));

        assertTrue("got no event for adding inviter",
                        addedEntries.contains(getBareJID(inviterIndex)));

    }

    public void testAddingRosterListenerAfterConnect() throws Exception {
        int inviterIndex = 0;
        int inviteeIndex = 1;
        XMPPConnection inviterConnection = getConnection(inviterIndex);
        connectAndLogin(inviterIndex);
        assertTrue("Inviter is not online", inviterConnection.isConnected());

        Roster inviterRoster = inviterConnection.getRoster();

        // add user1 to roster to create roster events stored at XMPP server
        inviterRoster.createEntry(getBareJID(inviteeIndex), getUsername(inviteeIndex), null);

        Thread.sleep(500); // wait for XMPP server

        XMPPConnection inviteeConnection = getConnection(inviteeIndex);
        connectAndLogin(inviteeIndex);
        assertTrue("Invitee is not online", inviteeConnection.isConnected());

        // collector for added entries
        final List<String> addedEntries = new ArrayList<String>();

        // wait to simulate concurrency before adding listener
        Thread.sleep(200);

        // register roster listener after login
        Roster inviteeRoster = inviteeConnection.getRoster();
        inviteeRoster.addRosterListener(new RosterListener() {

            public void presenceChanged(Presence presence) {
                // ignore
            }

            public void entriesUpdated(Collection<String> addresses) {
                // ignore
            }

            public void entriesDeleted(Collection<String> addresses) {
                // ignore
            }

            public void entriesAdded(Collection<String> addresses) {
                addedEntries.addAll(addresses);
            }
        });

        Thread.sleep(500); // wait for packets to be processed

        assertNotNull("Inviter is not in roster", inviteeRoster.getEntry(getBareJID(inviterIndex)));

        assertFalse("got event for adding inviter", addedEntries.contains(getBareJID(inviterIndex)));

    }

    @Override
    protected void tearDown() throws Exception {
        cleanUpRoster();
        super.tearDown();
    }

    protected int getMaxConnections() {
        return 2;
    }

    protected boolean createOfflineConnections() {
        return true;
    }

    /**
     * Clean up all the entries in the roster
     */
    private void cleanUpRoster() {
        for (int i = 0; i < getMaxConnections(); i++) {
            // Delete all the entries from the roster
            Roster roster = getConnection(i).getRoster();
            for (RosterEntry entry : roster.getEntries()) {
                try {
                    roster.removeEntry(entry);
                    Thread.sleep(100);
                }
                catch (XMPPException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }

            try {
                Thread.sleep(700);
            }
            catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }
        // Wait up to 6 seconds to receive roster removal notifications
        long initial = System.currentTimeMillis();
        while (System.currentTimeMillis() - initial < 6000
                        && (getConnection(0).getRoster().getEntryCount() != 0 || getConnection(1).getRoster().getEntryCount() != 0)) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
            }
        }

        assertEquals("Wrong number of entries in connection 0", 0,
                        getConnection(0).getRoster().getEntryCount());
        assertEquals("Wrong number of groups in connection 0", 0,
                        getConnection(0).getRoster().getGroupCount());

        assertEquals("Wrong number of entries in connection 1", 0,
                        getConnection(1).getRoster().getEntryCount());
        assertEquals("Wrong number of groups in connection 1", 0,
                        getConnection(1).getRoster().getGroupCount());

    }

}
