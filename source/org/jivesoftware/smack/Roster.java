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

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.filter.*;

import java.util.*;

/**
 * Roster.
 *
 * @see XMPPConnection#getRoster()
 * @author Matt Tucker
 */
public class Roster {

    private XMPPConnection connection;
    private Map groups;

    Roster(final XMPPConnection connection) {
        this.connection = connection;
        groups = new HashMap();
        // Listen for any roster packets.
        PacketFilter filter = new PacketTypeFilter(RosterPacket.class);
        PacketListener rosterListener = new RosterListener();
        connection.addPacketListener(rosterListener, filter);
    }

    /**
     * Reloads the entire roster from the server. This is an asynchronous operation,
     * which means the method will return immediately, and the roster will be
     * reloaded at a later point when the server responds to the reload request.
     */
    public void reload() {
        connection.sendPacket(new RosterPacket());
    }

    /**
     * Creates a new group.
     *
     * @param name the name of the group.
     * @return a new group.
     */
    public RosterGroup createGroup(String name) {
        synchronized (groups) {
            if (groups.containsKey(name)) {
                throw new IllegalArgumentException("Group with name " + name + " alread exists.");
            }
            RosterGroup group = new RosterGroup(name, connection);
            groups.put(name, group);
            return group;
        }
    }

    /**
     * Returns the roster group with the specified name, or <tt>null</tt> if the
     * group doesn't exist.
     *
     * @param name the name of the group.
     * @return the roster group with the specified name.
     */
    public RosterGroup getGroup(String name) {
        synchronized (groups) {
            return (RosterGroup)groups.get(name);
        }
    }

    /**
     * Returns an iterator the for all the roster groups.
     *
     * @return an iterator for all roster groups.
     */
    public Iterator getGroups() {
        synchronized (groups) {
            List groupsList = Collections.unmodifiableList(new ArrayList(groups.values()));
            return groupsList.iterator();
        }
    }

    /**
     * Listens for all roster packets and processes them.
     */
    private class RosterListener implements PacketListener {

        public void processPacket(Packet packet) {
            RosterPacket rosterPacket = (RosterPacket)packet;
            for (Iterator i=rosterPacket.getRosterItems(); i.hasNext(); ) {
                RosterPacket.Item item = (RosterPacket.Item)i.next();
                RosterEntry entry = new RosterEntry(item.getUser(), item.getName(),
                        connection);
                // Find the list of groups that the user currently belongs to.
                List currentGroupNames = new ArrayList();
                for (Iterator j = entry.getGroups(); j.hasNext();  ) {
                    RosterGroup group = (RosterGroup)j.next();
                    currentGroupNames.add(group.getName());
                }

                List newGroupNames = new ArrayList();
                for (Iterator k = item.getGroupNames(); k.hasNext();  ) {
                    String groupName = (String)k.next();
                    // Add the group name to the list.
                    newGroupNames.add(groupName);

                    // Add the entry to the group.
                    RosterGroup group = getGroup(groupName);
                    if (group == null) {
                        group = createGroup(groupName);
                        groups.put(groupName, group);
                    }
                    // Add the entry.
                    group.addEntryLocal(entry);
                }

                // We have the list of old and new group names. We now need to
                // remove the entry from the all the groups it may no longer belong
                // to. We do this by subracting the new group set from the old.
                for (int m=0; m<newGroupNames.size(); m++) {
                    currentGroupNames.remove(newGroupNames.get(m));
                }
                // Loop through any groups that remain and remove the entries.
                for (int n=0; n<currentGroupNames.size(); n++) {
                    String groupName = (String)currentGroupNames.get(n);
                    RosterGroup group = getGroup(groupName);
                    group.removeEntryLocal(entry);
                    if (group.getEntryCount() == 0) {
                        synchronized (groups) {
                            groups.remove(groupName);
                        }
                    }
                }
            }
        }
    }
}
