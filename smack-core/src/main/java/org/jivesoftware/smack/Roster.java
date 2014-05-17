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

package org.jivesoftware.smack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.RosterPacket.Item;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Represents a user's roster, which is the collection of users a person receives
 * presence updates for. Roster items are categorized into groups for easier management.<p>
 * <p/>
 * Others users may attempt to subscribe to this user using a subscription request. Three
 * modes are supported for handling these requests: <ul>
 * <li>{@link SubscriptionMode#accept_all accept_all} -- accept all subscription requests.</li>
 * <li>{@link SubscriptionMode#reject_all reject_all} -- reject all subscription requests.</li>
 * <li>{@link SubscriptionMode#manual manual} -- manually process all subscription requests.</li>
 * </ul>
 *
 * @author Matt Tucker
 * @see XMPPConnection#getRoster()
 */
public class Roster {

    private static final Logger LOGGER = Logger.getLogger(Roster.class.getName());

    private static final PacketFilter ROSTER_PUSH_FILTER = new AndFilter(new PacketTypeFilter(
                    RosterPacket.class), new IQTypeFilter(IQ.Type.SET));

    private static final PacketFilter PRESENCE_PACKET_FILTER = new PacketTypeFilter(Presence.class);

    /**
     * The default subscription processing mode to use when a Roster is created. By default
     * all subscription requests are automatically accepted.
     */
    private static SubscriptionMode defaultSubscriptionMode = SubscriptionMode.accept_all;

    private final XMPPConnection connection;
    private final RosterStore rosterStore;
    private final Map<String, RosterGroup> groups = new ConcurrentHashMap<String, RosterGroup>();
    private final Map<String,RosterEntry> entries = new ConcurrentHashMap<String,RosterEntry>();
    private final List<RosterEntry> unfiledEntries = new CopyOnWriteArrayList<RosterEntry>();
    private final List<RosterListener> rosterListeners = new CopyOnWriteArrayList<RosterListener>();
    private final Map<String, Map<String, Presence>> presenceMap = new ConcurrentHashMap<String, Map<String, Presence>>();
    // The roster is marked as initialized when at least a single roster packet
    // has been received and processed.
    boolean rosterInitialized = false;
    private final PresencePacketListener presencePacketListener = new PresencePacketListener();

    private SubscriptionMode subscriptionMode = getDefaultSubscriptionMode();

    /**
     * Returns the default subscription processing mode to use when a new Roster is created. The
     * subscription processing mode dictates what action Smack will take when subscription
     * requests from other users are made. The default subscription mode
     * is {@link SubscriptionMode#accept_all}.
     *
     * @return the default subscription mode to use for new Rosters
     */
    public static SubscriptionMode getDefaultSubscriptionMode() {
        return defaultSubscriptionMode;
    }

    /**
     * Sets the default subscription processing mode to use when a new Roster is created. The
     * subscription processing mode dictates what action Smack will take when subscription
     * requests from other users are made. The default subscription mode
     * is {@link SubscriptionMode#accept_all}.
     *
     * @param subscriptionMode the default subscription mode to use for new Rosters.
     */
    public static void setDefaultSubscriptionMode(SubscriptionMode subscriptionMode) {
        defaultSubscriptionMode = subscriptionMode;
    }

    /**
     * Creates a new roster.
     *
     * @param connection an XMPP connection.
     */
    Roster(final XMPPConnection connection) {
        this.connection = connection;
        rosterStore = connection.getConfiguration().getRosterStore();
        // Listen for any roster packets.
        connection.addPacketListener(new RosterPushListener(), ROSTER_PUSH_FILTER);
        // Listen for any presence packets.
        connection.addPacketListener(presencePacketListener, PRESENCE_PACKET_FILTER);

        // Listen for connection events
        connection.addConnectionListener(new AbstractConnectionListener() {
            
            public void connectionClosed() {
                // Changes the presence available contacts to unavailable
                try {
                    setOfflinePresences();
                }
                catch (NotConnectedException e) {
                    LOGGER.log(Level.SEVERE, "Not connected exception" ,e);
                }
            }

            public void connectionClosedOnError(Exception e) {
                // Changes the presence available contacts to unavailable
                try {
                    setOfflinePresences();
                }
                catch (NotConnectedException e1) {
                    LOGGER.log(Level.SEVERE, "Not connected exception" ,e);
                }
            }

        });
        // If the connection is already established, call reload
        if (connection.isAuthenticated()) {
            try {
                reload();
            }
            catch (SmackException e) {
                LOGGER.log(Level.SEVERE, "Could not reload Roster", e);
            }
        }
        connection.addConnectionListener(new AbstractConnectionListener() {
            public void authenticated(XMPPConnection connection) {
                // Anonymous users can't have a roster, but it is possible that a Roster instance is
                // retrieved if getRoster() is called *before* connect(). So we have to check here
                // again if it's an anonymous connection.
                if (connection.isAnonymous())
                    return;
                if (!connection.getConfiguration().isRosterLoadedAtLogin())
                    return;
                try {
                    Roster.this.reload();
                }
                catch (SmackException e) {
                    LOGGER.log(Level.SEVERE, "Could not reload Roster", e);
                    return;
                }
            }
        });
    }

    /**
     * Returns the subscription processing mode, which dictates what action
     * Smack will take when subscription requests from other users are made.
     * The default subscription mode is {@link SubscriptionMode#accept_all}.<p>
     * <p/>
     * If using the manual mode, a PacketListener should be registered that
     * listens for Presence packets that have a type of
     * {@link org.jivesoftware.smack.packet.Presence.Type#subscribe}.
     *
     * @return the subscription mode.
     */
    public SubscriptionMode getSubscriptionMode() {
        return subscriptionMode;
    }

    /**
     * Sets the subscription processing mode, which dictates what action
     * Smack will take when subscription requests from other users are made.
     * The default subscription mode is {@link SubscriptionMode#accept_all}.<p>
     * <p/>
     * If using the manual mode, a PacketListener should be registered that
     * listens for Presence packets that have a type of
     * {@link org.jivesoftware.smack.packet.Presence.Type#subscribe}.
     *
     * @param subscriptionMode the subscription mode.
     */
    public void setSubscriptionMode(SubscriptionMode subscriptionMode) {
        this.subscriptionMode = subscriptionMode;
    }

    /**
     * Reloads the entire roster from the server. This is an asynchronous operation,
     * which means the method will return immediately, and the roster will be
     * reloaded at a later point when the server responds to the reload request.
     * @throws NotLoggedInException If not logged in.
     * @throws NotConnectedException 
     */
    public void reload() throws NotLoggedInException, NotConnectedException{
        if (!connection.isAuthenticated()) {
            throw new NotLoggedInException();
        }
        if (connection.isAnonymous()) {
            throw new IllegalStateException("Anonymous users can't have a roster.");
        }

        RosterPacket packet = new RosterPacket();
        if (rosterStore != null && connection.isRosterVersioningSupported()) {
            packet.setVersion(rosterStore.getRosterVersion());
        }
        PacketFilter filter = new IQReplyFilter(packet, connection);
        connection.addPacketListener(new RosterResultListener(), filter);
        connection.sendPacket(packet);
    }

    /**
     * Adds a listener to this roster. The listener will be fired anytime one or more
     * changes to the roster are pushed from the server.
     *
     * @param rosterListener a roster listener.
     */
    public void addRosterListener(RosterListener rosterListener) {
        if (!rosterListeners.contains(rosterListener)) {
            rosterListeners.add(rosterListener);
        }
    }

    /**
     * Removes a listener from this roster. The listener will be fired anytime one or more
     * changes to the roster are pushed from the server.
     *
     * @param rosterListener a roster listener.
     */
    public void removeRosterListener(RosterListener rosterListener) {
        rosterListeners.remove(rosterListener);
    }

    /**
     * Creates a new group.<p>
     * <p/>
     * Note: you must add at least one entry to the group for the group to be kept
     * after a logout/login. This is due to the way that XMPP stores group information.
     *
     * @param name the name of the group.
     * @return a new group, or null if the group already exists
     * @throws IllegalStateException if logged in anonymously
     */
    public RosterGroup createGroup(String name) {
        if (connection.isAnonymous()) {
            throw new IllegalStateException("Anonymous users can't have a roster.");
        }
        if (groups.containsKey(name)) {
            return groups.get(name);
        }
        
        RosterGroup group = new RosterGroup(name, connection);
        groups.put(name, group);
        return group;
    }

    /**
     * Creates a new roster entry and presence subscription. The server will asynchronously
     * update the roster with the subscription status.
     *
     * @param user   the user. (e.g. johndoe@jabber.org)
     * @param name   the nickname of the user.
     * @param groups the list of group names the entry will belong to, or <tt>null</tt> if the
     *               the roster entry won't belong to a group.
     * @throws NoResponseException if there was no response from the server.
     * @throws XMPPErrorException if an XMPP exception occurs.
     * @throws NotLoggedInException If not logged in.
     * @throws NotConnectedException 
     */
    public void createEntry(String user, String name, String[] groups) throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException {
        if (!connection.isAuthenticated()) {
            throw new NotLoggedInException();
        }
        if (connection.isAnonymous()) {
            throw new IllegalStateException("Anonymous users can't have a roster.");
        }

        // Create and send roster entry creation packet.
        RosterPacket rosterPacket = new RosterPacket();
        rosterPacket.setType(IQ.Type.SET);
        RosterPacket.Item item = new RosterPacket.Item(user, name);
        if (groups != null) {
            for (String group : groups) {
                if (group != null && group.trim().length() > 0) {
                    item.addGroupName(group);
                }
            }
        }
        rosterPacket.addRosterItem(item);
        connection.createPacketCollectorAndSend(rosterPacket).nextResultOrThrow();

        // Create a presence subscription packet and send.
        Presence presencePacket = new Presence(Presence.Type.subscribe);
        presencePacket.setTo(user);
        connection.sendPacket(presencePacket);
    }

    /**
     * Removes a roster entry from the roster. The roster entry will also be removed from the
     * unfiled entries or from any roster group where it could belong and will no longer be part
     * of the roster. Note that this is a synchronous call -- Smack must wait for the server
     * to send an updated subscription status.
     *
     * @param entry a roster entry.
     * @throws XMPPErrorException if an XMPP error occurs.
     * @throws NotLoggedInException if not logged in.
     * @throws NoResponseException SmackException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws IllegalStateException if connection is not logged in or logged in anonymously
     */
    public void removeEntry(RosterEntry entry) throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException {
        if (!connection.isAuthenticated()) {
            throw new NotLoggedInException();
        }
        if (connection.isAnonymous()) {
            throw new IllegalStateException("Anonymous users can't have a roster.");
        }

        // Only remove the entry if it's in the entry list.
        // The actual removal logic takes place in RosterPacketListenerprocess>>Packet(Packet)
        if (!entries.containsKey(entry.getUser())) {
            return;
        }
        RosterPacket packet = new RosterPacket();
        packet.setType(IQ.Type.SET);
        RosterPacket.Item item = RosterEntry.toRosterItem(entry);
        // Set the item type as REMOVE so that the server will delete the entry
        item.setItemType(RosterPacket.ItemType.remove);
        packet.addRosterItem(item);
        connection.createPacketCollectorAndSend(packet).nextResultOrThrow();
    }

    /**
     * Returns a count of the entries in the roster.
     *
     * @return the number of entries in the roster.
     */
    public int getEntryCount() {
        return getEntries().size();
    }

    /**
     * Returns an unmodifiable collection of all entries in the roster, including entries
     * that don't belong to any groups.
     *
     * @return all entries in the roster.
     */
    public Collection<RosterEntry> getEntries() {
        Set<RosterEntry> allEntries = new HashSet<RosterEntry>();
        // Loop through all roster groups and add their entries to the answer
        for (RosterGroup rosterGroup : getGroups()) {
            allEntries.addAll(rosterGroup.getEntries());
        }
        // Add the roster unfiled entries to the answer
        allEntries.addAll(unfiledEntries);

        return Collections.unmodifiableCollection(allEntries);
    }

    /**
     * Returns a count of the unfiled entries in the roster. An unfiled entry is
     * an entry that doesn't belong to any groups.
     *
     * @return the number of unfiled entries in the roster.
     */
    public int getUnfiledEntryCount() {
        return unfiledEntries.size();
    }

    /**
     * Returns an unmodifiable collection for the unfiled roster entries. An unfiled entry is
     * an entry that doesn't belong to any groups.
     *
     * @return the unfiled roster entries.
     */
    public Collection<RosterEntry> getUnfiledEntries() {
        return Collections.unmodifiableList(unfiledEntries);
    }

    /**
     * Returns the roster entry associated with the given XMPP address or
     * <tt>null</tt> if the user is not an entry in the roster.
     *
     * @param user the XMPP address of the user (eg "jsmith@example.com"). The address could be
     *             in any valid format (e.g. "domain/resource", "user@domain" or "user@domain/resource").
     * @return the roster entry or <tt>null</tt> if it does not exist.
     */
    public RosterEntry getEntry(String user) {
        if (user == null) {
            return null;
        }
        return entries.get(user.toLowerCase(Locale.US));
    }

    /**
     * Returns true if the specified XMPP address is an entry in the roster.
     *
     * @param user the XMPP address of the user (eg "jsmith@example.com"). The
     *             address could be in any valid format (e.g. "domain/resource",
     *             "user@domain" or "user@domain/resource").
     * @return true if the XMPP address is an entry in the roster.
     */
    public boolean contains(String user) {
        return getEntry(user) != null;
    }

    /**
     * Returns the roster group with the specified name, or <tt>null</tt> if the
     * group doesn't exist.
     *
     * @param name the name of the group.
     * @return the roster group with the specified name.
     */
    public RosterGroup getGroup(String name) {
        return groups.get(name);
    }

    /**
     * Returns the number of the groups in the roster.
     *
     * @return the number of groups in the roster.
     */
    public int getGroupCount() {
        return groups.size();
    }

    /**
     * Returns an unmodifiable collections of all the roster groups.
     *
     * @return an iterator for all roster groups.
     */
    public Collection<RosterGroup> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    /**
     * Returns the presence info for a particular user. If the user is offline, or
     * if no presence data is available (such as when you are not subscribed to the
     * user's presence updates), unavailable presence will be returned.<p>
     * <p/>
     * If the user has several presences (one for each resource), then the presence with
     * highest priority will be returned. If multiple presences have the same priority,
     * the one with the "most available" presence mode will be returned. In order,
     * that's {@link org.jivesoftware.smack.packet.Presence.Mode#chat free to chat},
     * {@link org.jivesoftware.smack.packet.Presence.Mode#available available},
     * {@link org.jivesoftware.smack.packet.Presence.Mode#away away},
     * {@link org.jivesoftware.smack.packet.Presence.Mode#xa extended away}, and
     * {@link org.jivesoftware.smack.packet.Presence.Mode#dnd do not disturb}.<p>
     * <p/>
     * Note that presence information is received asynchronously. So, just after logging
     * in to the server, presence values for users in the roster may be unavailable
     * even if they are actually online. In other words, the value returned by this
     * method should only be treated as a snapshot in time, and may not accurately reflect
     * other user's presence instant by instant. If you need to track presence over time,
     * such as when showing a visual representation of the roster, consider using a
     * {@link RosterListener}.
     *
     * @param user an XMPP ID. The address could be in any valid format (e.g.
     *             "domain/resource", "user@domain" or "user@domain/resource"). Any resource
     *             information that's part of the ID will be discarded.
     * @return the user's current presence, or unavailable presence if the user is offline
     *         or if no presence information is available..
     */
    public Presence getPresence(String user) {
        String key = getPresenceMapKey(StringUtils.parseBareAddress(user));
        Map<String, Presence> userPresences = presenceMap.get(key);
        if (userPresences == null) {
            Presence presence = new Presence(Presence.Type.unavailable);
            presence.setFrom(user);
            return presence;
        }
        else {
            // Find the resource with the highest priority
            // Might be changed to use the resource with the highest availability instead.
            Presence presence = null;

            for (String resource : userPresences.keySet()) {
                Presence p = userPresences.get(resource);
                if (!p.isAvailable()) {
                    continue;
                }
                // Chose presence with highest priority first.
                if (presence == null || p.getPriority() > presence.getPriority()) {
                    presence = p;
                }
                // If equal priority, choose "most available" by the mode value.
                else if (p.getPriority() == presence.getPriority()) {
                    Presence.Mode pMode = p.getMode();
                    // Default to presence mode of available.
                    if (pMode == null) {
                        pMode = Presence.Mode.available;
                    }
                    Presence.Mode presenceMode = presence.getMode();
                    // Default to presence mode of available.
                    if (presenceMode == null) {
                        presenceMode = Presence.Mode.available;
                    }
                    if (pMode.compareTo(presenceMode) < 0) {
                        presence = p;
                    }
                }
            }
            if (presence == null) {
                presence = new Presence(Presence.Type.unavailable);
                presence.setFrom(user);
                return presence;
            }
            else {
                return presence;
            }
        }
    }

    /**
     * Returns the presence info for a particular user's resource, or unavailable presence
     * if the user is offline or if no presence information is available, such as
     * when you are not subscribed to the user's presence updates.
     *
     * @param userWithResource a fully qualified XMPP ID including a resource (user@domain/resource).
     * @return the user's current presence, or unavailable presence if the user is offline
     *         or if no presence information is available.
     */
    public Presence getPresenceResource(String userWithResource) {
        String key = getPresenceMapKey(userWithResource);
        String resource = StringUtils.parseResource(userWithResource);
        Map<String, Presence> userPresences = presenceMap.get(key);
        if (userPresences == null) {
            Presence presence = new Presence(Presence.Type.unavailable);
            presence.setFrom(userWithResource);
            return presence;
        }
        else {
            Presence presence = userPresences.get(resource);
            if (presence == null) {
                presence = new Presence(Presence.Type.unavailable);
                presence.setFrom(userWithResource);
                return presence;
            }
            else {
                return presence;
            }
        }
    }

    /**
     * Returns a List of Presence objects for all of a user's current presences
     * or an unavailable presence if the user is unavailable (offline) or if no presence
     * information is available, such as when you are not subscribed to the user's presence
     * updates.
     *
     * @param user a XMPP ID, e.g. jdoe@example.com.
     * @return a List of Presence objects for all the user's current presences,
     *         or an unavailable presence if the user is offline or if no presence information
     *         is available.
     */
    public List<Presence> getPresences(String user) {
        List<Presence> res;
        String key = getPresenceMapKey(user);
        Map<String, Presence> userPresences = presenceMap.get(key);
        if (userPresences == null) {
            Presence presence = new Presence(Presence.Type.unavailable);
            presence.setFrom(user);
            res = Arrays.asList(presence);
        }
        else {
            List<Presence> answer = new ArrayList<Presence>();
            for (Presence presence : userPresences.values()) {
                if (presence.isAvailable()) {
                    answer.add(presence);
                }
            }
            if (!answer.isEmpty()) {
                res = answer;
            }
            else {
                Presence presence = new Presence(Presence.Type.unavailable);
                presence.setFrom(user);
                res = Arrays.asList(presence);
            }
        }
        return Collections.unmodifiableList(res);
    }

    /**
     * Returns the key to use in the presenceMap for a fully qualified XMPP ID.
     * The roster can contain any valid address format such us "domain/resource",
     * "user@domain" or "user@domain/resource". If the roster contains an entry
     * associated with the fully qualified XMPP ID then use the fully qualified XMPP
     * ID as the key in presenceMap, otherwise use the bare address. Note: When the
     * key in presenceMap is a fully qualified XMPP ID, the userPresences is useless
     * since it will always contain one entry for the user.
     *
     * @param user the bare or fully qualified XMPP ID, e.g. jdoe@example.com or
     *             jdoe@example.com/Work.
     * @return the key to use in the presenceMap for the fully qualified XMPP ID.
     */
    private String getPresenceMapKey(String user) {
        if (user == null) {
            return null;
        }
        String key = user;
        if (!contains(user)) {
            key = StringUtils.parseBareAddress(user);
        }
        return key.toLowerCase(Locale.US);
    }

    /**
     * Changes the presence of available contacts offline by simulating an unavailable
     * presence sent from the server. After a disconnection, every Presence is set
     * to offline.
     * @throws NotConnectedException 
     */
    private void setOfflinePresences() throws NotConnectedException {
        Presence packetUnavailable;
        for (String user : presenceMap.keySet()) {
            Map<String, Presence> resources = presenceMap.get(user);
            if (resources != null) {
                for (String resource : resources.keySet()) {
                    packetUnavailable = new Presence(Presence.Type.unavailable);
                    packetUnavailable.setFrom(user + "/" + resource);
                    presencePacketListener.processPacket(packetUnavailable);
                }
            }
        }
    }

    /**
     * Fires roster changed event to roster listeners indicating that the
     * specified collections of contacts have been added, updated or deleted
     * from the roster.
     *
     * @param addedEntries   the collection of address of the added contacts.
     * @param updatedEntries the collection of address of the updated contacts.
     * @param deletedEntries the collection of address of the deleted contacts.
     */
    private void fireRosterChangedEvent(Collection<String> addedEntries, Collection<String> updatedEntries,
            Collection<String> deletedEntries) {
        for (RosterListener listener : rosterListeners) {
            if (!addedEntries.isEmpty()) {
                listener.entriesAdded(addedEntries);
            }
            if (!updatedEntries.isEmpty()) {
                listener.entriesUpdated(updatedEntries);
            }
            if (!deletedEntries.isEmpty()) {
                listener.entriesDeleted(deletedEntries);
            }
        }
    }

    /**
     * Fires roster presence changed event to roster listeners.
     *
     * @param presence the presence change.
     */
    private void fireRosterPresenceEvent(Presence presence) {
        for (RosterListener listener : rosterListeners) {
            listener.presenceChanged(presence);
        }
    }

    private void addUpdateEntry(Collection<String> addedEntries, Collection<String> updatedEntries,
                    Collection<String> unchangedEntries, RosterPacket.Item item, RosterEntry entry) {
        RosterEntry oldEntry = entries.put(item.getUser(), entry);
        if (oldEntry == null) {
            addedEntries.add(item.getUser());
        }
        else {
            RosterPacket.Item oldItem = RosterEntry.toRosterItem(oldEntry);
            if (!oldEntry.equalsDeep(entry) || !item.getGroupNames().equals(oldItem.getGroupNames())) {
                updatedEntries.add(item.getUser());
            } else {
                // Record the entry as unchanged, so that it doesn't end up as deleted entry
                unchangedEntries.add(item.getUser());
            }
        }

        // Mark the entry as unfiled if it does not belong to any groups.
        if (item.getGroupNames().isEmpty()) {
            unfiledEntries.remove(entry);
            unfiledEntries.add(entry);
        }
        else {
            unfiledEntries.remove(entry);
        }

        // Add the user to the new groups

        // Add the entry to the groups
        List<String> newGroupNames = new ArrayList<String>();
        for (String groupName : item.getGroupNames()) {
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

        // Remove user from the remaining groups.
        List<String> oldGroupNames = new ArrayList<String>();
        for (RosterGroup group: getGroups()) {
            oldGroupNames.add(group.getName());
        }
        oldGroupNames.removeAll(newGroupNames);

        for (String groupName : oldGroupNames) {
            RosterGroup group = getGroup(groupName);
            group.removeEntryLocal(entry);
            if (group.getEntryCount() == 0) {
                groups.remove(groupName);
            }
        }
    }

    private void deleteEntry(Collection<String> deletedEntries, RosterEntry entry) {
        String user = entry.getUser();
        entries.remove(user);
        unfiledEntries.remove(entry);
        presenceMap.remove(StringUtils.parseBareAddress(user));
        deletedEntries.add(user);

        for (Entry<String,RosterGroup> e: groups.entrySet()) {
            RosterGroup group = e.getValue();
            group.removeEntryLocal(entry);
            if (group.getEntryCount() == 0) {
                groups.remove(e.getKey());
            }
        }
    }


    /**
     * Removes all the groups with no entries.
     *
     * This is used by {@link RosterPushListener} and {@link RosterResultListener} to
     * cleanup groups after removing contacts.
     */
    private void removeEmptyGroups() {
        // We have to do this because RosterGroup.removeEntry removes the entry immediately
        // (locally) and the group could remain empty.
        // TODO Check the performance/logic for rosters with large number of groups
        for (RosterGroup group : getGroups()) {
            if (group.getEntryCount() == 0) {
                groups.remove(group.getName());
            }
        }
    }

    /**
     * Ignore ItemTypes as of RFC 6121, 2.1.2.5.
     *
     * This is used by {@link RosterPushListener} and {@link RosterResultListener}.
     * */
    private static boolean hasValidSubscriptionType(RosterPacket.Item item) {
        return item.getItemType().equals(RosterPacket.ItemType.none)
                || item.getItemType().equals(RosterPacket.ItemType.from)
                || item.getItemType().equals(RosterPacket.ItemType.to)
                || item.getItemType().equals(RosterPacket.ItemType.both);
    }

    /**
     * An enumeration for the subscription mode options.
     */
    public enum SubscriptionMode {

        /**
         * Automatically accept all subscription and unsubscription requests. This is
         * the default mode and is suitable for simple client. More complex client will
         * likely wish to handle subscription requests manually.
         */
        accept_all,

        /**
         * Automatically reject all subscription requests.
         */
        reject_all,

        /**
         * Subscription requests are ignored, which means they must be manually
         * processed by registering a listener for presence packets and then looking
         * for any presence requests that have the type Presence.Type.SUBSCRIBE or
         * Presence.Type.UNSUBSCRIBE.
         */
        manual
    }

    /**
     * Listens for all presence packets and processes them.
     */
    private class PresencePacketListener implements PacketListener {

        public void processPacket(Packet packet) throws NotConnectedException {
            Presence presence = (Presence) packet;
            String from = presence.getFrom();
            String key = getPresenceMapKey(from);

            // If an "available" presence, add it to the presence map. Each presence
            // map will hold for a particular user a map with the presence
            // packets saved for each resource.
            if (presence.getType() == Presence.Type.available) {
                Map<String, Presence> userPresences;
                // Get the user presence map
                if (presenceMap.get(key) == null) {
                    userPresences = new ConcurrentHashMap<String, Presence>();
                    presenceMap.put(key, userPresences);
                }
                else {
                    userPresences = presenceMap.get(key);
                }
                // See if an offline presence was being stored in the map. If so, remove
                // it since we now have an online presence.
                userPresences.remove("");
                // Add the new presence, using the resources as a key.
                userPresences.put(StringUtils.parseResource(from), presence);
                // If the user is in the roster, fire an event.
                RosterEntry entry = entries.get(key);
                if (entry != null) {
                    fireRosterPresenceEvent(presence);
                }
            }
            // If an "unavailable" packet.
            else if (presence.getType() == Presence.Type.unavailable) {
                // If no resource, this is likely an offline presence as part of
                // a roster presence flood. In that case, we store it.
                if ("".equals(StringUtils.parseResource(from))) {
                    Map<String, Presence> userPresences;
                    // Get the user presence map
                    if (presenceMap.get(key) == null) {
                        userPresences = new ConcurrentHashMap<String, Presence>();
                        presenceMap.put(key, userPresences);
                    }
                    else {
                        userPresences = presenceMap.get(key);
                    }
                    userPresences.put("", presence);
                }
                // Otherwise, this is a normal offline presence.
                else if (presenceMap.get(key) != null) {
                    Map<String, Presence> userPresences = presenceMap.get(key);
                    // Store the offline presence, as it may include extra information
                    // such as the user being on vacation.
                    userPresences.put(StringUtils.parseResource(from), presence);
                }
                // If the user is in the roster, fire an event.
                RosterEntry entry = entries.get(key);
                if (entry != null) {
                    fireRosterPresenceEvent(presence);
                }
            }
            else if (presence.getType() == Presence.Type.subscribe) {
                Presence response = null;
                switch (subscriptionMode) {
                case accept_all:
                    // Accept all subscription requests.
                    response = new Presence(Presence.Type.subscribed);
                    break;
                case reject_all:
                    // Reject all subscription requests.
                    response = new Presence(Presence.Type.unsubscribed);
                    break;
                case manual:
                default:
                    // Otherwise, in manual mode so ignore.
                    break;
                }
                if (response != null) {
                    response.setTo(presence.getFrom());
                    connection.sendPacket(response);
                }
            }
            else if (presence.getType() == Presence.Type.unsubscribe) {
                if (subscriptionMode != SubscriptionMode.manual) {
                    // Acknowledge and accept unsubscription notification so that the
                    // server will stop sending notifications saying that the contact
                    // has unsubscribed to our presence.
                    Presence response = new Presence(Presence.Type.unsubscribed);
                    response.setTo(presence.getFrom());
                    connection.sendPacket(response);
                }
                // Otherwise, in manual mode so ignore.
            }
            // Error presence packets from a bare JID mean we invalidate all existing
            // presence info for the user.
            else if (presence.getType() == Presence.Type.error &&
                    "".equals(StringUtils.parseResource(from)))
            {
                Map<String, Presence> userPresences;
                if (!presenceMap.containsKey(key)) {
                    userPresences = new ConcurrentHashMap<String, Presence>();
                    presenceMap.put(key, userPresences);
                }
                else {
                    userPresences = presenceMap.get(key);
                    // Any other presence data is invalidated by the error packet.
                    userPresences.clear();
                }
                // Set the new presence using the empty resource as a key.
                userPresences.put("", presence);
                // If the user is in the roster, fire an event.
                RosterEntry entry = entries.get(key);
                if (entry != null) {
                    fireRosterPresenceEvent(presence);
                }
            }
        }
    }

    /**
     * Handles the case of the empty IQ-result for roster versioning.
     *
     * Intended to listen for a concrete roster result and deregisters
     * itself after a processed packet.
     */
    private class RosterResultListener implements PacketListener {

        @Override
        public void processPacket(Packet packet) {
            connection.removePacketListener(this);

            IQ result = (IQ)packet;
            if (!result.getType().equals(IQ.Type.RESULT)) {
                LOGGER.severe("Roster result IQ not of type result. Packet: " + result.toXML());
                return;
            }

            Collection<String> addedEntries = new ArrayList<String>();
            Collection<String> updatedEntries = new ArrayList<String>();
            Collection<String> deletedEntries = new ArrayList<String>();
            Collection<String> unchangedEntries = new ArrayList<String>();

            if (packet instanceof RosterPacket) {
                // Non-empty roster result. This stanza contains all the roster elements.
                RosterPacket rosterPacket = (RosterPacket) packet;

                String version = rosterPacket.getVersion();

                // Ignore items without valid subscription type
                ArrayList<Item> validItems = new ArrayList<RosterPacket.Item>();
                for (RosterPacket.Item item : rosterPacket.getRosterItems()) {
                    if (hasValidSubscriptionType(item)) {
                        validItems.add(item);
                    }
                }

                for (RosterPacket.Item item : validItems) {
                    RosterEntry entry = new RosterEntry(item.getUser(), item.getName(),
                            item.getItemType(), item.getItemStatus(), Roster.this, connection);
                    addUpdateEntry(addedEntries, updatedEntries, unchangedEntries, item, entry);
                }

                // Delete all entries which where not added or updated
                Set<String> toDelete = new HashSet<String>();
                for (RosterEntry entry : entries.values()) {
                    toDelete.add(entry.getUser());
                }
                toDelete.removeAll(addedEntries);
                toDelete.removeAll(updatedEntries);
                toDelete.removeAll(unchangedEntries);
                for (String user : toDelete) {
                    deleteEntry(deletedEntries, entries.get(user));
                }

                if (rosterStore != null) {
                    rosterStore.resetEntries(validItems, version);
                }

                removeEmptyGroups();
            }
            else {
                // Empty roster result as defined in RFC6121 2.6.3. An empty roster result basically
                // means that rosterver was used and the roster hasn't changed (much) since the
                // version we presented the server. So we simply load the roster from the store and
                // await possible further roster pushes.
                for (RosterPacket.Item item : rosterStore.getEntries()) {
                    RosterEntry entry = new RosterEntry(item.getUser(), item.getName(),
                            item.getItemType(), item.getItemStatus(), Roster.this, connection);
                    addUpdateEntry(addedEntries, updatedEntries, unchangedEntries, item, entry);
                }
            }

            rosterInitialized = true;
            synchronized (Roster.this) {
                Roster.this.notifyAll();
            }
            // Fire event for roster listeners.
            fireRosterChangedEvent(addedEntries, updatedEntries, deletedEntries);
        }
    }

    /**
     * Listens for all roster pushes and processes them.
     */
    private class RosterPushListener implements PacketListener {

        public void processPacket(Packet packet) throws NotConnectedException {
            RosterPacket rosterPacket = (RosterPacket) packet;

            String version = rosterPacket.getVersion();

            // Roster push (RFC 6121, 2.1.6)
            // A roster push with a non-empty from not matching our address MUST be ignored
            String jid = StringUtils.parseBareAddress(connection.getUser());
            if (rosterPacket.getFrom() != null &&
                    !rosterPacket.getFrom().equals(jid)) {
                LOGGER.warning("Ignoring roster push with a non matching 'from' ourJid=" + jid
                                + " from=" + rosterPacket.getFrom());
                return;
            }

            // A roster push must contain exactly one entry
            Collection<Item> items = rosterPacket.getRosterItems();
            if (items.size() != 1) {
                LOGGER.warning("Ignoring roster push with not exaclty one entry. size=" + items.size());
                return;
            }

            Collection<String> addedEntries = new ArrayList<String>();
            Collection<String> updatedEntries = new ArrayList<String>();
            Collection<String> deletedEntries = new ArrayList<String>();
            Collection<String> unchangedEntries = new ArrayList<String>();

            // We assured abouve that the size of items is exaclty 1, therefore we are able to
            // safely retrieve this single item here.
            Item item = items.iterator().next();
            RosterEntry entry = new RosterEntry(item.getUser(), item.getName(),
                            item.getItemType(), item.getItemStatus(), Roster.this, connection);

            if (item.getItemType().equals(RosterPacket.ItemType.remove)) {
                deleteEntry(deletedEntries, entry);
                if (rosterStore != null) {
                    rosterStore.removeEntry(entry.getUser(), version);
                }
            }
            else if (hasValidSubscriptionType(item)) {
                addUpdateEntry(addedEntries, updatedEntries, unchangedEntries, item, entry);
                if (rosterStore != null) {
                    rosterStore.addEntry(item, version);
                }
            }
            connection.sendPacket(IQ.createResultIQ(rosterPacket));

            removeEmptyGroups();

            // Fire event for roster listeners.
            fireRosterChangedEvent(addedEntries, updatedEntries, deletedEntries);
        }
    }
}
