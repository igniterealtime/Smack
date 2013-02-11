/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A DiscoverItems IQ packet, which is used by XMPP clients to request and receive items 
 * associated with XMPP entities.<p>
 * 
 * The items could also be queried in order to discover if they contain items inside. Some items 
 * may be addressable by its JID and others may require to be addressed by a JID and a node name.
 *
 * @author Gaston Dombiak
 */
public class DiscoverItems extends IQ {

    public static final String NAMESPACE = "http://jabber.org/protocol/disco#items";

    private final List<Item> items = new CopyOnWriteArrayList<Item>();
    private String node;

    /**
     * Adds a new item to the discovered information.
     * 
     * @param item the discovered entity's item
     */
    public void addItem(Item item) {
        synchronized (items) {
            items.add(item);
        }
    }

    /**
     * Returns the discovered items of the queried XMPP entity. 
     *
     * @return an Iterator on the discovered entity's items
     */
    public Iterator<DiscoverItems.Item> getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(items).iterator();
        }
    }

    /**
     * Returns the node attribute that supplements the 'jid' attribute. A node is merely 
     * something that is associated with a JID and for which the JID can provide information.<p> 
     * 
     * Node attributes SHOULD be used only when trying to provide or query information which 
     * is not directly addressable.
     *
     * @return the node attribute that supplements the 'jid' attribute
     */
    public String getNode() {
        return node;
    }

    /**
     * Sets the node attribute that supplements the 'jid' attribute. A node is merely 
     * something that is associated with a JID and for which the JID can provide information.<p> 
     * 
     * Node attributes SHOULD be used only when trying to provide or query information which 
     * is not directly addressable.
     * 
     * @param node the node attribute that supplements the 'jid' attribute
     */
    public void setNode(String node) {
        this.node = node;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"" + NAMESPACE + "\"");
        if (getNode() != null) {
            buf.append(" node=\"");
            buf.append(StringUtils.escapeForXML(getNode()));
            buf.append("\"");
        }
        buf.append(">");
        synchronized (items) {
            for (Item item : items) {
                buf.append(item.toXML());
            }
        }
        buf.append("</query>");
        return buf.toString();
    }

    /**
     * An item is associated with an XMPP Entity, usually thought of a children of the parent 
     * entity and normally are addressable as a JID.<p> 
     * 
     * An item associated with an entity may not be addressable as a JID. In order to handle 
     * such items, Service Discovery uses an optional 'node' attribute that supplements the 
     * 'jid' attribute.
     */
    public static class Item {

        /**
         * Request to create or update the item.
         */
        public static final String UPDATE_ACTION = "update";

        /**
         * Request to remove the item.
         */
        public static final String REMOVE_ACTION = "remove";

        private String entityID;
        private String name;
        private String node;
        private String action;

        /**
         * Create a new Item associated with a given entity.
         * 
         * @param entityID the id of the entity that contains the item
         */
        public Item(String entityID) {
            this.entityID = entityID;
        }

        /**
         * Returns the entity's ID.
         *
         * @return the entity's ID.
         */
        public String getEntityID() {
            return entityID;
        }

        /**
         * Returns the entity's name.
         *
         * @return the entity's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the entity's name.
         *
         * @param name the entity's name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns the node attribute that supplements the 'jid' attribute. A node is merely 
         * something that is associated with a JID and for which the JID can provide information.<p> 
         * 
         * Node attributes SHOULD be used only when trying to provide or query information which 
         * is not directly addressable.
         *
         * @return the node attribute that supplements the 'jid' attribute
         */
        public String getNode() {
            return node;
        }

        /**
         * Sets the node attribute that supplements the 'jid' attribute. A node is merely 
         * something that is associated with a JID and for which the JID can provide information.<p> 
         * 
         * Node attributes SHOULD be used only when trying to provide or query information which 
         * is not directly addressable.
         * 
         * @param node the node attribute that supplements the 'jid' attribute
         */
        public void setNode(String node) {
            this.node = node;
        }

        /**
         * Returns the action that specifies the action being taken for this item. Possible action 
         * values are: "update" and "remove". Update should either create a new entry if the node 
         * and jid combination does not already exist, or simply update an existing entry. If 
         * "remove" is used as the action, the item should be removed from persistent storage.
         *  
         * @return the action being taken for this item
         */
        public String getAction() {
            return action;
        }

        /**
         * Sets the action that specifies the action being taken for this item. Possible action 
         * values are: "update" and "remove". Update should either create a new entry if the node 
         * and jid combination does not already exist, or simply update an existing entry. If 
         * "remove" is used as the action, the item should be removed from persistent storage.
         * 
         * @param action the action being taken for this item
         */
        public void setAction(String action) {
            this.action = action;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<item jid=\"").append(entityID).append("\"");
            if (name != null) {
                buf.append(" name=\"").append(StringUtils.escapeForXML(name)).append("\"");
            }
            if (node != null) {
                buf.append(" node=\"").append(StringUtils.escapeForXML(node)).append("\"");
            }
            if (action != null) {
                buf.append(" action=\"").append(StringUtils.escapeForXML(action)).append("\"");
            }
            buf.append("/>");
            return buf.toString();
        }
    }
}
