/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2010 Jive Software.
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

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.RosterPacket.Item;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that verifies the correct behavior of the {@see Roster} implementation.
 * 
 * @see Roster
 * @see <a href="http://xmpp.org/rfcs/rfc3921.html#roster">Roster Management</a>
 * @author Guenther Niess
 */
public class RosterTest {

    private DummyConnection connection;
    private TestRosterListener rosterListener;

    @Before
    public void setUp() throws Exception {
        // Uncomment this to enable debug output
        //Connection.DEBUG_ENABLED = true;

        connection = new DummyConnection();
        connection.connect();
        connection.login("rostertest", "secret");
        rosterListener = new TestRosterListener();
        connection.getRoster().addRosterListener(rosterListener);
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            if (rosterListener != null && connection.getRoster() != null) {
                connection.getRoster().removeRosterListener(rosterListener);
                rosterListener = null;
            }
            connection.disconnect();
            connection = null;
        }
    }

    /**
     * Test a simple roster initialization according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-login"
     *     >RFC3921: Retrieving One's Roster on Login</a>.
     */
    @Test(timeout=5000)
    public void testSimpleRosterInitialization() throws Exception {
        // Setup
        final Roster roster = connection.getRoster();
        assertNotNull("Can't get the roster from the provided connection!", roster);
        assertFalse("Roster shouldn't be already initialized!",
                roster.rosterInitialized);

        // Perform roster initialization
        initRoster(connection, roster);

        // Verify roster
        assertTrue("Roster can't be initialized!", roster.rosterInitialized);
        verifyRomeosEntry(roster.getEntry("romeo@example.net"));
        verifyMercutiosEntry(roster.getEntry("mercutio@example.com"));
        verifyBenvoliosEntry(roster.getEntry("benvolio@example.net"));
        assertSame("Wrong number of roster entries.", 3, roster.getEntries().size());

        // Verify roster listener
        assertTrue("The roster listener wasn't invoked for Romeo.",
                rosterListener.getAddedAddresses().contains("romeo@example.net"));
        assertTrue("The roster listener wasn't invoked for Mercutio.",
                rosterListener.getAddedAddresses().contains("mercutio@example.com"));
        assertTrue("The roster listener wasn't invoked for Benvolio.",
                rosterListener.getAddedAddresses().contains("benvolio@example.net"));
        assertSame("RosterListeners implies that a item was deleted!",
                0,
                rosterListener.getDeletedAddresses().size());
        assertSame("RosterListeners implies that a item was updated!",
                0,
                rosterListener.getUpdatedAddresses().size());
    }

    /**
     * Test adding a roster item according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-add"
     *     >RFC3921: Adding a Roster Item</a>.
     */
    @Test(timeout=5000)
    public void testAddRosterItem() throws Throwable {
        // Constants for the new contact
        final String contactJID = "nurse@example.com";
        final String contactName = "Nurse";
        final String[] contactGroup = {"Servants"};

        // Setup
        final Roster roster = connection.getRoster();
        assertNotNull("Can't get the roster from the provided connection!", roster);
        initRoster(connection, roster);
        rosterListener.reset();

        // Adding the new roster item
        final RosterUpdateResponder serverSimulator = new RosterUpdateResponder() {
            void verifyUpdateRequest(final RosterPacket updateRequest) {
                final Item item = updateRequest.getRosterItems().iterator().next();
                assertSame("The provided JID doesn't match the requested!",
                        contactJID,
                        item.getUser());
                assertSame("The provided name doesn't match the requested!",
                        contactName,
                        item.getName());
                assertSame("The provided group number doesn't match the requested!",
                        contactGroup.length,
                        item.getGroupNames().size());
                assertSame("The provided group doesn't match the requested!",
                        contactGroup[0],
                        item.getGroupNames().iterator().next());
            }
        };
        serverSimulator.start();
        roster.createEntry(contactJID, contactName, contactGroup);
        serverSimulator.join();

        // Check if an error occurred within the simulator
        final Throwable exception = serverSimulator.getException();
        if (exception != null) {
            throw exception;
        }

        // Verify the roster entry of the new contact
        final RosterEntry addedEntry = roster.getEntry(contactJID);
        assertNotNull("The new contact wasn't added to the roster!", addedEntry);
        assertTrue("The roster listener wasn't invoked for the new contact!",
                rosterListener.getAddedAddresses().contains(contactJID));
        assertSame("Setup wrong name for the new contact!",
                contactName,
                addedEntry.getName());
        assertSame("Setup wrong default subscription status!",
                ItemType.none,
                addedEntry.getType());
        assertSame("The new contact should be member of exactly one group!",
                1,
                addedEntry.getGroups().size());
        assertSame("Setup wrong group name for the added contact!",
                contactGroup[0],
                addedEntry.getGroups().iterator().next().getName());

        // Verify the unchanged roster items
        verifyRomeosEntry(roster.getEntry("romeo@example.net"));
        verifyMercutiosEntry(roster.getEntry("mercutio@example.com"));
        verifyBenvoliosEntry(roster.getEntry("benvolio@example.net"));
        assertSame("Wrong number of roster entries.", 4, roster.getEntries().size());
    }

    /**
     * Test updating a roster item according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-update"
     *     >RFC3921: Updating a Roster Item</a>.
     */
    @Test(timeout=5000)
    public void testUpdateRosterItem() throws Throwable {
        // Constants for the updated contact
        final String contactJID = "romeo@example.net";
        final String contactName = "Romeo";
        final String[] contactGroups = {"Friends", "Lovers"};

        // Setup
        final Roster roster = connection.getRoster();
        assertNotNull("Can't get the roster from the provided connection!", roster);
        initRoster(connection, roster);
        rosterListener.reset();

        // Updating the roster item
        final RosterUpdateResponder serverSimulator = new RosterUpdateResponder() {
            void verifyUpdateRequest(final RosterPacket updateRequest) {
                final Item item = updateRequest.getRosterItems().iterator().next();
                assertSame("The provided JID doesn't match the requested!",
                        contactJID,
                        item.getUser());
                assertSame("The provided name doesn't match the requested!",
                        contactName,
                        item.getName());
                assertTrue("The updated contact doesn't belong to the requested groups ("
                        + contactGroups[0] +")!",
                        item.getGroupNames().contains(contactGroups[0]));
                assertTrue("The updated contact doesn't belong to the requested groups ("
                        + contactGroups[1] +")!",
                        item.getGroupNames().contains(contactGroups[1]));
                assertSame("The provided group number doesn't match the requested!",
                        contactGroups.length,
                        item.getGroupNames().size());
            }
        };
        serverSimulator.start();
        roster.createGroup(contactGroups[1]).addEntry(roster.getEntry(contactJID));
        serverSimulator.join();

        // Check if an error occurred within the simulator
        final Throwable exception = serverSimulator.getException();
        if (exception != null) {
            throw exception;
        }

        // Verify the roster entry of the updated contact
        final RosterEntry addedEntry = roster.getEntry(contactJID);
        assertNotNull("The contact was deleted from the roster!", addedEntry);
        assertTrue("The roster listener wasn't invoked for the updated contact!",
                rosterListener.getUpdatedAddresses().contains(contactJID));
        assertSame("Setup wrong name for the changed contact!",
                contactName,
                addedEntry.getName());
        assertTrue("The updated contact doesn't belong to the requested groups ("
                + contactGroups[0] +")!",
                roster.getGroup(contactGroups[0]).contains(addedEntry));
        assertTrue("The updated contact doesn't belong to the requested groups ("
                + contactGroups[1] +")!",
                roster.getGroup(contactGroups[1]).contains(addedEntry));
        assertSame("The updated contact should be member of two groups!",
                contactGroups.length,
                addedEntry.getGroups().size());

        // Verify the unchanged roster items
        verifyMercutiosEntry(roster.getEntry("mercutio@example.com"));
        verifyBenvoliosEntry(roster.getEntry("benvolio@example.net"));
        assertSame("Wrong number of roster entries (" + roster.getEntries() + ").",
                3,
                roster.getEntries().size());
    }

    /**
     * Test deleting a roster item according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-delete"
     *     >RFC3921: Deleting a Roster Item</a>.
     */
    @Test(timeout=5000)
    public void testDeleteRosterItem() throws Throwable {
        // The contact which should be deleted
        final String contactJID = "romeo@example.net";

        // Setup
        final Roster roster = connection.getRoster();
        assertNotNull("Can't get the roster from the provided connection!", roster);
        initRoster(connection, roster);
        rosterListener.reset();

        // Delete a roster item
        final RosterUpdateResponder serverSimulator = new RosterUpdateResponder() {
            void verifyUpdateRequest(final RosterPacket updateRequest) {
                final Item item = updateRequest.getRosterItems().iterator().next();
                assertSame("The provided JID doesn't match the requested!",
                        contactJID,
                        item.getUser());
            }
        };
        serverSimulator.start();
        roster.removeEntry(roster.getEntry(contactJID));
        serverSimulator.join();

        // Check if an error occurred within the simulator
        final Throwable exception = serverSimulator.getException();
        if (exception != null) {
            throw exception;
        }

        // Verify
        final RosterEntry deletedEntry = roster.getEntry(contactJID);
        assertNull("The contact wasn't deleted from the roster!", deletedEntry);
        assertTrue("The roster listener wasn't invoked for the deleted contact!",
                rosterListener.getDeletedAddresses().contains(contactJID));
        verifyMercutiosEntry(roster.getEntry("mercutio@example.com"));
        verifyBenvoliosEntry(roster.getEntry("benvolio@example.net"));
        assertSame("Wrong number of roster entries (" + roster.getEntries() + ").",
                2,
                roster.getEntries().size());
    }

    /**
     * Remove all roster entries by iterating trough {@see Roster#getEntries()}
     * and simulating receiving roster pushes from the server.
     * 
     * @param connection the dummy connection of which the provided roster belongs to.
     * @param roster the roster (or buddy list) which should be initialized.
     */
    public static void removeAllRosterEntries(DummyConnection connection, Roster roster)
            throws InterruptedException, XMPPException {
        for(RosterEntry entry : roster.getEntries()) {
            // prepare the roster push packet
            final RosterPacket rosterPush= new RosterPacket();
            rosterPush.setType(Type.SET);
            rosterPush.setTo(connection.getUser());

            // prepare the buddy's item entry which should be removed
            final RosterPacket.Item item = new RosterPacket.Item(entry.getUser(), entry.getName());
            item.setItemType(ItemType.remove);
            rosterPush.addRosterItem(item);

            // simulate receiving the roster push
            connection.processPacket(rosterPush);
        }
    }

    /**
     * Initialize the roster according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-login"
     *     >RFC3921: Retrieving One's Roster on Login</a>.
     * 
     * @param connection the dummy connection of which the provided roster belongs to.
     * @param roster the roster (or buddy list) which should be initialized.
     */
    public static void initRoster(DummyConnection connection, Roster roster) throws InterruptedException, XMPPException {
        roster.reload();
        while (true) {
            final Packet sentPacket = connection.getSentPacket();
            if (sentPacket instanceof RosterPacket && ((IQ) sentPacket).getType() == Type.GET) {
                // setup the roster get request
                final RosterPacket rosterRequest = (RosterPacket) sentPacket;
                assertSame("The <query/> element MUST NOT contain any <item/> child elements!",
                        0,
                        rosterRequest.getRosterItemCount());

                // prepare the roster result
                final RosterPacket rosterResult = new RosterPacket();
                rosterResult.setTo(connection.getUser());
                rosterResult.setType(Type.RESULT);
                rosterResult.setPacketID(rosterRequest.getPacketID());

                // prepare romeo's roster entry
                final Item romeo = new Item("romeo@example.net", "Romeo");
                romeo.addGroupName("Friends");
                romeo.setItemType(ItemType.both);
                rosterResult.addRosterItem(romeo);

                // prepare mercutio's roster entry
                final Item mercutio = new Item("mercutio@example.com", "Mercutio");
                mercutio.setItemType(ItemType.from);
                rosterResult.addRosterItem(mercutio);

                // prepare benvolio's roster entry
                final Item benvolio = new Item("benvolio@example.net", "Benvolio");
                benvolio.setItemType(ItemType.both);
                rosterResult.addRosterItem(benvolio);

                // simulate receiving the roster result and exit the loop
                connection.processPacket(rosterResult);
                break;
            }
        };
    }

    /**
     * Check Romeo's roster entry according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-login"
     *     >RFC3921: Retrieving One's Roster on Login</a>.
     * 
     * @param romeo the roster entry which should be verified.
     */
    public static void verifyRomeosEntry(final RosterEntry romeo) {
        assertNotNull("Can't get Romeo's roster entry!", romeo);
        assertSame("Setup wrong name for Romeo!",
                "Romeo",
                romeo.getName());
        assertSame("Setup wrong subscription status for Romeo!",
                ItemType.both,
                romeo.getType());
        assertSame("Romeo should be member of exactly one group!",
                1,
                romeo.getGroups().size());
        assertSame("Setup wrong group name for Romeo!",
                "Friends",
                romeo.getGroups().iterator().next().getName());
    }

    /**
     * Check Mercutio's roster entry according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-login"
     *     >RFC3921: Retrieving One's Roster on Login</a>.
     *  
     * @param mercutio the roster entry which should be verified.
     */
    public static void verifyMercutiosEntry(final RosterEntry mercutio) {
        assertNotNull("Can't get Mercutio's roster entry!", mercutio);
        assertSame("Setup wrong name for Mercutio!",
                "Mercutio",
                mercutio.getName());
        assertSame("Setup wrong subscription status for Mercutio!",
                ItemType.from,
                mercutio.getType());
        assertTrue("Mercutio shouldn't be a member of any group!",
                mercutio.getGroups().isEmpty());
    }

    /**
     * Check Benvolio's roster entry according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-login"
     *     >RFC3921: Retrieving One's Roster on Login</a>.
     * 
     * @param benvolio the roster entry which should be verified.
     */
    public static void verifyBenvoliosEntry(final RosterEntry benvolio) {
        assertNotNull("Can't get Benvolio's roster entry!", benvolio);
        assertSame("Setup wrong name for Benvolio!",
                "Benvolio",
                benvolio.getName());
        assertSame("Setup wrong subscription status for Benvolio!",
                ItemType.both,
                benvolio.getType());
        assertTrue("Benvolio shouldn't be a member of any group!",
                benvolio.getGroups().isEmpty());
    }


    /**
     * This class can be used to simulate the server response for
     * a roster update request.
     */
    private abstract class RosterUpdateResponder extends Thread {
        private Throwable exception = null;

        /**
         * Overwrite this method to check if the received update request is valid.
         * 
         * @param updateRequest the request which would be sent to the server.
         */
        abstract void verifyUpdateRequest(final RosterPacket updateRequest);

        public void run() {
            try {
                while (true) {
                    final Packet packet = connection.getSentPacket();
                    if (packet instanceof RosterPacket && ((IQ) packet).getType() == Type.SET) {
                        final RosterPacket rosterRequest = (RosterPacket) packet;

                        // Prepare and process the roster push
                        final RosterPacket rosterPush = new RosterPacket();
                        final Item item = rosterRequest.getRosterItems().iterator().next();
                        if (item.getItemType() != ItemType.remove) {
                            item.setItemType(ItemType.none);
                        }
                        rosterPush.setType(Type.SET);
                        rosterPush.setTo(connection.getUser());
                        rosterPush.addRosterItem(item);
                        connection.processPacket(rosterPush);

                        // Create and process the IQ response
                        final IQ response = new IQ() {
                            public String getChildElementXML() {
                                return null;
                            }
                        };
                        response.setPacketID(rosterRequest.getPacketID());
                        response.setType(Type.RESULT);
                        response.setTo(connection.getUser());
                        connection.processPacket(response);

                        // Verify the roster update request
                        assertSame("A roster set MUST contain one and only one <item/> element.",
                                1,
                                rosterRequest.getRosterItemCount());
                        verifyUpdateRequest(rosterRequest);
                        break;
                    }
                }
            }
            catch (Throwable e) {
                exception = e;
                fail(e.getMessage());
            }
        }

        /**
         * Returns the exception or error if something went wrong.
         * 
         * @return the Throwable exception or error that occurred.
         */
        public Throwable getException() {
            return exception;
        }
    }


    /**
     * This class can be used to check if the RosterListener was invoked.
     */
    public static class TestRosterListener implements RosterListener {
        private CopyOnWriteArrayList<String> addressesAdded = new CopyOnWriteArrayList<String>();
        private CopyOnWriteArrayList<String> addressesDeleted = new CopyOnWriteArrayList<String>();
        private CopyOnWriteArrayList<String> addressesUpdated = new CopyOnWriteArrayList<String>();

        public synchronized void entriesAdded(Collection<String> addresses) {
            addressesAdded.addAll(addresses);
            if (Connection.DEBUG_ENABLED) {
                for (String address : addresses) {
                    System.out.println("Roster entry for " + address + " added.");
                }
            }
        }

        public synchronized void entriesDeleted(Collection<String> addresses) {
            addressesDeleted.addAll(addresses);
            if (Connection.DEBUG_ENABLED) {
                for (String address : addresses) {
                    System.out.println("Roster entry for " + address + " deleted.");
                }
            }
        }

        public synchronized void entriesUpdated(Collection<String> addresses) {
            addressesUpdated.addAll(addresses);
            if (Connection.DEBUG_ENABLED) {
                for (String address : addresses) {
                    System.out.println("Roster entry for " + address + " updated.");
                }
            }
        }

        public void presenceChanged(Presence presence) {
            if (Connection.DEBUG_ENABLED) {
                System.out.println("Roster presence changed: " + presence.toXML());
            }
        }

        /**
         * Get a collection of JIDs of the added roster items.
         * 
         * @return the collection of addresses which were added.
         */
        public Collection<String> getAddedAddresses() {
            return Collections.unmodifiableCollection(addressesAdded);
        }

        /**
         * Get a collection of JIDs of the deleted roster items.
         * 
         * @return the collection of addresses which were deleted.
         */
        public Collection<String> getDeletedAddresses() {
            return Collections.unmodifiableCollection(addressesDeleted);
        }

        /**
         * Get a collection of JIDs of the updated roster items.
         * 
         * @return the collection of addresses which were updated.
         */
        public Collection<String> getUpdatedAddresses() {
            return Collections.unmodifiableCollection(addressesUpdated);
        }

        /**
         * Reset the lists of added, deleted or updated items.
         */
        public synchronized void reset() {
            addressesAdded.clear();
            addressesDeleted.clear();
            addressesUpdated.clear();
        }
    }
}
