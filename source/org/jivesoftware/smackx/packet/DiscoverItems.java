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

package org.jivesoftware.smackx.packet;

import java.util.*;

import org.jivesoftware.smack.packet.IQ;

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

    private List items = new ArrayList();
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
    public Iterator getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(new ArrayList(items)).iterator();
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
        StringBuffer buf = new StringBuffer();
        buf.append("<query xmlns=\"http://jabber.org/protocol/disco#items\">");
        synchronized (items) {
            for (int i = 0; i < items.size(); i++) {
                Item item = (Item) items.get(i);
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
            StringBuffer buf = new StringBuffer();
            buf.append("<item jid=\"").append(entityID).append("\"");
            if (name != null) {
                buf.append(" name=\"").append(name).append("\"");
            }
            if (node != null) {
                buf.append(" node=\"").append(node).append("\"");
            }
            if (action != null) {
                buf.append(" action=\"").append(action).append("\"");
            }
            buf.append("/>");
            return buf.toString();
        }
    }
}