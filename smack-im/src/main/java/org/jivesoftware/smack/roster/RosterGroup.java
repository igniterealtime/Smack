/**
 *
 * Copyright 2003-2007 Jive Software.
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.roster.packet.RosterPacket;

import org.jxmpp.jid.Jid;

/**
 * A group of roster entries.
 *
 * @see Roster#getGroup(String)
 * @author Matt Tucker
 */
public class RosterGroup extends Manager {

    private final String name;
    private final Set<RosterEntry> entries;

    /**
     * Creates a new roster group instance.
     *
     * @param name the name of the group.
     * @param connection the connection the group belongs to.
     */
    RosterGroup(String name, XMPPConnection connection) {
        super(connection);
        this.name = name;
        entries = new LinkedHashSet<>();
    }

    /**
     * Returns the name of the group.
     *
     * @return the name of the group.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the group. Changing the group's name is like moving all the group entries
     * of the group to a new group specified by the new name. Since this group won't have entries
     * it will be removed from the roster. This means that all the references to this object will
     * be invalid and will need to be updated to the new group specified by the new name.
     *
     * @param name the name of the group.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setName(String name) throws NotConnectedException, NoResponseException, XMPPErrorException, InterruptedException {
        synchronized (entries) {
            for (RosterEntry entry : entries) {
                RosterPacket packet = new RosterPacket();
                packet.setType(IQ.Type.set);
                RosterPacket.Item item = RosterEntry.toRosterItem(entry);
                item.removeGroupName(this.name);
                item.addGroupName(name);
                packet.addRosterItem(item);
                connection().sendIqRequestAndWaitForResponse(packet);
            }
        }
    }

    /**
     * Returns the number of entries in the group.
     *
     * @return the number of entries in the group.
     */
    public int getEntryCount() {
        synchronized (entries) {
            return entries.size();
        }
    }

    /**
     * Returns an copied list of all entries in the group.
     *
     * @return all entries in the group.
     */
    public List<RosterEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    /**
     * Returns the roster entry associated with the given XMPP address or
     * <code>null</code> if the user is not an entry in the group.
     *
     * @param user the XMPP address of the user (eg "jsmith@example.com").
     * @return the roster entry or <code>null</code> if it does not exist in the group.
     */
    public RosterEntry getEntry(Jid user) {
        if (user == null) {
            return null;
        }
        // Roster entries never include a resource so remove the resource
        // if it's a part of the XMPP address.
        user = user.asBareJid();
        synchronized (entries) {
            for (RosterEntry entry : entries) {
                if (entry.getJid().equals(user)) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the specified entry is part of this group.
     *
     * @param entry a roster entry.
     * @return true if the entry is part of this group.
     */
    public boolean contains(RosterEntry entry) {
        synchronized (entries) {
            return entries.contains(entry);
        }
    }

    /**
     * Returns true if the specified XMPP address is an entry in this group.
     *
     * @param user the XMPP address of the user.
     * @return true if the XMPP address is an entry in this group.
     */
    public boolean contains(Jid user) {
        return getEntry(user) != null;
    }

    /**
     * Adds a roster entry to this group. If the entry was unfiled then it will be removed from
     * the unfiled list and will be added to this group.
     * Note that this is a synchronous call -- Smack must wait for the server
     * to receive the updated roster.
     *
     * @param entry a roster entry.
     * @throws XMPPErrorException if an error occurred while trying to add the entry to the group.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void addEntry(RosterEntry entry) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Only add the entry if it isn't already in the list.
        synchronized (entries) {
            if (!entries.contains(entry)) {
                RosterPacket packet = new RosterPacket();
                packet.setType(IQ.Type.set);
                RosterPacket.Item item = RosterEntry.toRosterItem(entry);
                item.addGroupName(getName());
                packet.addRosterItem(item);
                // Wait up to a certain number of seconds for a reply from the server.
                connection().sendIqRequestAndWaitForResponse(packet);
            }
        }
    }

    /**
     * Removes a roster entry from this group. If the entry does not belong to any other group
     * then it will be considered as unfiled, therefore it will be added to the list of unfiled
     * entries.
     * Note that this is a synchronous call -- Smack must wait for the server
     * to receive the updated roster.
     *
     * @param entry a roster entry.
     * @throws XMPPErrorException if an error occurred while trying to remove the entry from the group.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void removeEntry(RosterEntry entry) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Only remove the entry if it's in the entry list.
        // Remove the entry locally, if we wait for RosterPacketListenerProcess>>Packet(Packet)
        // to take place the entry will exist in the group until a packet is received from the
        // server.
        synchronized (entries) {
            if (entries.contains(entry)) {
                RosterPacket packet = new RosterPacket();
                packet.setType(IQ.Type.set);
                RosterPacket.Item item = RosterEntry.toRosterItem(entry);
                item.removeGroupName(this.getName());
                packet.addRosterItem(item);
                // Wait up to a certain number of seconds for a reply from the server.
                connection().sendIqRequestAndWaitForResponse(packet);
            }
        }
    }

    void addEntryLocal(RosterEntry entry) {
        // Update the entry if it is already in the list
        synchronized (entries) {
            entries.remove(entry);
            entries.add(entry);
        }
    }

    void removeEntryLocal(RosterEntry entry) {
         // Only remove the entry if it's in the entry list.
        synchronized (entries) {
            if (entries.contains(entry)) {
                entries.remove(entry);
            }
        }
    }
}
