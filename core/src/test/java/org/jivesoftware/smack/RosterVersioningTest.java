/**
 *
 * Copyright the original author or authors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.RosterPacket.Item;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that verify the correct behavior of the {@link Roster} implementation
 * with regard to roster versioning
 *
 * @see Roster
 * @see <a href="http://xmpp.org/rfcs/rfc6121.html#roster">Managing the Roster</a>
 * @author Fabian Schuetz
 * @author Lars Noschinski
 */
public class RosterVersioningTest {

    private DummyConnection connection;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        // Uncomment this to enable debug output
        //XMPPConnection.DEBUG_ENABLED = true;

        DefaultRosterStore store = DefaultRosterStore.init(tmpFolder.newFolder("store"));
        populateStore(store);

        ConnectionConfiguration conf = new ConnectionConfiguration("dummy");
        conf.setRosterStore(store);
        connection = new DummyConnection(conf);
        connection.connect();

        connection.setRosterVersioningSupported();

        connection.login("rostertest", "secret");
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    /**
     * Tests that receiving an empty roster result causes the roster to be populated
     * by all entries of the roster store.
     * @throws SmackException 
     * @throws XMPPException 
     */
    @Test(timeout = 5000)
    public void testEqualVersionStored() throws InterruptedException, IOException, XMPPException, SmackException {
        connection.getRoster().reload();
        answerWithEmptyRosterResult();

        Roster roster = connection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        assertSame("Size of the roster", 3, entries.size());

        HashSet<Item> items = new HashSet<Item>();
        for (RosterEntry entry : entries) {
            items.add(RosterEntry.toRosterItem(entry));
        }
        RosterStore store = DefaultRosterStore.init(tmpFolder.newFolder());
        populateStore(store);
        assertEquals("Elements of the roster", new HashSet<Item>(store.getEntries()), items);

        for (RosterEntry entry : entries) {
            assertTrue("joe stevens".equals(entry.getName()) || "geoff hurley".equals(entry.getName())
                    || "higgins mcmann".equals(entry.getName()));
        }
        Collection<RosterGroup> groups = roster.getGroups();
        assertSame(3, groups.size());

        for (RosterGroup group : groups) {
            assertTrue("all".equals(group.getName()) || "friends".equals(group.getName())
                    || "partners".equals(group.getName()));
        }
    }

    /**
     * Tests that a non-empty roster result empties the store.
     * @throws SmackException 
     * @throws XMPPException 
     */
    @Test(timeout = 5000)
    public void testOtherVersionStored() throws InterruptedException, XMPPException, SmackException {
        connection.getRoster().reload();

        Item vaglafItem = vaglafItem();

        // We expect that the roster request is the only packet sent. This is not part of the specification,
        // but a shortcut in the test implementation.
        Packet sentPacket = connection.getSentPacket();
        if (sentPacket instanceof RosterPacket) {
            RosterPacket sentRP = (RosterPacket)sentPacket;
            RosterPacket answer = new RosterPacket();
            answer.setPacketID(sentRP.getPacketID());
            answer.setType(Type.RESULT);
            answer.setTo(sentRP.getFrom());

            answer.setVersion("newVersion");
            answer.addRosterItem(vaglafItem);

            connection.processPacket(answer);
        } else {
            assertTrue("Expected to get a RosterPacket ", false);
        }

        Roster roster = connection.getRoster();
        assertEquals("Size of roster", 1, roster.getEntries().size());
        RosterEntry entry = roster.getEntry(vaglafItem.getUser());
        assertNotNull("Roster contains vaglaf entry", entry);
        assertEquals("vaglaf entry in roster equals the sent entry", vaglafItem, RosterEntry.toRosterItem(entry));

        RosterStore store = connection.getConfiguration().getRosterStore();
        assertEquals("Size of store", 1, store.getEntries().size());
        Item item = store.getEntry(vaglafItem.getUser());
        assertNotNull("Store contains vaglaf entry");
        assertEquals("vaglaf entry in store equals the sent entry", vaglafItem, item);
    }

    /**
     * Test roster versioning with roster pushes
     */
    @Test(timeout = 5000)
    public void testRosterVersioningWithCachedRosterAndPushes() throws Throwable {
        connection.getRoster().reload();
        answerWithEmptyRosterResult();

        RosterStore store = connection.getConfiguration().getRosterStore();
        Roster roster = connection.getRoster();

        // Simulate a roster push adding vaglaf
        {
            RosterPacket rosterPush = new RosterPacket();
            rosterPush.setTo("rostertest@example.com/home");
            rosterPush.setType(Type.SET);
            rosterPush.setVersion("v97");

            Item pushedItem = vaglafItem();
            rosterPush.addRosterItem(pushedItem);
            connection.processPacket(rosterPush);

            assertEquals("Expect store version after push", "v97", store.getRosterVersion());

            Item storedItem = store.getEntry("vaglaf@example.com");
            assertNotNull("Expect vaglaf to be added", storedItem);
            assertEquals("Expect vaglaf to be equal to pushed item", pushedItem, storedItem);

            Collection<Item> rosterItems = new HashSet<Item>();
            for (RosterEntry entry : roster.getEntries()) {
                rosterItems.add(RosterEntry.toRosterItem(entry));
            }
            assertEquals(rosterItems, new HashSet<Item>(store.getEntries()));
        }

        // Simulate a roster push removing vaglaf
        {
            RosterPacket rosterPush = new RosterPacket();
            rosterPush.setTo("rostertest@example.com/home");
            rosterPush.setType(Type.SET);
            rosterPush.setVersion("v98");

            Item item = new Item("vaglaf@example.com", "vaglaf the only");
            item.setItemType(ItemType.remove);
            rosterPush.addRosterItem(item);
            connection.processPacket(rosterPush);

            assertNull("Store doses not contain vaglaf", store.getEntry("vaglaf@example.com"));
            assertEquals("Expect store version after push", "v98", store.getRosterVersion());
        }
    }

    private Item vaglafItem() {
        Item item = new Item("vaglaf@example.com", "vaglaf the only");
        item.setItemType(ItemType.both);
        item.addGroupName("all");
        item.addGroupName("friends");
        item.addGroupName("partners");
        return item;
    }

    private void populateStore(RosterStore store) throws IOException {
        store.addEntry(new RosterPacket.Item("geoff@example.com", "geoff hurley"), "");

        RosterPacket.Item item = new RosterPacket.Item("joe@example.com", "joe stevens");
        item.addGroupName("friends");
        item.addGroupName("partners");
        store.addEntry(item, "");

        item = new RosterPacket.Item("higgins@example.com", "higgins mcmann");
        item.addGroupName("all");
        item.addGroupName("friends");
        store.addEntry(item, "v96");
    }

    private void answerWithEmptyRosterResult() throws InterruptedException {
        // We expect that the roster request is the only packet sent. This is not part of the specification,
        // but a shortcut in the test implementation.
        Packet sentPacket = connection.getSentPacket();
        if (sentPacket instanceof RosterPacket) {
            final IQ emptyIQ = IQ.createResultIQ((RosterPacket)sentPacket);
            connection.processPacket(emptyIQ);
        } else {
            assertTrue("Expected to get a RosterPacket ", false);
        }
    }

}
