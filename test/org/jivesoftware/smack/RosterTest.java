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

package org.jivesoftware.smack;

import java.util.Iterator;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Tests the Roster functionality by creating and removing roster entries.
 *
 * @author Gaston Dombiak
 */
public class RosterTest extends SmackTestCase {

    /**
     * Constructor for RosterTest.
     * @param name
     */
    public RosterTest(String name) {
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
            roster.createEntry(getBareJID(1), "gato11", new String[] { "Friends", "Family" });
            roster.createEntry(getBareJID(2), "gato12", new String[] { "Family" });

            // Wait until the server confirms the new entries
            while (roster.getEntryCount() != 2) {
                Thread.sleep(50);
            }

            Iterator it = roster.getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                Iterator groups = entry.getGroups();
                while (groups.hasNext()) {
                    RosterGroup rosterGroup = (RosterGroup) groups.next();
                    rosterGroup.removeEntry(entry);
                }
            }
            Thread.sleep(750);

            assertEquals(
                "The number of entries in connection 1 should be 1",
                1,
                getConnection(1).getRoster().getEntryCount());
            assertEquals(
                "The number of groups in connection 1 should be 0",
                0,
                getConnection(1).getRoster().getGroupCount());

            assertEquals(
                "The number of entries in connection 2 should be 1",
                1,
                getConnection(2).getRoster().getEntryCount());
            assertEquals(
                "The number of groups in connection 2 should be 0",
                0,
                getConnection(2).getRoster().getGroupCount());

            assertEquals(
                "The number of entries in connection 0 should be 2",
                2,
                roster.getEntryCount());
            assertEquals(
                "The number of groups in connection 0 should be 0",
                0,
                roster.getGroupCount());

            cleanUpRoster();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 1. Create entries in roster groups
     * 2. Iterate on all the entries and remove them from the roster
     * 3. Check that the number of entries and groups is zero
     */
    public void testDeleteAllRosterEntries() {
        try {
            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            roster.createEntry(getBareJID(1), "gato11", new String[] { "Friends" });
            roster.createEntry(getBareJID(2), "gato12", new String[] { "Family" });

            Thread.sleep(200);

            Iterator it = roster.getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                roster.removeEntry(entry);
                Thread.sleep(250);
            }

            assertEquals("Wrong number of entries in connection 0", 0, roster.getEntryCount());
            assertEquals("Wrong number of groups in connection 0", 0, roster.getGroupCount());

            assertEquals(
                "Wrong number of entries in connection 1",
                0,
                getConnection(1).getRoster().getEntryCount());
            assertEquals(
                "Wrong number of groups in connection 1",
                0,
                getConnection(1).getRoster().getGroupCount());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
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
            roster.createEntry(getBareJID(1), "gato11", null);
            roster.createEntry(getBareJID(2), "gato12", null);

            Thread.sleep(200);

            Iterator it = roster.getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                roster.removeEntry(entry);
                Thread.sleep(250);
            }

            assertEquals("Wrong number of entries in connection 0", 0, roster.getEntryCount());
            assertEquals("Wrong number of groups in connection 0", 0, roster.getGroupCount());

            assertEquals(
                "Wrong number of entries in connection 1",
                0,
                getConnection(1).getRoster().getEntryCount());
            assertEquals(
                "Wrong number of groups in connection 1",
                0,
                getConnection(1).getRoster().getGroupCount());
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
            roster.createEntry(getBareJID(1), null, null);

            Thread.sleep(200);

            // Change the roster entry name and check if the change was made
            Iterator it = roster.getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                entry.setName("gato11");
                assertEquals("gato11", entry.getName());
            }
            // Reload the roster and check the name again
            roster.reload();
            Thread.sleep(2000);
            it = roster.getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                assertEquals("gato11", entry.getName());
            }

            cleanUpRoster();
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

            Thread.sleep(200);

            getConnection(1).getRoster().createEntry(getBareJID(0), null, null);

            Thread.sleep(200);

            Iterator it = roster.getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                assertFalse("The roster entry belongs to a group", entry.getGroups().hasNext());
            }

            // Change the roster entry name and check if the change was made
            roster.createEntry(getBareJID(1), "NewName", new String[] { "Friends" });

            // Reload the roster and check the name again
            Thread.sleep(200);
            it = roster.getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                assertEquals("Name of roster entry is wrong", "NewName", entry.getName());
                assertTrue("The roster entry does not belong to any group", entry.getGroups()
                        .hasNext());
            }

            cleanUpRoster();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test if renaming a roster group works fine.
     *
     */
    public void testRenameRosterGroup() {
        try {
            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            roster.createEntry(getBareJID(1), "gato11", new String[] { "Friends" });
            roster.createEntry(getBareJID(2), "gato12", new String[] { "Friends" });

            Thread.sleep(200);

            roster.getGroup("Friends").setName("Amigos");
            Thread.sleep(200);
            assertNull("The group Friends still exists", roster.getGroup("Friends"));
            assertNotNull("The group Amigos does not exist", roster.getGroup("Amigos"));
            assertEquals(
                "Wrong number of entries in the group Amigos",
                2,
                roster.getGroup("Amigos").getEntryCount());

            roster.getGroup("Amigos").setName("");
            Thread.sleep(200);
            assertNull("The group Amigos still exists", roster.getGroup("Amigos"));
            assertNotNull("The group with no name does not exist", roster.getGroup(""));
            assertEquals(
                "Wrong number of entries in the group \"\" ",
                2,
                roster.getGroup("").getEntryCount());

            cleanUpRoster();
            Thread.sleep(200);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test presence management.
     */
    public void testRosterPresences() {
        try {
            Presence presence = null;

            // Create another connection for the same user of connection 1
            XMPPConnection conn4 = new XMPPConnection(getHost());
            conn4.login(getUsername(1), getUsername(1), "Home");

            // Add a new roster entry
            Roster roster = getConnection(0).getRoster();
            roster.createEntry(getBareJID(1), "gato11", null);

            Thread.sleep(250);

            // Check that a presence is returned for a user
            presence = roster.getPresence(getBareJID(1));
            assertNotNull("Returned a null Presence for an existing user", presence);

            // Check that the right presence is returned for a user+resource
            presence = roster.getPresenceResource(getUsername(1) + "@" + conn4.getHost() + "/Home");
            assertEquals(
                "Returned the wrong Presence",
                StringUtils.parseResource(presence.getFrom()),
                "Home");

            // Check that the right presence is returned for a user+resource
            presence = roster.getPresenceResource(getFullJID(1));
            assertEquals(
                "Returned the wrong Presence",
                StringUtils.parseResource(presence.getFrom()),
                "Smack");

            // Check that the no presence is returned for a non-existent user+resource
            presence = roster.getPresenceResource("noname@" + getHost() + "/Smack");
            assertNull("Returned a Presence for a non-existing user", presence);

            // Check that the returned presences are correct
            Iterator presences = roster.getPresences(getBareJID(1));
            int count = 0;
            while (presences.hasNext()) {
                count++;
                presences.next();
            }
            assertEquals("Wrong number of returned presences", count, 2);

            // Close the connection so one presence must go            
            conn4.close();

            // Check that the returned presences are correct
            presences = roster.getPresences(getBareJID(1));
            count = 0;
            while (presences.hasNext()) {
                count++;
                presences.next();
            }
            assertEquals("Wrong number of returned presences", count, 1);

            Thread.sleep(200);
            cleanUpRoster();

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Clean up all the entries in the roster
     */
    private void cleanUpRoster() {
        // Delete all the entries from the roster
        Iterator it = getConnection(0).getRoster().getEntries();
        while (it.hasNext()) {
            RosterEntry entry = (RosterEntry) it.next();
            try {
                getConnection(0).getRoster().removeEntry(entry);
            } catch (XMPPException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
        try {
            Thread.sleep(700);
        }
        catch (Exception e) {
        }

        assertEquals(
            "Wrong number of entries in connection 0",
            0,
            getConnection(0).getRoster().getEntryCount());
        assertEquals(
            "Wrong number of groups in connection 0",
            0,
            getConnection(0).getRoster().getGroupCount());

        assertEquals(
            "Wrong number of entries in connection 1",
            0,
            getConnection(1).getRoster().getEntryCount());
        assertEquals(
            "Wrong number of groups in connection 1",
            0,
            getConnection(1).getRoster().getGroupCount());

        assertEquals(
            "Wrong number of entries in connection 2",
            0,
            getConnection(2).getRoster().getEntryCount());
        assertEquals(
            "Wrong number of groups in connection 2",
            0,
            getConnection(2).getRoster().getGroupCount());
    }

    protected int getMaxConnections() {
        return 3;
    }
}