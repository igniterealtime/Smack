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
package org.jivesoftware.smack.roster.rosterstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.packet.RosterPacket.Item;
import org.jivesoftware.smack.roster.packet.RosterPacket.ItemType;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;

/**
 * Tests the implementation of {@link DirectoryRosterStore}.
 *
 * @author Lars Noschinski
 */
public class DirectoryRosterStoreTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Tests that opening an uninitialized directory fails.
     */
    @Test
    public void testStoreUninitialized() throws IOException {
        File storeDir = tmpFolder.newFolder();
        assertNull(DirectoryRosterStore.open(storeDir));
    }

    /**
     * Tests that an initialized directory is empty.
     */
    @Test
    public void testStoreInitializedEmpty() throws IOException {
        File storeDir = tmpFolder.newFolder();
        DirectoryRosterStore store = DirectoryRosterStore.init(storeDir);
        assertNotNull("Initialization returns store", store);
        assertEquals("Freshly initialized store must have empty version",
                "", store.getRosterVersion());
        assertEquals("Freshly initialized store must have no entries",
                0, store.getEntries().size());
    }

    /**
     * Tests adding and removing entries.
     */
    @Test
    public void testStoreAddRemove() throws IOException {
        File storeDir = tmpFolder.newFolder();
        DirectoryRosterStore store = DirectoryRosterStore.init(storeDir);

        assertEquals("Initial roster version", "", store.getRosterVersion());

        BareJid userName = JidTestUtil.DUMMY_AT_EXAMPLE_ORG;

        final RosterPacket.Item item1 = new Item(userName, null);
        final String version1 = "1";
        store.addEntry(item1, version1);

        assertEquals("Adding entry sets version correctly", version1, store.getRosterVersion());

        RosterPacket.Item storedItem = store.getEntry(userName);
        assertNotNull("Added entry not found found", storedItem);
        assertEquals("User of added entry",
                item1.getJid(), storedItem.getJid());
        assertEquals("Name of added entry",
                item1.getName(), storedItem.getName());
        assertEquals("Groups", item1.getGroupNames(), storedItem.getGroupNames());
        assertEquals("ItemType of added entry",
                item1.getItemType(), storedItem.getItemType());
        assertEquals("ItemStatus of added entry",
                item1.isSubscriptionPending(), storedItem.isSubscriptionPending());
        assertEquals("Approved of added entry",
                item1.isApproved(), storedItem.isApproved());


        final String version2 = "2";
        final RosterPacket.Item item2 = new Item(userName, "Ursula Example");
        item2.addGroupName("users");
        item2.addGroupName("examples");
        item2.setSubscriptionPending(true);
        item2.setItemType(ItemType.none);
        item2.setApproved(true);
        store.addEntry(item2, version2);
        assertEquals("Updating entry sets version correctly", version2, store.getRosterVersion());
        storedItem = store.getEntry(userName);
        assertNotNull("Added entry not found", storedItem);
        assertEquals("User of added entry",
                item2.getJid(), storedItem.getJid());
        assertEquals("Name of added entry",
                item2.getName(), storedItem.getName());
        assertEquals("Groups", item2.getGroupNames(), storedItem.getGroupNames());
        assertEquals("ItemType of added entry",
                item2.getItemType(), storedItem.getItemType());
        assertEquals("ItemStatus of added entry",
                item2.isSubscriptionPending(), storedItem.isSubscriptionPending());
        assertEquals("Approved of added entry",
                item2.isApproved(), storedItem.isApproved());

        List<Item> entries = store.getEntries();
        assertEquals("Number of entries", 1, entries.size());

        final RosterPacket.Item item3 = new Item(JidTestUtil.BARE_JID_1, "Foo Bar");
        item3.addGroupName("The Foo Fighters");
        item3.addGroupName("Bar Friends");
        item3.setSubscriptionPending(true);
        item3.setItemType(ItemType.both);

        final RosterPacket.Item item4 = new Item(JidTestUtil.BARE_JID_2, "Baba Baz");
        item4.addGroupName("The Foo Fighters");
        item4.addGroupName("Bar Friends");
        item4.setSubscriptionPending(false);
        item4.setItemType(ItemType.both);
        item4.setApproved(true);

        ArrayList<Item> items34 = new ArrayList<RosterPacket.Item>();
        items34.add(item3);
        items34.add(item4);

        String version3 = "3";
        store.resetEntries(items34, version3);

        storedItem = store.getEntry(JidTestUtil.BARE_JID_1);
        assertNotNull("Added entry not found", storedItem);
        assertEquals("User of added entry",
                item3.getJid(), storedItem.getJid());
        assertEquals("Name of added entry",
                item3.getName(), storedItem.getName());
        assertEquals("Groups", item3.getGroupNames(), storedItem.getGroupNames());
        assertEquals("ItemType of added entry",
                item3.getItemType(), storedItem.getItemType());
        assertEquals("ItemStatus of added entry",
                item3.isSubscriptionPending(), storedItem.isSubscriptionPending());
        assertEquals("Approved of added entry",
                item3.isApproved(), storedItem.isApproved());


        storedItem = store.getEntry(JidTestUtil.BARE_JID_2);
        assertNotNull("Added entry not found", storedItem);
        assertEquals("User of added entry",
                item4.getJid(), storedItem.getJid());
        assertEquals("Name of added entry",
                item4.getName(), storedItem.getName());
        assertEquals("Groups", item4.getGroupNames(), storedItem.getGroupNames());
        assertEquals("ItemType of added entry",
                item4.getItemType(), storedItem.getItemType());
        assertEquals("ItemStatus of added entry",
                item4.isSubscriptionPending(), storedItem.isSubscriptionPending());
        assertEquals("Approved of added entry",
                item4.isApproved(), storedItem.isApproved());

        entries = store.getEntries();
        assertEquals("Number of entries", 2, entries.size());

        String version4 = "4";
        store.removeEntry(JidTestUtil.BARE_JID_2, version4);
        assertEquals("Removing entry sets version correctly",
                version4, store.getRosterVersion());
        assertNull("Removed entry is gone", store.getEntry(userName));

        entries = store.getEntries();
        assertEquals("Number of entries", 1, entries.size());
    }

}
