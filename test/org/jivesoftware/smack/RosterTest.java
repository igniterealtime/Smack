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

import junit.framework.TestCase;

/**
 * Tests the Roster functionality by creating and removing roster entries.
 *
 * @author Gaston Dombiak
 */
public class RosterTest extends TestCase {
    
    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;
    private XMPPConnection conn3 = null;

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
            conn1.getRoster().createEntry("gato11@localhost", "gato11", new String[] {"Friends", "Family"});
            conn1.getRoster().createEntry("gato12@localhost", "gato12", new String[] {"Family"});

            Thread.sleep(200);

            Iterator it = conn1.getRoster().getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                Iterator groups = entry.getGroups();
                while (groups.hasNext()) {
                    RosterGroup rosterGroup = (RosterGroup) groups.next();
                    rosterGroup.removeEntry(entry);
                    Thread.sleep(250);
                }
            }

            assertEquals("The number of entries in conn1 should be 2", 2, conn1.getRoster().getEntryCount());
            assertEquals("The number of groups in conn1 should be 0", 0, conn1.getRoster().getGroupCount());

            assertEquals("The number of entries in conn2 should be 1", 1, conn2.getRoster().getEntryCount());
            assertEquals("The number of groups in conn2 should be 0", 0, conn2.getRoster().getGroupCount());

            assertEquals("The number of entries in conn3 should be 1", 1, conn3.getRoster().getEntryCount());
            assertEquals("The number of groups in conn3 should be 0", 0, conn3.getRoster().getGroupCount());
            
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
            conn1.getRoster().createEntry("gato11@localhost", "gato11", new String[] {"Friends"});
            conn1.getRoster().createEntry("gato12@localhost", "gato12", new String[] {"Family"});

            Thread.sleep(200);

            Iterator it = conn1.getRoster().getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                conn1.getRoster().removeEntry(entry);
                Thread.sleep(250);
            }

            assertEquals("The number of entries in conn1 should be 0", 0, conn1.getRoster().getEntryCount());
            assertEquals("The number of groups in conn1 should be 0", 0, conn1.getRoster().getGroupCount());

            assertEquals("The number of entries in conn2 should be 0", 0, conn2.getRoster().getEntryCount());
            assertEquals("The number of groups in conn2 should be 0", 0, conn2.getRoster().getGroupCount());
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
            conn1.getRoster().createEntry("gato11@localhost", "gato11", null);
            conn1.getRoster().createEntry("gato12@localhost", "gato12", null);

            Thread.sleep(200);

            Iterator it = conn1.getRoster().getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                conn1.getRoster().removeEntry(entry);
                Thread.sleep(250);
            }

            assertEquals("The number of entries in conn1 should be 0", 0, conn1.getRoster().getEntryCount());
            assertEquals("The number of groups in conn1 should be 0", 0, conn1.getRoster().getGroupCount());

            assertEquals("The number of entries in conn2 should be 0", 0, conn2.getRoster().getEntryCount());
            assertEquals("The number of groups in conn2 should be 0", 0, conn2.getRoster().getGroupCount());
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
            conn1.getRoster().createEntry("gato11@localhost", null, null);

            Thread.sleep(200);

            // Change the roster entry name and check if the change was made
            Iterator it = conn1.getRoster().getEntries();
            while (it.hasNext()) {
                RosterEntry entry = (RosterEntry) it.next();
                entry.setName("gato11");
                assertEquals("gato11", entry.getName());
            }
            // Reload the roster and check the name again 
            conn1.getRoster().reload();
            Thread.sleep(2000);
            it = conn1.getRoster().getEntries();
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
     * Clean up all the entries in the roster
     */
    private void cleanUpRoster() {
        // Delete all the entries from the roster            
        Iterator it = conn1.getRoster().getEntries();
        while (it.hasNext()) {
            RosterEntry entry = (RosterEntry) it.next();
            conn1.getRoster().removeEntry(entry);
        }
        try {
            Thread.sleep(700);
        }
        catch (Exception e) {}
    
        assertEquals("The number of entries in conn1 should be 0", 0, conn1.getRoster().getEntryCount());
        assertEquals("The number of groups in conn1 should be 0", 0, conn1.getRoster().getGroupCount());
    
        assertEquals("The number of entries in conn2 should be 0", 0, conn2.getRoster().getEntryCount());
        assertEquals("The number of groups in conn2 should be 0", 0, conn2.getRoster().getGroupCount());
                
        assertEquals("The number of entries in conn3 should be 0", 0, conn3.getRoster().getEntryCount());
        assertEquals("The number of groups in conn3 should be 0", 0, conn3.getRoster().getGroupCount());
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        try {
            // Connect to the server
            conn1 = new XMPPConnection("localhost");
            // Use a second connection to create and delete the entry that will be added and 
            // deleted from the roster  
            conn2 = new XMPPConnection("localhost");
            // Use a third connection to create and delete the entry that will be added and 
            // deleted from the roster  
            conn3 = new XMPPConnection("localhost");
            
            // Create the test accounts
            if (!conn1.getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");
            conn1.getAccountManager().createAccount("gato10", "gato10");
            conn2.getAccountManager().createAccount("gato11", "gato11");
            conn3.getAccountManager().createAccount("gato12", "gato12");

            // Login with the test accounts
            conn1.login("gato10", "gato10");
            conn2.login("gato11", "gato11");
            conn3.login("gato12", "gato12");

        }
        catch (Exception e) {
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
        
        // Close all the connections
        conn1.close();
        conn2.close();
        conn3.close();
    }

}
