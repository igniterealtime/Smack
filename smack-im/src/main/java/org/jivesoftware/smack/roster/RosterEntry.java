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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.EqualsUtil;

import org.jxmpp.jid.BareJid;


/**
 * Each user in your roster is represented by a roster entry, which contains the user's
 * JID and a name or nickname you assign.
 *
 * @author Matt Tucker
 * @author Florian Schmaus
 */
public final class RosterEntry extends Manager {

    private RosterPacket.Item item;
    private final Roster roster;

    /**
     * Creates a new roster entry.
     *
     * @param item the Roster Stanza's Item entry.
     * @param roster The Roster managing this entry.
     * @param connection a connection to the XMPP server.
     */
    RosterEntry(RosterPacket.Item item, Roster roster, XMPPConnection connection) {
        super(connection);
        this.item = item;
        this.roster = roster;
    }

    /**
     * Returns the JID of the user associated with this entry.
     *
     * @return the user associated with this entry.
     * @deprecated use {@link #getJid()} instead.
     */
    @Deprecated
    public String getUser() {
        return getJid().toString();
    }

    /**
     * Returns the JID associated with this entry.
     *
     * @return the user associated with this entry.
     */
    public BareJid getJid() {
        return item.getJid();
    }

    /**
     * Returns the name associated with this entry.
     *
     * @return the name.
     */
    public String getName() {
        return item.getName();
    }

    /**
     * Sets the name associated with this entry.
     *
     * @param name the name.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public synchronized void setName(String name) throws NotConnectedException, NoResponseException, XMPPErrorException, InterruptedException {
        // Do nothing if the name hasn't changed.
        if (name != null && name.equals(getName())) {
            return;
        }

        RosterPacket packet = new RosterPacket();
        packet.setType(IQ.Type.set);

        // Create a new roster item with the current RosterEntry and the *new* name. Note that we can't set the name of
        // RosterEntry right away, as otherwise the updated event wont get fired, because equalsDeep would return true.
        packet.addRosterItem(toRosterItem(this, name));
        connection().sendIqRequestAndWaitForResponse(packet);

        // We have received a result response to the IQ set, the name was successfully changed
        item.setName(name);
    }

    /**
     * Updates this entries item.
     *
     * @param item new item
     */
    void updateItem(RosterPacket.Item item) {
        assert item != null;
        this.item = item;
    }

    /**
     * Returns the pre-approval state of this entry.
     *
     * @return the pre-approval state.
     */
    public boolean isApproved() {
        return item.isApproved();
    }

    /**
     * Returns an copied list of the roster groups that this entry belongs to.
     *
     * @return an iterator for the groups this entry belongs to.
     */
    public List<RosterGroup> getGroups() {
        List<RosterGroup> results = new ArrayList<>();
        // Loop through all roster groups and find the ones that contain this
        // entry. This algorithm should be fine
        for (RosterGroup group : roster.getGroups()) {
            if (group.contains(this)) {
                results.add(group);
            }
        }
        return results;
    }

    /**
     * Returns the roster subscription type of the entry. When the type is
     * RosterPacket.ItemType.none or RosterPacket.ItemType.from,
     * refer to {@link RosterEntry getStatus()} to see if a subscription request
     * is pending.
     *
     * @return the type.
     */
    public RosterPacket.ItemType getType() {
        return item.getItemType();
    }

    /**
     * Returns the roster subscription request status of the entry. If
     * {@code true}, then the contact did not answer the subscription request
     * yet.
     *
     * @return the status.
     * @since 4.2
     */
    public boolean isSubscriptionPending() {
        return item.isSubscriptionPending();
    }

    /**
     * Check if the contact is subscribed to "my" presence. This allows the contact to see the presence information.
     *
     * @return true if the contact has a presence subscription.
     * @since 4.2
     */
    public boolean canSeeMyPresence() {
        switch (getType()) {
        case from:
        case both:
            return true;
        default:
            return false;
        }
    }

    /**
     * Check if we are subscribed to the contact's presence. If <code>true</code> then the contact has allowed us to
     * receive presence information.
     *
     * @return true if we are subscribed to the contact's presence.
     * @since 4.2
     */
    public boolean canSeeHisPresence() {
        switch (getType()) {
        case to:
        case both:
            return true;
        default:
            return false;
        }
    }

    /**
     * Cancel the presence subscription the XMPP entity representing this roster entry has with us.
     *
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @since 4.2
     */
    public void cancelSubscription() throws NotConnectedException, InterruptedException {
        XMPPConnection connection = connection();
        Presence unsubscribed = connection.getStanzaFactory().buildPresenceStanza()
                .to(item.getJid())
                .ofType(Presence.Type.unsubscribed)
                .build();
        connection.sendStanza(unsubscribed);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (getName() != null) {
            buf.append(getName()).append(": ");
        }
        buf.append(getJid());
        Collection<RosterGroup> groups = getGroups();
        if (!groups.isEmpty()) {
            buf.append(" [");
            Iterator<RosterGroup> iter = groups.iterator();
            RosterGroup group = iter.next();
            buf.append(group.getName());
            while (iter.hasNext()) {
            buf.append(", ");
                group = iter.next();
                buf.append(group.getName());
            }
            buf.append(']');
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        return getJid().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return EqualsUtil.equals(this, object, (e, o) ->
            e.append(getJid(), o.getJid())
        );
    }

    /**
     * Indicates whether some other object is "equal to" this by comparing all members.
     * <p>
     * The {@link #equals(Object)} method returns <code>true</code> if the user JIDs are equal.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj argument; <code>false</code>
     *         otherwise.
     */
    public boolean equalsDeep(Object obj) {
        return EqualsUtil.equals(this, obj, (e, o) ->
            e.append(item, o.item)
        );
    }

    /**
     * Convert the RosterEntry to a Roster stanza &lt;item/&gt; element.
     *
     * @param entry the roster entry.
     * @return the roster item.
     */
    static RosterPacket.Item toRosterItem(RosterEntry entry) {
        return toRosterItem(entry, entry.getName(), false);
    }

    /**
     * Convert the RosterEntry to a Roster stanza &lt;item/&gt; element.
     *
     * @param entry the roster entry
     * @param name the name of the roster item.
     * @return the roster item.
     */
    static RosterPacket.Item toRosterItem(RosterEntry entry, String name) {
        return toRosterItem(entry, name, false);
    }

    static RosterPacket.Item toRosterItem(RosterEntry entry, boolean includeAskAttribute) {
        return toRosterItem(entry, entry.getName(), includeAskAttribute);
    }

    /**
     * Convert a roster entry with the given name to a roster item. As per RFC 6121 § 2.1.2.2., clients MUST NOT include
     * the 'ask' attribute, thus set {@code includeAskAttribute} to {@code false}.
     *
     * @param entry the roster entry.
     * @param name the name of the roster item.
     * @param includeAskAttribute whether or not to include the 'ask' attribute.
     * @return the roster item.
     */
    private static RosterPacket.Item toRosterItem(RosterEntry entry, String name, boolean includeAskAttribute) {
        RosterPacket.Item item = new RosterPacket.Item(entry.getJid(), name);
        item.setItemType(entry.getType());
        if (includeAskAttribute) {
            item.setSubscriptionPending(entry.isSubscriptionPending());
        }
        item.setApproved(entry.isApproved());
        // Set the correct group names for the item.
        for (RosterGroup group : entry.getGroups()) {
            item.addGroupName(group.getName());
        }
        return item;
    }

}
