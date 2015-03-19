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

package org.jivesoftware.smack.roster.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents XMPP roster packets.
 *
 * @author Matt Tucker
 */
public class RosterPacket extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = "jabber:iq:roster";

    private final List<Item> rosterItems = new ArrayList<Item>();
    private String rosterVersion;

    public RosterPacket() {
        super(ELEMENT, NAMESPACE);
    }

    /**
     * Adds a roster item to the packet.
     *
     * @param item a roster item.
     */
    public void addRosterItem(Item item) {
        synchronized (rosterItems) {
            rosterItems.add(item);
        }
    }

    /**
     * Returns the number of roster items in this roster packet.
     *
     * @return the number of roster items.
     */
    public int getRosterItemCount() {
        synchronized (rosterItems) {
            return rosterItems.size();
        }
    }

    /**
     * Returns a copied list of the roster items in the packet.
     *
     * @return a copied list of the roster items in the packet.
     */
    public List<Item> getRosterItems() {
        synchronized (rosterItems) {
            return new ArrayList<Item>(rosterItems);
        }
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.optAttribute("ver", rosterVersion);
        buf.rightAngleBracket();

        synchronized (rosterItems) {
            for (Item entry : rosterItems) {
                buf.append(entry.toXML());
            }
        }
        return buf;
    }

    public String getVersion() {
        return rosterVersion;
    }

    public void setVersion(String version) {
        rosterVersion = version;
    }

    /**
     * A roster item, which consists of a JID, their name, the type of subscription, and
     * the groups the roster item belongs to.
     */
    public static class Item {

        public static final String GROUP = "group";

        private final Jid user;
        private String name;
        private ItemType itemType;
        private ItemStatus itemStatus;
        private boolean approved;
        private final Set<String> groupNames;

        /**
         * Creates a new roster item.
         *
         * @param user the user.
         * @param name the user's name.
         */
        public Item(Jid user, String name) {
            this.user = user;
            this.name = name;
            itemType = null;
            itemStatus = null;
            groupNames = new CopyOnWriteArraySet<String>();
        }

        /**
         * Returns the user.
         *
         * @return the user.
         */
        public Jid getUser() {
            return user;
        }

        /**
         * Returns the user's name.
         *
         * @return the user's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the user's name.
         *
         * @param name the user's name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns the roster item type.
         *
         * @return the roster item type.
         */
        public ItemType getItemType() {
            return itemType;
        }

        /**
         * Sets the roster item type.
         *
         * @param itemType the roster item type.
         */
        public void setItemType(ItemType itemType) {
            this.itemType = itemType;
        }

        /**
         * Returns the roster item status.
         *
         * @return the roster item status.
         */
        public ItemStatus getItemStatus() {
            return itemStatus;
        }

        /**
         * Sets the roster item status.
         *
         * @param itemStatus the roster item status.
         */
        public void setItemStatus(ItemStatus itemStatus) {
            this.itemStatus = itemStatus;
        }


        /**
         * Returns the roster item pre-approval state.
         *
         * @return the pre-approval state.
         */
        public boolean isApproved() {
            return approved;
        }

        /**
         * Sets the roster item pre-approval state.
         *
         * @param approved the pre-approval flag.
         */
        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        /**
         * Returns an unmodifiable set of the group names that the roster item
         * belongs to.
         *
         * @return an unmodifiable set of the group names.
         */
        public Set<String> getGroupNames() {
            return Collections.unmodifiableSet(groupNames);
        }

        /**
         * Adds a group name.
         *
         * @param groupName the group name.
         */
        public void addGroupName(String groupName) {
            groupNames.add(groupName);
        }

        /**
         * Removes a group name.
         *
         * @param groupName the group name.
         */
        public void removeGroupName(String groupName) {
            groupNames.remove(groupName);
        }

        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(Stanza.ITEM).attribute("jid", user);
            xml.optAttribute("name", name);
            xml.optAttribute("subscription", itemType);
            xml.optAttribute("ask", itemStatus);
            xml.optBooleanAttribute("approved", approved);
            xml.rightAngleBracket();

            for (String groupName : groupNames) {
                xml.openElement(GROUP).escape(groupName).closeElement(GROUP);
            }
            xml.closeElement(Stanza.ITEM);
            return xml;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((groupNames == null) ? 0 : groupNames.hashCode());
            result = prime * result + ((itemStatus == null) ? 0 : itemStatus.hashCode());
            result = prime * result + ((itemType == null) ? 0 : itemType.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((user == null) ? 0 : user.hashCode());
            result = prime * result + ((approved == false) ? 0 : 1);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Item other = (Item) obj;
            if (groupNames == null) {
                if (other.groupNames != null)
                    return false;
            }
            else if (!groupNames.equals(other.groupNames))
                return false;
            if (itemStatus != other.itemStatus)
                return false;
            if (itemType != other.itemType)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            if (user == null) {
                if (other.user != null)
                    return false;
            }
            else if (!user.equals(other.user))
                return false;
            if (approved != other.approved)
                return false;
            return true;
        }

    }

    /**
     * The subscription status of a roster item. An optional element that indicates
     * the subscription status if a change request is pending.
     */
    public static enum ItemStatus {
        /**
         * Request to subscribe
         */
        subscribe,

        /**
         * Request to unsubscribe
         */
        unsubscribe;

        public static final ItemStatus SUBSCRIPTION_PENDING = subscribe;
        public static final ItemStatus UNSUBSCRIPTION_PENDING = unsubscribe;

        public static ItemStatus fromString(String s) {
            if (s == null) {
                return null;
            }
            try {
                return ItemStatus.valueOf(s);
            }
            catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static enum ItemType {

        /**
         * The user does not have a subscription to the contact's presence, and the contact does not
         * have a subscription to the user's presence; this is the default value, so if the
         * subscription attribute is not included then the state is to be understood as "none".
         */
        none,

        /**
         * The user has a subscription to the contact's presence, but the contact does not have a
         * subscription to the user's presence.
         */
        to,

        /**
         * The contact has a subscription to the user's presence, but the user does not have a
         * subscription to the contact's presence.
         */
        from,

        /**
         * The user and the contact have subscriptions to each other's presence (also called a
         * "mutual subscription").
         */
        both,

        /**
         * The user wishes to stop receiving presence updates from the subscriber.
         */
        remove
    }
}
