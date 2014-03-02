/**
 *
 * Copyright 2005 Jive Software.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.util.StringUtils;
import org.mockito.internal.util.RemoveFirstLine;

/**
 * Tests the Roster functionality by creating and removing roster entries.
 *
 * @author Gaston Dombiak
 */
public class RosterSmackTest extends SmackTestCase {

    /**
     * Constructor for RosterSmackTest.
     * @param name
     */
    public RosterSmackTest(String name) {
        super(name);
    }

    
    /**
     * 1. Create entries in roster groups
     * 2. Iterate on the groups and remove the entry from each group
     * 3. Check that the entries are kept as unfiled entries
     */
    public void testDeleteAllRosterGroupEntries() {
        try {
            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();

            CountDownLatch latch = new CountDownLatch(2);
            setupCountdown(latch, roster);
            
            roster.createEntry(getBareJID(1), "gato11", new String[] { "Friends", "Family" });
            roster.createEntry(getBareJID(2), "gato12", new String[] { "Family" });
            
            waitForCountdown(latch, roster, 2);

            final CountDownLatch removeLatch = new CountDownLatch(3);
            RosterListener latchCounter = new RosterListener() {
                @Override
                public void presenceChanged(Presence presence) {}
                
                @Override
                public void entriesUpdated(Collection<String> addresses) {
                    removeLatch.countDown();
                }
                
                @Override
                public void entriesDeleted(Collection<String> addresses) {}
                
                @Override
                public void entriesAdded(Collection<String> addresses) {}
            }; 
            
            roster.addRosterListener(latchCounter);

            for (RosterEntry entry : roster.getEntries()) {
                for (RosterGroup rosterGroup : entry.getGroups()) {
                    rosterGroup.removeEntry(entry);
                }
            }
            
            removeLatch.await(5, TimeUnit.SECONDS);
            roster.removeRosterListener(latchCounter);
            
            assertEquals("The number of entries in connection 1 should be 1", 1, getConnection(1).getRoster().getEntryCount());
            assertEquals("The number of groups in connection 1 should be 0", 0, getConnection(1).getRoster().getGroupCount());
            assertEquals("The number of entries in connection 2 should be 1", 1, getConnection(2).getRoster().getEntryCount());
            assertEquals("The number of groups in connection 2 should be 0", 0, getConnection(2).getRoster().getGroupCount());
            assertEquals("The number of entries in connection 0 should be 2", 2, roster.getEntryCount());
            assertEquals("The number of groups in connection 0 should be 0", 0, roster.getGroupCount());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void setupCountdown(final CountDownLatch latch, Roster roster) {
        roster.addRosterListener(new RosterListener() {
            
            @Override
            public void presenceChanged(Presence presence) {}
            
            @Override
            public void entriesUpdated(Collection<String> addresses) {
                latch.countDown();
            }
            
            @Override
            public void entriesDeleted(Collection<String> addresses) {}
            
            @Override
            public void entriesAdded(Collection<String> addresses) {}
        });
    }

    private void waitForCountdown(CountDownLatch latch, Roster roster, int entryCount) throws InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(entryCount, roster.getEntryCount());
    }

    /**
     * 1. Create entries in roster groups
     * 2. Iterate on all the entries and remove them from the roster
     * 3. Check that the number of entries and groups is zero
     */
    public void testDeleteAllRosterEntries() throws Exception {
        // Add a new roster entry
        Roster roster = getConnection(0).getRoster();
        
        CountDownLatch latch = new CountDownLatch(2);
        setupCountdown(latch, roster);
        
        roster.createEntry(getBareJID(1), "gato11", new String[] { "Friends" });
        roster.createEntry(getBareJID(2), "gato12", new String[] { "Family" });

        waitForCountdown(latch, roster, 2);

        CountDownLatch removeLatch = new CountDownLatch(2);
        RosterListener latchCounter = new RemovalListener(removeLatch);
        roster.addRosterListener(latchCounter);

        for (RosterEntry entry : roster.getEntries()) {
            roster.removeEntry(entry);
        }

        removeLatch.await(5, TimeUnit.SECONDS);
        roster.removeRosterListener(latchCounter);

        assertEquals("Wrong number of entries in connection 0", 0, roster.getEntryCount());
        assertEquals("Wrong number of groups in connection 0", 0, roster.getGroupCount());
        assertEquals("Wrong number of entries in connection 1", 0, getConnection(1).getRoster().getEntryCount());
        assertEquals("Wrong number of groups in connection 1", 0, getConnection(1).getRoster().getGroupCount());
    }

    /**
     * 1. Create unfiled entries
     * 2. Iterate on all the entries and remove them from the roster
     * 3. Check that the number of entries and groups is zero
     */
    public void testDeleteAllUnfiledRosterEntries() {
        try {
            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            
            CountDownLatch latch = new CountDownLatch(2);
            setupCountdown(latch, roster);
            
            roster.createEntry(getBareJID(1), "gato11", null);
            roster.createEntry(getBareJID(2), "gato12", null);

            waitForCountdown(latch, roster, 2);
            CountDownLatch removeLatch = new CountDownLatch(2);
            RosterListener latchCounter = new RemovalListener(removeLatch);
            roster.addRosterListener(latchCounter);

            for (RosterEntry entry : roster.getEntries()) {
                roster.removeEntry(entry);
            }

            removeLatch.await(5, TimeUnit.SECONDS);
            roster.removeRosterListener(latchCounter);

            assertEquals("Wrong number of entries in connection 0", 0, roster.getEntryCount());
            assertEquals("Wrong number of groups in connection 0", 0, roster.getGroupCount());
            assertEquals("Wrong number of entries in connection 1", 0, getConnection(1).getRoster().getEntryCount());
            assertEquals("Wrong number of groups in connection 1", 0, getConnection(1).getRoster().getGroupCount());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 1. Create an unfiled entry
     * 2. Change its name
     * 3. Check that the name has been modified
     * 4. Reload the whole roster
     * 5. Check that the name has been modified
     */
    public void testChangeNameToUnfiledEntry() {
        try {
            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            CountDownLatch latch = new CountDownLatch(1);
            setupCountdown(latch, roster);
            
            roster.createEntry(getBareJID(1), null, null);
            
            waitForCountdown(latch, roster, 1);

            final CountDownLatch updateLatch = new CountDownLatch(2);
            RosterListener latchCounter = new RosterListener() {
                @Override
                public void entriesAdded(Collection<String> addresses) {}

                @Override
                public void entriesUpdated(Collection<String> addresses) {
                    updateLatch.countDown();
                }

                @Override
                public void entriesDeleted(Collection<String> addresses) {}

                @Override
                public void presenceChanged(Presence presence) {}
            };
            roster.addRosterListener(latchCounter);

            // Change the roster entry name and check if the change was made
            for (RosterEntry entry : roster.getEntries()) {
                entry.setName("gato11");
                assertEquals("gato11", entry.getName());
            }
            // Reload the roster and check the name again
            roster.reload();
            
            updateLatch.await(5, TimeUnit.SECONDS);
            
            for (RosterEntry entry : roster.getEntries()) {
                assertEquals("gato11", entry.getName());
            }
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 1. Create an unfiled entry with no name
     * 2. Check that the the entry does not belong to any group
     * 3. Change its name and add it to a group
     * 4. Check that the name has been modified and that the entry belongs to a group
     */
    public void testChangeGroupAndNameToUnfiledEntry() {
        try {
            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            roster.createEntry(getBareJID(1), null, null);

            Thread.sleep(500);

            getConnection(1).getRoster().createEntry(getBareJID(0), null, null);

            // Wait up to 5 seconds to receive presences of the new roster contacts
            long initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 5000 &&
                    !roster.getPresence(getBareJID(0)).isAvailable()) {
                Thread.sleep(100);
            }
            //assertNotNull("Presence not received", roster.getPresence(getBareJID(0)));

            for (RosterEntry entry : roster.getEntries()) {
                assertFalse("The roster entry belongs to a group", !entry.getGroups().isEmpty());
            }

            // Change the roster entry name and check if the change was made
            roster.createEntry(getBareJID(1), "NewName", new String[] { "Friends" });

            // Reload the roster and check the name again
            Thread.sleep(200);
            for (RosterEntry entry : roster.getEntries()) {
                assertEquals("Name of roster entry is wrong", "NewName", entry.getName());
                assertTrue("The roster entry does not belong to any group", !entry.getGroups().isEmpty());
            }
            // Wait up to 5 seconds to receive presences of the new roster contacts
            initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 5000 &&
                    !roster.getPresence(getBareJID(1)).isAvailable()) {
                Thread.sleep(100);
            }
            assertTrue("Presence not received", roster.getPresence(getBareJID(1)).isAvailable());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Tests that adding an existing roster entry that belongs to a group to another group
     * works fine.
     */
    public void testAddEntryToNewGroup() {
        try {
            Thread.sleep(500);

            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            roster.createEntry(getBareJID(1), "gato11", new String[] { "Friends" });
            roster.createEntry(getBareJID(2), "gato12", new String[] { "Family" });

            // Wait up to 2 seconds to receive new roster contacts
            long initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 2000  && roster.getEntryCount() != 2) {
                Thread.sleep(100);
            }

            assertEquals("Wrong number of entries in connection 0", 2, roster.getEntryCount());

            // Add "gato11" to a new group called NewGroup
            roster.createGroup("NewGroup").addEntry(roster.getEntry(getBareJID(1)));


            // Log in from another resource so we can test the roster
            TCPConnection con2 = createConnection();
            con2.connect();
            con2.login(getUsername(0), getUsername(0), "MyNewResource");

            Roster roster2 = con2.getRoster();

            assertEquals("Wrong number of entries in new connection", 2, roster2.getEntryCount());
            assertEquals("Wrong number of groups in new connection", 3, roster2.getGroupCount());


            RosterEntry entry = roster2.getEntry(getBareJID(1));
            assertNotNull("No entry for user 1 was found", entry);

            List<String> groupNames = new ArrayList<String>();
            for (RosterGroup rosterGroup : entry.getGroups()) {
                groupNames.add(rosterGroup.getName());
            }
            assertTrue("Friends group was not found", groupNames.contains("Friends"));
            assertTrue("NewGroup group was not found", groupNames.contains("NewGroup"));

            // Close the new connection
            con2.disconnect();
            Thread.sleep(500);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test if renaming a roster group works fine.
     *
     */
    public void testRenameRosterGroup() {
        try {
            Thread.sleep(200);

            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            roster.createEntry(getBareJID(1), "gato11", new String[] { "Friends" });
            roster.createEntry(getBareJID(2), "gato12", new String[] { "Friends" });

            // Wait up to 2 seconds to let the server process presence subscriptions
            long initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 2000 && (
                    !roster.getPresence(getBareJID(1)).isAvailable() ||
                            !roster.getPresence(getBareJID(2)).isAvailable())) {
                Thread.sleep(100);
            }

            roster.getGroup("Friends").setName("Amigos");

            // Wait up to 2 seconds
            initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 2000 &&
                    (roster.getGroup("Friends") != null)) {
                Thread.sleep(100);
            }

            assertNull("The group Friends still exists", roster.getGroup("Friends"));
            assertNotNull("The group Amigos does not exist", roster.getGroup("Amigos"));
            assertEquals(
                "Wrong number of entries in the group Amigos",
                2,
                roster.getGroup("Amigos").getEntryCount());

            // Setting the name to empty is like removing this group
            roster.getGroup("Amigos").setName("");

            // Wait up to 2 seconds for the group to change its name
            initial = System.currentTimeMillis();
            while (System.currentTimeMillis() - initial < 2000 &&
                    (roster.getGroup("Amigos") != null)) {
                Thread.sleep(100);
            }

            assertNull("The group Amigos still exists", roster.getGroup("Amigos"));
            assertNull("The group with no name does exist", roster.getGroup(""));
            assertEquals("There are still groups in the roster", 0, roster.getGroupCount());
            assertEquals(
                "Wrong number of unfiled entries",
                2,
                roster.getUnfiledEntryCount());

            Thread.sleep(200);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test presence management.<p>
     * 
     * 1. Log in user0 from a client and user1 from 2 clients
     * 2. Create presence subscription of type BOTH between 2 users
     * 3. Check that presence is correctly delivered to both users
     * 4. User1 logs out from a client
     * 5. Check that presence for each connected resource is correct
     */
    public void testRosterPresences() throws Exception {
        Presence presence;

        // Create another connection for the same user of connection 1
        ConnectionConfiguration connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        TCPConnection conn4 = new XMPPConnection(connectionConfiguration);
        conn4.connect();
        conn4.login(getUsername(1), getPassword(1), "Home");

        // Add a new roster entry
        Roster roster = getConnection(0).getRoster();
        roster.createEntry(getBareJID(1), "gato11", null);

        // Wait up to 2 seconds
        long initial = System.currentTimeMillis();
        while (System.currentTimeMillis() - initial < 2000 &&
                (roster.getPresence(getBareJID(1)).getType() == Presence.Type.unavailable)) {
            Thread.sleep(100);
        }

        // Check that a presence is returned for a user
        presence = roster.getPresence(getBareJID(1));
        assertTrue("Returned a null Presence for an existing user", presence.isAvailable());

        // Check that the right presence is returned for a user+resource
        presence = roster.getPresenceResource(getUsername(1) + "@" + conn4.getServiceName() + "/Home");
        assertEquals("Returned the wrong Presence", "Home",
                StringUtils.parseResource(presence.getFrom()));

        // Check that the right presence is returned for a user+resource
        presence = roster.getPresenceResource(getFullJID(1));
        assertTrue("Presence not found for user " + getFullJID(1), presence.isAvailable());
        assertEquals("Returned the wrong Presence", "Smack",
                StringUtils.parseResource(presence.getFrom()));

        // Check the returned presence for a non-existent user+resource
        presence = roster.getPresenceResource("noname@" + getServiceName() + "/Smack");
        assertFalse("Available presence was returned for a non-existing user", presence.isAvailable());
        assertEquals("Returned Presence for a non-existing user has the incorrect type",
                Presence.Type.unavailable, presence.getType());

        // Check that the returned presences are correct
        Iterator<Presence> presences = roster.getPresences(getBareJID(1));
        int count = 0;
        while (presences.hasNext()) {
            count++;
            presences.next();
        }
        assertEquals("Wrong number of returned presences", count, 2);

        // Close the connection so one presence must go
        conn4.disconnect();

        // Check that the returned presences are correct
        presences = roster.getPresences(getBareJID(1));
        count = 0;
        while (presences.hasNext()) {
            count++;
            presences.next();
        }
        assertEquals("Wrong number of returned presences", count, 1);
    }

    /**
     * User1 is connected from 2 resources. User1 adds User0 to his roster. Ensure
     * that both resources of user1 get the available presence of User0. Remove User0
     * from User1's roster and check presences again.
     */
    public void testMultipleResources() throws Exception {
        // Create another connection for the same user of connection 1
        ConnectionConfiguration connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        TCPConnection conn4 = new XMPPConnection(connectionConfiguration);
        conn4.connect();
        conn4.login(getUsername(1), getPassword(1), "Home");

        // Add a new roster entry
        Roster roster = conn4.getRoster();
        roster.createEntry(getBareJID(0), "gato11", null);

        // Wait up to 2 seconds
        long initial = System.currentTimeMillis();
        while (System.currentTimeMillis() - initial < 2000 && (
                !roster.getPresence(getBareJID(0)).isAvailable() ||
                        !getConnection(1).getRoster().getPresence(getBareJID(0)).isAvailable())) {
            Thread.sleep(100);
        }

        // Check that a presence is returned for the new contact
        Presence presence = roster.getPresence(getBareJID(0));
        assertTrue("Returned a null Presence for an existing user", presence.isAvailable());

        // Check that a presence is returned for the new contact
        presence = getConnection(1).getRoster().getPresence(getBareJID(0));
        assertTrue("Returned a null Presence for an existing user", presence.isAvailable());

        // Delete user from roster
        roster.removeEntry(roster.getEntry(getBareJID(0)));

        // Wait up to 2 seconds
        initial = System.currentTimeMillis();
        while (System.currentTimeMillis() - initial < 2000 && (
                roster.getPresence(getBareJID(0)).getType() != Presence.Type.unavailable ||
                        getConnection(1).getRoster().getPresence(getBareJID(0)).getType() !=
                                Presence.Type.unavailable)) {
            Thread.sleep(100);
        }

        // Check that no presence is returned for the removed contact
        presence = roster.getPresence(getBareJID(0));
        assertFalse("Available presence was returned for removed contact", presence.isAvailable());
        assertEquals("Returned Presence for removed contact has incorrect type",
                Presence.Type.unavailable, presence.getType());

        // Check that no presence is returned for the removed contact
        presence = getConnection(1).getRoster().getPresence(getBareJID(0));
        assertFalse("Available presence was returned for removed contact", presence.isAvailable());
        assertEquals("Returned Presence for removed contact has incorrect type",
                Presence.Type.unavailable, presence.getType());
    }

    /**
     * Tests that the server and Smack are able to handle nicknames that include
     * < > characters.
     */
    public void testNotCommonNickname() throws Exception {
        // Add a new roster entry
        Roster roster = getConnection(0).getRoster();
        roster.createEntry(getBareJID(1), "Thiago <12001200>", null);

        Thread.sleep(500);

        assertEquals("Created entry was never received", 1, roster.getEntryCount());

        // Create another connection for the same user of connection 0
        ConnectionConfiguration connectionConfiguration =
                new ConnectionConfiguration(getHost(), getPort(), getServiceName());
        TCPConnection conn2 = new XMPPConnection(connectionConfiguration);
        conn2.connect();
        conn2.login(getUsername(0), getPassword(0), "Home");

        // Retrieve roster and verify that new contact is there and nickname is correct
        Roster roster2 = conn2.getRoster();
        assertEquals("Created entry was never received", 1, roster2.getEntryCount());
        RosterEntry entry = roster2.getEntry(getBareJID(1));
        assertNotNull("New entry was not returned from the server", entry);
        assertEquals("Roster item name is incorrect", "Thiago <12001200>", entry.getName());
    }

    /**
     * Clean up all the entries in the roster
     */
    private void cleanUpRoster() {
        for (int i=0; i<getMaxConnections(); i++) {
            // Delete all the entries from the roster
            Roster roster = getConnection(i).getRoster();
            final CountDownLatch removalLatch = new CountDownLatch(roster.getEntryCount());
            roster.addRosterListener(new RosterListener() {
                
                @Override
                public void presenceChanged(Presence presence) {}
                
                @Override
                public void entriesUpdated(Collection<String> addresses) {}
                
                @Override
                public void entriesDeleted(Collection<String> addresses) {
                    removalLatch.countDown();
                }
                
                @Override
                public void entriesAdded(Collection<String> addresses) {}
            });
            
            for (RosterEntry entry : roster.getEntries()) {
                try {
                    roster.removeEntry(entry);
                }
                catch (XMPPException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            try  {
                removalLatch.await(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }

        assertEquals("Wrong number of entries in connection 0", 0, getConnection(0).getRoster().getEntryCount());
        assertEquals("Wrong number of entries in connection 1", 0, getConnection(1).getRoster().getEntryCount());
        assertEquals("Wrong number of entries in connection 2", 0, getConnection(2).getRoster().getEntryCount());
    }

    /**
     * Tests the creation of a roster and then simulates abrupt termination. Cached presences
     * must go offline. At reconnection, presences must go back to online.
     * <ol>
     *     <li> Create some entries
     *     <li> Breack the connection
     *     <li> Check offline presences
     *     <li> Whait for automatic reconnection
     *     <li> Check online presences
     * </ol>
     */
    public void testOfflinePresencesAfterDisconnection() throws Exception {
        // Add a new roster entry
        Roster roster = getConnection(0).getRoster();
        roster.createEntry(getBareJID(1), "gato11", null);
        roster.createEntry(getBareJID(2), "gato12", null);

        // Wait up to 2 seconds to let the server process presence subscriptions
        long initial = System.currentTimeMillis();
        while (System.currentTimeMillis() - initial < 2000 && (
                !roster.getPresence(getBareJID(1)).isAvailable() ||
                        !roster.getPresence(getBareJID(2)).isAvailable())) {
            Thread.sleep(100);
        }

        Thread.sleep(200);

        // Break the connection
        getConnection(0).notifyConnectionError(new Exception("Simulated Error"));

        Presence presence = roster.getPresence(getBareJID(1));
        assertFalse("Unavailable presence not found for offline user", presence.isAvailable());
        assertEquals("Unavailable presence not found for offline user", Presence.Type.unavailable,
                presence.getType());
        // Reconnection should occur in 10 seconds
        Thread.sleep(12200);
        presence = roster.getPresence(getBareJID(1));
        assertTrue("Presence not found for user", presence.isAvailable());
        assertEquals("Presence should be online after a connection reconnection",
                Presence.Type.available, presence.getType());
    }
    
    protected int getMaxConnections() {
        return 3;
    }

    protected void setUp() throws Exception {
        super.setUp();
        cleanUpRoster();
    }


    @Override
    protected void tearDown() throws Exception {
        cleanUpRoster();
        super.tearDown();
    }
    
    private class RemovalListener implements RosterListener {
        private CountDownLatch latch;
        
        private RemovalListener(CountDownLatch removalLatch) {
            latch = removalLatch;
        }
        
        @Override
        public void presenceChanged(Presence presence) {}
        
        @Override
        public void entriesUpdated(Collection<String> addresses) {}
        
        @Override
        public void entriesDeleted(Collection<String> addresses) {                
            latch.countDown();
        }
        
        @Override
        public void entriesAdded(Collection<String> addresses) {}
    }; 

}
