/**
 *
 * Copyright 2010 Jive Software.
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

package org.jivesoftware.smack.roster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.im.InitSmackIm;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.packet.RosterPacket.Item;
import org.jivesoftware.smack.roster.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.test.util.WaitForPacketListener;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xmlpull.v1.XmlPullParser;

/**
 * Tests that verifies the correct behavior of the {@link Roster} implementation.
 * 
 * @see Roster
 * @see <a href="http://xmpp.org/rfcs/rfc3921.html#roster">Roster Management</a>
 * @author Guenther Niess
 */
public class RosterTest extends InitSmackIm {

    private DummyConnection connection;
    private Roster roster;
    private TestRosterListener rosterListener;

    @Before
    public void setUp() throws Exception {
        connection = new DummyConnection();
        connection.connect();
        connection.login();
        rosterListener = new TestRosterListener();
        roster = Roster.getInstanceFor(connection);
        roster.addRosterListener(rosterListener);
        connection.setReplyTimeout(1000 * 60 * 5);
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            if (rosterListener != null) {
                roster.removeRosterListener(rosterListener);
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
    @Test
    public void testSimpleRosterInitialization() throws Exception {
        assertNotNull("Can't get the roster from the provided connection!", roster);
        assertFalse("Roster shouldn't be already loaded!",
                roster.isLoaded());

        // Perform roster initialization
        initRoster();

        // Verify roster
        assertTrue("Roster can't be loaded!", roster.waitUntilLoaded());
        verifyRomeosEntry(roster.getEntry(JidCreate.entityBareFrom("romeo@example.net")));
        verifyMercutiosEntry(roster.getEntry(JidCreate.entityBareFrom("mercutio@example.com")));
        verifyBenvoliosEntry(roster.getEntry(JidCreate.entityBareFrom("benvolio@example.net")));
        assertSame("Wrong number of roster entries.", 3, roster.getEntries().size());

        // Verify roster listener
        assertTrue("The roster listener wasn't invoked for Romeo.",
                rosterListener.addedAddressesContains("romeo@example.net"));
        assertTrue("The roster listener wasn't invoked for Mercutio.",
                rosterListener.addedAddressesContains("mercutio@example.com"));
        assertTrue("The roster listener wasn't invoked for Benvolio.",
                rosterListener.addedAddressesContains("benvolio@example.net"));
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
    @Test
    public void testAddRosterItem() throws Throwable {
        // Constants for the new contact
        final BareJid contactJID = JidCreate.entityBareFrom("nurse@example.com");
        final String contactName = "Nurse";
        final String[] contactGroup = {"Servants"};

        // Setup
        assertNotNull("Can't get the roster from the provided connection!", roster);
        initRoster();
        rosterListener.reset();

        // Adding the new roster item
        final RosterUpdateResponder serverSimulator = new RosterUpdateResponder() {
            @Override
            void verifyUpdateRequest(final RosterPacket updateRequest) {
                final Item item = updateRequest.getRosterItems().iterator().next();
                assertEquals("The provided JID doesn't match the requested!",
                        contactJID,
                        item.getJid());
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
        rosterListener.waitUntilInvocationOrTimeout();

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
        verifyRomeosEntry(roster.getEntry(JidCreate.entityBareFrom("romeo@example.net")));
        verifyMercutiosEntry(roster.getEntry(JidCreate.entityBareFrom("mercutio@example.com")));
        verifyBenvoliosEntry(roster.getEntry(JidCreate.entityBareFrom("benvolio@example.net")));
        assertSame("Wrong number of roster entries.", 4, roster.getEntries().size());
    }

    /**
     * Test updating a roster item according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-update"
     *     >RFC3921: Updating a Roster Item</a>.
     */
    @Test
    public void testUpdateRosterItem() throws Throwable {
        // Constants for the updated contact
        final BareJid contactJID = JidCreate.entityBareFrom("romeo@example.net");
        final String contactName = "Romeo";
        final String[] contactGroups = {"Friends", "Lovers"};

        // Setup
        assertNotNull("Can't get the roster from the provided connection!", roster);
        initRoster();
        rosterListener.reset();

        // Updating the roster item
        final RosterUpdateResponder serverSimulator = new RosterUpdateResponder() {
            @Override
            void verifyUpdateRequest(final RosterPacket updateRequest) {
                final Item item = updateRequest.getRosterItems().iterator().next();
                assertEquals("The provided JID doesn't match the requested!",
                        contactJID,
                        item.getJid());
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
        rosterListener.waitUntilInvocationOrTimeout();

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
        verifyMercutiosEntry(roster.getEntry(JidCreate.entityBareFrom("mercutio@example.com")));
        verifyBenvoliosEntry(roster.getEntry(JidCreate.entityBareFrom("benvolio@example.net")));
        assertSame("Wrong number of roster entries (" + roster.getEntries() + ").",
                3,
                roster.getEntries().size());
    }

    /**
     * Test deleting a roster item according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-delete"
     *     >RFC3921: Deleting a Roster Item</a>.
     */
    @Test
    public void testDeleteRosterItem() throws Throwable {
        // The contact which should be deleted
        final BareJid contactJID = JidCreate.entityBareFrom("romeo@example.net");

        // Setup
        assertNotNull("Can't get the roster from the provided connection!", roster);
        initRoster();
        rosterListener.reset();

        // Delete a roster item
        final RosterUpdateResponder serverSimulator = new RosterUpdateResponder() {
            @Override
            void verifyUpdateRequest(final RosterPacket updateRequest) {
                final Item item = updateRequest.getRosterItems().iterator().next();
                assertEquals("The provided JID doesn't match the requested!",
                        contactJID,
                        item.getJid());
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
        rosterListener.waitUntilInvocationOrTimeout();

        // Verify
        final RosterEntry deletedEntry = roster.getEntry(contactJID);
        assertNull("The contact wasn't deleted from the roster!", deletedEntry);
        assertTrue("The roster listener wasn't invoked for the deleted contact!",
                rosterListener.getDeletedAddresses().contains(contactJID));
        verifyMercutiosEntry(roster.getEntry(JidCreate.entityBareFrom("mercutio@example.com")));
        verifyBenvoliosEntry(roster.getEntry(JidCreate.entityBareFrom("benvolio@example.net")));
        assertSame("Wrong number of roster entries (" + roster.getEntries() + ").",
                2,
                roster.getEntries().size());
    }

    /**
     * Test a simple roster push according to the example in
     * <a href="http://xmpp.org/internet-drafts/draft-ietf-xmpp-3921bis-03.html#roster-syntax-actions-push"
     *     >RFC3921bis-03: Roster Push</a>.
     */
    @Test
    public void testSimpleRosterPush() throws Throwable {
        final BareJid contactJID = JidCreate.entityBareFrom("nurse@example.com");
        assertNotNull("Can't get the roster from the provided connection!", roster);
        final StringBuilder sb = new StringBuilder();
        sb.append("<iq id=\"rostertest1\" type=\"set\" ")
                .append("to=\"").append(connection.getUser()).append("\">")
                .append("<query xmlns=\"jabber:iq:roster\">")
                .append("<item jid=\"").append(contactJID).append("\"/>")
                .append("</query>")
                .append("</iq>");
        final XmlPullParser parser = TestUtils.getIQParser(sb.toString());
        final IQ rosterPush = PacketParserUtils.parseIQ(parser);
        initRoster();
        rosterListener.reset();

        // Simulate receiving the roster push
        connection.processStanza(rosterPush);
        rosterListener.waitUntilInvocationOrTimeout();

        // Verify the roster entry of the new contact
        final RosterEntry addedEntry = roster.getEntry(contactJID);
        assertNotNull("The new contact wasn't added to the roster!", addedEntry);
        assertTrue("The roster listener wasn't invoked for the new contact!",
                rosterListener.getAddedAddresses().contains(contactJID));
        assertSame("Setup wrong default subscription status!",
                ItemType.none,
                addedEntry.getType());
        assertSame("The new contact shouldn't be member of any group!",
                0,
                addedEntry.getGroups().size());

        // Verify the unchanged roster items
        verifyRomeosEntry(roster.getEntry(JidCreate.entityBareFrom("romeo@example.net")));
        verifyMercutiosEntry(roster.getEntry(JidCreate.entityBareFrom("mercutio@example.com")));
        verifyBenvoliosEntry(roster.getEntry(JidCreate.entityBareFrom("benvolio@example.net")));
        assertSame("Wrong number of roster entries.", 4, roster.getEntries().size());
    }

    /**
     * Tests that roster pushes with invalid from are ignored.
     * @throws XmppStringprepException 
     *
     * @see <a href="http://xmpp.org/rfcs/rfc6121.html#roster-syntax-actions-push">RFC 6121, Section 2.1.6</a>
     */
    @Test
    public void testIgnoreInvalidFrom() throws XmppStringprepException {
        final BareJid spammerJid = JidCreate.entityBareFrom("spam@example.com");
        RosterPacket packet = new RosterPacket();
        packet.setType(Type.set);
        packet.setTo(connection.getUser());
        packet.setFrom(JidCreate.entityBareFrom("mallory@example.com"));
        packet.addRosterItem(new Item(spammerJid, "Cool products!"));

        final String requestId = packet.getStanzaId();
        // Simulate receiving the roster push
        connection.processStanza(packet);

        // Smack should reply with an error IQ
        ErrorIQ errorIQ = (ErrorIQ) connection.getSentPacket();
        assertEquals(requestId, errorIQ.getStanzaId());
        assertEquals(Condition.service_unavailable, errorIQ.getError().getCondition());

        assertNull("Contact was added to roster", Roster.getInstanceFor(connection).getEntry(spammerJid));
    }

    /**
     * Test if adding an user with an empty group is equivalent with providing
     * no group.
     * 
     * @see <a href="http://www.igniterealtime.org/issues/browse/SMACK-294">SMACK-294</a>
     */
    @Test(timeout=5000)
    public void testAddEmptyGroupEntry() throws Throwable {
        // Constants for the new contact
        final BareJid contactJID = JidCreate.entityBareFrom("nurse@example.com");
        final String contactName = "Nurse";
        final String[] contactGroup = {""};

        // Setup
        assertNotNull("Can't get the roster from the provided connection!", roster);
        initRoster();
        rosterListener.reset();

        // Adding the new roster item
        final RosterUpdateResponder serverSimulator = new RosterUpdateResponder() {
            @Override
            void verifyUpdateRequest(final RosterPacket updateRequest) {
                final Item item = updateRequest.getRosterItems().iterator().next();
                assertSame("The provided JID doesn't match the requested!",
                        contactJID,
                        item.getJid());
                assertSame("The provided name doesn't match the requested!",
                        contactName,
                        item.getName());
                assertSame("Shouldn't provide an empty group element!",
                        0,
                        item.getGroupNames().size());

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
        rosterListener.waitUntilInvocationOrTimeout();

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
        assertSame("The new contact shouldn't be member of any group!",
                0,
                addedEntry.getGroups().size());

        // Verify the unchanged roster items
        verifyRomeosEntry(roster.getEntry(JidCreate.entityBareFrom("romeo@example.net")));
        verifyMercutiosEntry(roster.getEntry(JidCreate.entityBareFrom("mercutio@example.com")));
        verifyBenvoliosEntry(roster.getEntry(JidCreate.entityBareFrom("benvolio@example.net")));
        assertSame("Wrong number of roster entries.", 4, roster.getEntries().size());
    }

    /**
     * Test processing a roster push with an empty group is equivalent with providing
     * no group.
     * 
     * @see <a href="http://www.igniterealtime.org/issues/browse/SMACK-294">SMACK-294</a>
     */
    @Test
    public void testEmptyGroupRosterPush() throws Throwable {
        final BareJid contactJID = JidCreate.entityBareFrom("nurse@example.com");
        assertNotNull("Can't get the roster from the provided connection!", roster);
        final StringBuilder sb = new StringBuilder();
        sb.append("<iq id=\"rostertest2\" type=\"set\" ")
                .append("to=\"").append(connection.getUser()).append("\">")
                .append("<query xmlns=\"jabber:iq:roster\">")
                .append("<item jid=\"").append(contactJID).append("\">")
                .append("<group></group>")
                .append("</item>")
                .append("</query>")
                .append("</iq>");
        final XmlPullParser parser = TestUtils.getIQParser(sb.toString());
        final IQ rosterPush = PacketParserUtils.parseIQ(parser);
        initRoster();
        rosterListener.reset();

        // Simulate receiving the roster push
        connection.processStanza(rosterPush);
        rosterListener.waitUntilInvocationOrTimeout();

        // Verify the roster entry of the new contact
        final RosterEntry addedEntry = roster.getEntry(contactJID);
        assertNotNull("The new contact wasn't added to the roster!", addedEntry);
        assertTrue("The roster listener wasn't invoked for the new contact!",
                rosterListener.getAddedAddresses().contains(contactJID));
        assertSame("Setup wrong default subscription status!",
                ItemType.none,
                addedEntry.getType());
        assertSame("The new contact shouldn't be member of any group!",
                0,
                addedEntry.getGroups().size());

        // Verify the unchanged roster items
        verifyRomeosEntry(roster.getEntry(JidCreate.entityBareFrom("romeo@example.net")));
        verifyMercutiosEntry(roster.getEntry(JidCreate.entityBareFrom("mercutio@example.com")));
        verifyBenvoliosEntry(roster.getEntry(JidCreate.entityBareFrom("benvolio@example.net")));
        assertSame("Wrong number of roster entries.", 4, roster.getEntries().size());
    }

    /**
     * Remove all roster entries by iterating trough {@link Roster#getEntries()}
     * and simulating receiving roster pushes from the server.
     * 
     * @param connection the dummy connection of which the provided roster belongs to.
     * @param roster the roster (or buddy list) which should be initialized.
     */
    public static void removeAllRosterEntries(DummyConnection connection, Roster roster) {
        for(RosterEntry entry : roster.getEntries()) {
            // prepare the roster push packet
            final RosterPacket rosterPush= new RosterPacket();
            rosterPush.setType(Type.set);
            rosterPush.setTo(connection.getUser());

            // prepare the buddy's item entry which should be removed
            final RosterPacket.Item item = new RosterPacket.Item(entry.getJid(), entry.getName());
            item.setItemType(ItemType.remove);
            rosterPush.addRosterItem(item);

            // simulate receiving the roster push
            connection.processStanza(rosterPush);
        }
    }

    /**
     * Initialize the roster according to the example in
     * <a href="http://xmpp.org/rfcs/rfc3921.html#roster-login"
     *     >RFC3921: Retrieving One's Roster on Login</a>.
     * 
     * @param connection the dummy connection of which the provided roster belongs to.
     * @param roster the roster (or buddy list) which should be initialized.
     * @throws SmackException 
     * @throws XmppStringprepException 
     */
    private void initRoster() throws InterruptedException, SmackException, XmppStringprepException {
        roster.reload();
        while (true) {
            final Stanza sentPacket = connection.getSentPacket();
            if (sentPacket instanceof RosterPacket && ((IQ) sentPacket).getType() == Type.get) {
                // setup the roster get request
                final RosterPacket rosterRequest = (RosterPacket) sentPacket;
                assertSame("The <query/> element MUST NOT contain any <item/> child elements!",
                        0,
                        rosterRequest.getRosterItemCount());

                // prepare the roster result
                final RosterPacket rosterResult = new RosterPacket();
                rosterResult.setTo(connection.getUser());
                rosterResult.setType(Type.result);
                rosterResult.setStanzaId(rosterRequest.getStanzaId());

                // prepare romeo's roster entry
                final Item romeo = new Item(JidCreate.entityBareFrom("romeo@example.net"), "Romeo");
                romeo.addGroupName("Friends");
                romeo.setItemType(ItemType.both);
                rosterResult.addRosterItem(romeo);

                // prepare mercutio's roster entry
                final Item mercutio = new Item(JidCreate.entityBareFrom("mercutio@example.com"), "Mercutio");
                mercutio.setItemType(ItemType.from);
                rosterResult.addRosterItem(mercutio);

                // prepare benvolio's roster entry
                final Item benvolio = new Item(JidCreate.entityBareFrom("benvolio@example.net"), "Benvolio");
                benvolio.setItemType(ItemType.both);
                rosterResult.addRosterItem(benvolio);

                // simulate receiving the roster result and exit the loop
                connection.processStanza(rosterResult);
                break;
            }
        }
        roster.waitUntilLoaded();
        rosterListener.waitUntilInvocationOrTimeout();
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

        @Override
        public void run() {
            try {
                while (true) {
                    final Stanza packet = connection.getSentPacket();
                    if (packet instanceof RosterPacket && ((IQ) packet).getType() == Type.set) {
                        final RosterPacket rosterRequest = (RosterPacket) packet;

                        // Prepare and process the roster push
                        final RosterPacket rosterPush = new RosterPacket();
                        final Item item = rosterRequest.getRosterItems().iterator().next();
                        if (item.getItemType() != ItemType.remove) {
                            item.setItemType(ItemType.none);
                        }
                        rosterPush.setType(Type.set);
                        rosterPush.setTo(connection.getUser());
                        rosterPush.addRosterItem(item);
                        connection.processStanza(rosterPush);

                        // Create and process the IQ response
                        final IQ response = IQ.createResultIQ(rosterRequest);
                        connection.processStanza(response);

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
    public static class TestRosterListener extends WaitForPacketListener implements RosterListener {
        private final List<Jid> addressesAdded = new CopyOnWriteArrayList<>();
        private final List<Jid> addressesDeleted = new CopyOnWriteArrayList<>();
        private final List<Jid> addressesUpdated = new CopyOnWriteArrayList<>();

        @Override
        public synchronized void entriesAdded(Collection<Jid> addresses) {
            addressesAdded.addAll(addresses);
            reportInvoked();
        }

        @Override
        public synchronized void entriesDeleted(Collection<Jid> addresses) {
            addressesDeleted.addAll(addresses);
            reportInvoked();
        }

        @Override
        public synchronized void entriesUpdated(Collection<Jid> addresses) {
            addressesUpdated.addAll(addresses);
            reportInvoked();
        }

        @Override
        public void presenceChanged(Presence presence) {
            reportInvoked();
        }

        /**
         * Get a collection of JIDs of the added roster items.
         * 
         * @return the collection of addresses which were added.
         */
        public Collection<Jid> getAddedAddresses() {
            return Collections.unmodifiableCollection(addressesAdded);
        }

        /**
         * Get a collection of JIDs of the deleted roster items.
         * 
         * @return the collection of addresses which were deleted.
         */
        public Collection<Jid> getDeletedAddresses() {
            return Collections.unmodifiableCollection(addressesDeleted);
        }

        /**
         * Get a collection of JIDs of the updated roster items.
         * 
         * @return the collection of addresses which were updated.
         */
        public Collection<Jid> getUpdatedAddresses() {
            return Collections.unmodifiableCollection(addressesUpdated);
        }

        public boolean addedAddressesContains(String jidString) {
            Jid jid;
            try {
                jid = JidCreate.from(jidString);
            }
            catch (XmppStringprepException e) {
                throw new IllegalArgumentException(e);
            }
            return addressesAdded.contains(jid);
        }

        public boolean deletedAddressesContains(String jidString) {
            Jid jid;
            try {
                jid = JidCreate.from(jidString);
            }
            catch (XmppStringprepException e) {
                throw new IllegalArgumentException(e);
            }
            return addressesDeleted.contains(jid);
        }

        public boolean updatedAddressesContains(String jidString) {
            Jid jid;
            try {
                jid = JidCreate.from(jidString);
            }
            catch (XmppStringprepException e) {
                throw new IllegalArgumentException(e);
            }
            return addressesUpdated.contains(jid);
        }

        /**
         * Reset the lists of added, deleted or updated items.
         */
        @Override
        public synchronized void reset() {
            super.reset();
            addressesAdded.clear();
            addressesDeleted.clear();
            addressesUpdated.clear();
        }
    }
}
