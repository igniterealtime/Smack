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
    List entries;

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

    public int getEntryCount() {
        synchronized (entries) {
            return entries.size();
        }
    }

    public Iterator getEntries() {
        return Collections.unmodifiableList(entries).iterator();
    }

    public boolean contains(RosterEntry entry) {
        return entries.contains(entry);
    }

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

    public void removeEntry(RosterEntry entry) {
        // Only remove the entry if it's in the entry list.
        synchronized (entries) {
            if (entries.contains(entry)) {
                entries.remove(entry);
                RosterPacket packet = new RosterPacket();
                packet.setType(IQ.Type.SET);
                packet.addRosterItem(RosterEntry.toRosterItem(entry));
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
