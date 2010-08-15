package org.jivesoftware.smack;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;

import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.packet.Presence;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the behavior of the roster if the connection is not authenticated yet.
 * 
 * @author Henning Staib
 */
public class RosterOfflineTest {

    Connection connection;

    Roster roster;

    @Before
    public void setup() {
        this.connection = new XMPPConnection("localhost");
        assertFalse(connection.isConnected());

        roster = connection.getRoster();
        assertNotNull(roster);
    }

    @Test
    public void shouldThrowNoExceptionOnGetterMethods() {
        // all getter methods should work
        assertFalse(roster.contains("test"));

        Collection<RosterEntry> entries = roster.getEntries();
        assertTrue(entries.size() == 0);

        assertNull(roster.getEntry("test"));

        assertEquals(0, roster.getEntryCount());

        assertNull(roster.getGroup("test"));

        assertEquals(0, roster.getGroupCount());

        Collection<RosterGroup> groups = roster.getGroups();
        assertEquals(0, groups.size());

        Presence presence = roster.getPresence("test");
        assertEquals(Presence.Type.unavailable, presence.getType());

        Presence presenceResource = roster.getPresenceResource("test");
        assertEquals(Presence.Type.unavailable, presenceResource.getType());

        Iterator<Presence> iterator = roster.getPresences("test");
        assertTrue(iterator.hasNext());
        assertEquals(Presence.Type.unavailable, iterator.next().getType());
        assertFalse(iterator.hasNext());

        assertEquals(0, roster.getUnfiledEntries().size());

        assertEquals(0, roster.getUnfiledEntryCount());

        roster.setSubscriptionMode(SubscriptionMode.accept_all);
        assertEquals(SubscriptionMode.accept_all, roster.getSubscriptionMode());

    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnCreateEntry() throws Exception {
        roster.createEntry("test", "test", null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnCreateGroup() throws Exception {
        roster.createGroup("test");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnReload() throws Exception {
        roster.reload();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionRemoveEntry() throws Exception {
        roster.removeEntry(null);
    }

}
