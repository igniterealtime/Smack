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

import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;

import java.util.*;

/**
 * A group of roster entries.
 *
 * @see Roster#getGroup(String)
 * @author Matt Tucker
 */
public class RosterGroup {

    private String name;
    private XMPPConnection connection;
    private List entries;

    /**
     * Creates a new roster group instance.
     *
     * @param name the name of the group.
     * @param connection the connection the group belongs to.
     */
    RosterGroup(String name, XMPPConnection connection) {
        this.name = name;
        this.connection = connection;
        entries = new ArrayList();
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
     * Sets the name of the group.
     *
     * @param name the name of the group.
     */
    public void setName(String name) {
        this.name = name;
        synchronized (entries) {
            for (int i=0; i<entries.size(); i++) {
                RosterPacket packet = new RosterPacket();
                packet.setType(IQ.Type.SET);
                RosterEntry entry = (RosterEntry)entries.get(i);
                packet.addRosterItem(RosterEntry.toRosterItem(entry));
                connection.sendPacket(packet);
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
     * Returns an iterator for the entries in the group.
     *
     * @return an iterator for the entries in the group.
     */
    public Iterator getEntries() {
        synchronized (entries) {
            return Collections.unmodifiableList(new ArrayList(entries)).iterator();
        }
    }

    /**
     * Returns the roster entry associated with the given XMPP address or
     * <tt>null</tt> if the user is not an entry in the group.
     *
     * @param user the XMPP address of the user (eg "jsmith@example.com").
     * @return the roster entry or <tt>null</tt> if it does not exist in the group.
     */
    public RosterEntry getEntry(String user) {
        if (user == null) {
            return null;
        }
        // Roster entries never include a resource so remove the resource
        // if it's a part of the XMPP address.
        user = StringUtils.parseBareAddress(user);
        synchronized (entries) {
            for (Iterator i=entries.iterator(); i.hasNext(); ) {
                RosterEntry entry = (RosterEntry)i.next();
                if (entry.getUser().equals(user)) {
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
    public boolean contains(String user) {
        if (user == null) {
            return false;
        }
        // Roster entries never include a resource so remove the resource
        // if it's a part of the XMPP address.
        user = StringUtils.parseBareAddress(user);
        synchronized (entries) {
            for (Iterator i=entries.iterator(); i.hasNext(); ) {
                RosterEntry entry = (RosterEntry)i.next();
                if (entry.getUser().equals(user)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds a roster entry to this group. If the entry was unfiled then it will be removed from 
     * the unfiled list and will be added to this group.
     *
     * @param entry a roster entry.
     */
    public void addEntry(RosterEntry entry) {
        // Only add the entry if it isn't already in the list.
        synchronized (entries) {
            if (!entries.contains(entry)) {
                entries.add(entry);
                RosterPacket packet = new RosterPacket();
                packet.setType(IQ.Type.SET);
                packet.addRosterItem(RosterEntry.toRosterItem(entry));
                connection.sendPacket(packet);
            }
        }
    }

    /**
     * Removes a roster entry from this group. If the entry does not belong to any other group 
     * then it will be considered as unfiled, therefore it will be added to the list of unfiled 
     * entries.
     *
     * @param entry a roster entry.
     */
    public void removeEntry(RosterEntry entry) {
        // Only remove the entry if it's in the entry list.
        // The actual removal logic takes place in RosterPacketListenerprocess>>Packet(Packet)
        synchronized (entries) {
            if (entries.contains(entry)) {
                RosterPacket packet = new RosterPacket();
                packet.setType(IQ.Type.SET);
                RosterPacket.Item item = RosterEntry.toRosterItem(entry);
                item.removeGroupName(this.getName());
                packet.addRosterItem(item);
                connection.sendPacket(packet);
            }
        }
    }

    void addEntryLocal(RosterEntry entry) {
        // Only add the entry if it isn't already in the list.
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