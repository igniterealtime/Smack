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
import org.jivesoftware.smack.util.StringUtils;

import java.util.*;

/**
 * Represents a user's roster, which is the collection of users a person receives
 * presence updates for. Roster items are categorized into groups for easier management.<p>
 *
 * Others users may attempt to subscribe to this user using a subscription request. Three
 * modes are supported for handling these requests: <ul>
 *      <li> SUBSCRIPTION_ACCEPT_ALL -- accept all subscription requests.
 *      <li> SUBCRIPTION_REJECT_ALL -- reject all subscription requests.
 *      <li> SUBSCRIPTION_MANUAL -- manually process all subscription requests. </ul>
 *
 * All presence subscription requests are automatically approved to this client
 * are automatically approved. This logic will be updated in the future to allow for
 * pluggable behavior.
 *
 * @see XMPPConnection#getRoster()
 * @author Matt Tucker
 */
public class Roster {

    /**
     * Automatically accept all subscription requests. This is the default mode
     * and is suitable for simple client. More complex client will likely wish to
     * handle subscription requests manually.
     */
    public static final int SUBCRIPTION_ACCEPT_ALL = 0;

    /**
     * Automatically reject all subscription requests.
     */
    public static final int SUBCRIPTION_REJECT_ALL = 1;

    /**
     * Subscription requests are ignored, which means they must be manually
     * processed by registering a listener for presence packets and then looking
     * for any presence requests that have the type Presence.Type.SUBSCRIBE.
     */
    public static final int SUBSCRIPTION_MANUAL = 2;

    private XMPPConnection connection;
    private Map groups;
    private List unfiledEntries;
    private List rosterListeners;
    private Map presenceMap;
    // The roster is marked as initialized when at least a single roster packet
    // has been recieved and processed.
    boolean rosterInitialized = false;

    private int subscriptionMode = SUBCRIPTION_ACCEPT_ALL;

    /**
     * Creates a new roster.
     *
     * @param connection an XMPP connection.
     */
    Roster(final XMPPConnection connection) {
        this.connection = connection;
        groups = new Hashtable();
        unfiledEntries = new ArrayList();
        rosterListeners = new ArrayList();
        presenceMap = new HashMap();
        // Listen for any roster packets.
        PacketFilter rosterFilter = new PacketTypeFilter(RosterPacket.class);
        connection.addPacketListener(new RosterPacketListener(), rosterFilter);
        // Listen for any presence packets.
        PacketFilter presenceFilter = new PacketTypeFilter(Presence.class);
        connection.addPacketListener(new PresencePacketListener(), presenceFilter);
    }

    /**
     * Returns the subscription processing mode, which dictates what action
     * Smack will take when subscription requests from other users are made.
     *
     * @return the subscription mode.
     */
    public int getSubscriptionMode() {
        return subscriptionMode;
    }

    /**
     * Sets the subscription processing mode, which dictates what action
     * Smack will take when subscription requests from other users are made.
     *
     * @param subscriptionMode the subscription mode.
     */
    public void setSubscriptionMode(int subscriptionMode) {
        if (subscriptionMode != SUBCRIPTION_ACCEPT_ALL &&
                subscriptionMode != SUBCRIPTION_REJECT_ALL &&
                subscriptionMode != SUBSCRIPTION_MANUAL)
        {
            throw new IllegalArgumentException("Invalid mode.");
        }
        this.subscriptionMode = subscriptionMode;
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
     * Adds a listener to this roster. The listener will be fired anytime one or more
     * changes to the roster are pushed from the server.
     *
     * @param rosterListener a roster listener.
     */
    public void addRosterListener(RosterListener rosterListener) {
        synchronized (rosterListeners) {
            if (!rosterListeners.contains(rosterListener)) {
                rosterListeners.add(rosterListener);
            }
        }
    }

    /**
     * Removes a listener from this roster. The listener will be fired anytime one or more
     * changes to the roster are pushed from the server.
     *
     * @param rosterListener a roster listener.
     */
    public void removeRosterListener(RosterListener rosterListener) {
        synchronized (rosterListeners) {
            rosterListeners.remove(rosterListener);
        }
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
     * Cretaes a new roster entry and prsence subscription. The server will asynchronously
     * update the roster with the subscription status.
     *
     * @param user the user.
     * @param name the nickname of the user.
     * @param groups the list of groups entry will belong to, or <tt>null</tt> if the
     *      the roster entry won't belong to a group.
     */
    public void createEntry(String user, String name, String [] groups) throws XMPPException {
        // Create and send roster entry creation packet.
        RosterPacket rosterPacket = new RosterPacket();
        rosterPacket.setType(IQ.Type.SET);
        RosterPacket.Item item = new RosterPacket.Item(user, name);
        if (groups != null) {
            for (int i=0; i<groups.length; i++) {
                item.addGroupName(groups[i]);
            }
        }
        rosterPacket.addRosterItem(item);
        // Wait up to 5 seconds for a reply from the server.
        PacketCollector collector = connection.createPacketCollector(
                new PacketIDFilter(rosterPacket.getPacketID()));
        connection.sendPacket(rosterPacket);
        IQ response = (IQ)collector.nextResult(5000);
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        // If the server replied with an error, throw an exception.
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
        collector.cancel();

        // Create a presence subscription packet and send.
        Presence presencePacket = new Presence(Presence.Type.SUBSCRIBE);
        presencePacket.setTo(user);
        connection.sendPacket(presencePacket);
    }

    /**
     * Returns an Iterator for the roster entries that haven't been assigned to any groups.
     *
     * @return an iterator the roster entries that haven't been filed into groups.
     */
    public Iterator getUnfiledEntries() {
        synchronized (unfiledEntries) {
            return Collections.unmodifiableList(new ArrayList(unfiledEntries)).iterator();
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
     * Returns the number of the groups in the roster.
     *
     * @return the number of groups in the roster.
     */
    public int getGroupCount() {
        synchronized (groups) {
            return groups.size();
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
     * Returns the presence info for a particular user, or <tt>null</tt> if there is
     * no presence information.
     *
     * @param user a fully qualified xmpp ID, e.g. jdoe@example.com
     * @return the user's presence.
     */
    public Presence getPresence(String user) {
        String key = StringUtils.parseName(user) + "@" + StringUtils.parseServer(user);
        return (Presence)presenceMap.get(key);
    }

    /**
     * Fires roster listeners.
     */
    private void fireRosterListeners() {
        RosterListener [] listeners = null;
        synchronized (rosterListeners) {
            listeners = new RosterListener[rosterListeners.size()];
            rosterListeners.toArray(listeners);
        }
        for (int i=0; i<listeners.length; i++) {
            listeners[i].rosterModified();
        }
    }

    /**
     * Listens for all presence packets and processes them.
     */
    private class PresencePacketListener implements PacketListener {
        public void processPacket(Packet packet) {
            Presence presence = (Presence)packet;
            String from = presence.getFrom();
            String key = StringUtils.parseName(from) + "@" + StringUtils.parseServer(from);
            // If an "available" packet, add it to the presence map. This means that for
            // a particular user, we'll only ever have a single presence packet saved.
            // Because this ignores resources, this is not an ideal solution, so will
            // have to be revisited.
            if (presence.getType() == Presence.Type.AVAILABLE) {
                presenceMap.put(key, presence);
            }
            // If an "unavailable" packet, remove any entries in the presence map.
            else if (presence.getType() == Presence.Type.UNAVAILABLE) {
                presenceMap.remove(key);
            }

            else if (presence.getType() == Presence.Type.SUBSCRIBE) {
                if (subscriptionMode == SUBCRIPTION_ACCEPT_ALL) {
                    // Accept all subscription requests.
                    Presence response = new Presence(Presence.Type.SUBSCRIBED);
                    response.setTo(presence.getFrom());
                    connection.sendPacket(response);
                }
                else if (subscriptionMode == SUBCRIPTION_REJECT_ALL) {
                    // Reject all subscription requests.
                    Presence response = new Presence(Presence.Type.UNSUBSCRIBED);
                    response.setTo(presence.getFrom());
                    connection.sendPacket(response);
                }
                // Otherwise, in manual mode so ignore.
            }
        }
    }

    /**
     * Listens for all roster packets and processes them.
     */
    private class RosterPacketListener implements PacketListener {

        public void processPacket(Packet packet) {
            RosterPacket rosterPacket = (RosterPacket)packet;
            for (Iterator i=rosterPacket.getRosterItems(); i.hasNext(); ) {
                RosterPacket.Item item = (RosterPacket.Item)i.next();
                if (item.getItemType() == RosterPacket.ItemType.TO ||
                        item.getItemType() == RosterPacket.ItemType.BOTH)
                {

                }
                RosterEntry entry = new RosterEntry(item.getUser(), item.getName(),
                        item.getItemType(), connection);
                // If the roster entry has any groups, remove it from the list of unfiled
                // users.
                if (entry.getGroups().hasNext()) {
                    synchronized (unfiledEntries) {
                        unfiledEntries.remove(entry);
                    }
                }
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
                // If the user doesn't belong to any groups, add them to the list of
                // unfiled users.
                if (currentGroupNames.isEmpty() && newGroupNames.isEmpty()) {
                    synchronized (unfiledEntries) {
                        if (!unfiledEntries.contains(entry)) {
                            unfiledEntries.add(entry);
                        }
                    }
                }
            }

            // Fire event for roster listeners.
            fireRosterListeners();

            // Mark the roster as initialized.
            rosterInitialized = true;
        }
    }
}