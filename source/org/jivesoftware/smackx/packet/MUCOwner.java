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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * IQ packet that serves for granting and revoking ownership privileges, granting 
 * and revoking administrative privileges and destroying a room. All these operations 
 * are scoped by the 'http://jabber.org/protocol/muc#owner' namespace.
 * 
 * @author Gaston Dombiak
 */
public class MUCOwner extends IQ {

    private List items = new ArrayList();
    private Destroy destroy;

    /**
     * Returns an Iterator for item childs that holds information about affiliation, 
     * jids and nicks.
     * 
     * @return an Iterator for item childs that holds information about affiliation,
     *          jids and nicks.
     */
    public Iterator getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(new ArrayList(items)).iterator();
        }
    }

    /**
     * Returns a request to the server to destroy a room. The sender of the request
     * should be the room's owner. If the sender of the destroy request is not the room's owner
     * then the server will answer a "Forbidden" error.
     * 
     * @return a request to the server to destroy a room.
     */
    public Destroy getDestroy() {
        return destroy;
    }

    /**
     * Sets a request to the server to destroy a room. The sender of the request
     * should be the room's owner. If the sender of the destroy request is not the room's owner
     * then the server will answer a "Forbidden" error.
     * 
     * @param destroy the request to the server to destroy a room.
     */
    public void setDestroy(Destroy destroy) {
        this.destroy = destroy;
    }

    /**
     * Adds an item child that holds information about affiliation, jids and nicks.
     * 
     * @param item the item child that holds information about affiliation, jids and nicks.
     */
    public void addItem(Item item) {
        synchronized (items) {
            items.add(item);
        }
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"http://jabber.org/protocol/muc#owner\">");
        synchronized (items) {
            for (int i = 0; i < items.size(); i++) {
                Item item = (Item) items.get(i);
                buf.append(item.toXML());
            }
        }
        if (getDestroy() != null) {
            buf.append(getDestroy().toXML());
        }
        // Add packet extensions, if any are defined.
        buf.append(getExtensionsXML());
        buf.append("</query>");
        return buf.toString();
    }

    /**
     * Item child that holds information about affiliation, jids and nicks.
     *
     * @author Gaston Dombiak
     */
    public static class Item {
        
        private String actor;
        private String reason;
        private String affiliation;
        private String jid;
        private String nick;
        private String role;

        /**
         * Creates a new item child. 
         * 
         * @param affiliation the actor's affiliation to the room
         */
        public Item(String affiliation) {
            this.affiliation = affiliation;
        }
        
        /**
         * Returns the actor (JID of an occupant in the room) that was kicked or banned.
         * 
         * @return the JID of an occupant in the room that was kicked or banned.
         */
        public String getActor() {
            return actor;
        }

        /**
         * Returns the reason for the item child. The reason is optional and could be used to
         * explain the reason why a user (occupant) was kicked or banned.
         *  
         * @return the reason for the item child.
         */
        public String getReason() {
            return reason;
        }

        /**
         * Returns the occupant's affiliation to the room. The affiliation is a semi-permanent 
         * association or connection with a room. The possible affiliations are "owner", "admin", 
         * "member", and "outcast" (naturally it is also possible to have no affiliation). An 
         * affiliation lasts across a user's visits to a room.
         * 
         * @return the actor's affiliation to the room
         */
        public String getAffiliation() {
            return affiliation;
        }

        /**
         * Returns the <room@service/nick> by which an occupant is identified within the context 
         * of a room. If the room is non-anonymous, the JID will be included in the item. 
         * 
         * @return the room JID by which an occupant is identified within the room.
         */
        public String getJid() {
            return jid;
        }

        /**
         * Returns the new nickname of an occupant that is changing his/her nickname. The new 
         * nickname is sent as part of the unavailable presence.  
         * 
         * @return the new nickname of an occupant that is changing his/her nickname.
         */
        public String getNick() {
            return nick;
        }

        /**
         * Returns the temporary position or privilege level of an occupant within a room. The
         * possible roles are "moderator", "participant", and "visitor" (it is also possible to
         * have no defined role). A role lasts only for the duration of an occupant's visit to
         * a room.
         *
         * @return the privilege level of an occupant within a room.
         */
        public String getRole() {
            return role;
        }

        /**
         * Sets the actor (JID of an occupant in the room) that was kicked or banned.
         * 
         * @param actor the actor (JID of an occupant in the room) that was kicked or banned.
         */
        public void setActor(String actor) {
            this.actor = actor;
        }

        /**
         * Sets the reason for the item child. The reason is optional and could be used to
         * explain the reason why a user (occupant) was kicked or banned.
         * 
         * @param reason the reason why a user (occupant) was kicked or banned.
         */
        public void setReason(String reason) {
            this.reason = reason;
        }

        /**
         * Sets the <room@service/nick> by which an occupant is identified within the context 
         * of a room. If the room is non-anonymous, the JID will be included in the item.
         *  
         * @param jid the JID by which an occupant is identified within a room.
         */
        public void setJid(String jid) {
            this.jid = jid;
        }

        /**
         * Sets the new nickname of an occupant that is changing his/her nickname. The new 
         * nickname is sent as part of the unavailable presence.
         *   
         * @param nick the new nickname of an occupant that is changing his/her nickname.
         */
        public void setNick(String nick) {
            this.nick = nick;
        }

        /**
         * Sets the temporary position or privilege level of an occupant within a room. The
         * possible roles are "moderator", "participant", and "visitor" (it is also possible to
         * have no defined role). A role lasts only for the duration of an occupant's visit to
         * a room.
         *
         * @param role the new privilege level of an occupant within a room.
         */
        public void setRole(String role) {
            this.role = role;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<item");
            if (getAffiliation() != null) {
                buf.append(" affiliation=\"").append(getAffiliation()).append("\"");
            }
            if (getJid() != null) {
                buf.append(" jid=\"").append(getJid()).append("\"");
            }
            if (getNick() != null) {
                buf.append(" nick=\"").append(getNick()).append("\"");
            }
            if (getRole() != null) {
                buf.append(" role=\"").append(getRole()).append("\"");
            }
            if (getReason() == null && getActor() == null) {
                buf.append("/>");
            }
            else {
                buf.append(">");
                if (getReason() != null) {
                    buf.append("<reason>").append(getReason()).append("</reason>");
                }
                if (getActor() != null) {
                    buf.append("<actor jid=\"").append(getActor()).append("\"/>");
                }
                buf.append("</item>");
            }
            return buf.toString();
        }
    };

    /**
     * Represents a request to the server to destroy a room. The sender of the request
     * should be the room's owner. If the sender of the destroy request is not the room's owner
     * then the server will answer a "Forbidden" error.
     * 
     * @author Gaston Dombiak
     */
    public static class Destroy {
        private String reason;
        private String jid;
        
        
        /**
         * Returns the JID of an alternate location since the current room is being destroyed.
         * 
         * @return the JID of an alternate location.
         */
        public String getJid() {
            return jid;
        }

        /**
         * Returns the reason for the room destruction.
         * 
         * @return the reason for the room destruction.
         */
        public String getReason() {
            return reason;
        }

        /**
         * Sets the JID of an alternate location since the current room is being destroyed.
         * 
         * @param jid the JID of an alternate location.
         */
        public void setJid(String jid) {
            this.jid = jid;
        }

        /**
         * Sets the reason for the room destruction.
         * 
         * @param reason the reason for the room destruction.
         */
        public void setReason(String reason) {
            this.reason = reason;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<destroy");
            if (getJid() != null) {
                buf.append(" jid=\"").append(getJid()).append("\"");
            }
            if (getReason() == null) {
                buf.append("/>");
            }
            else {
                buf.append(">");
                if (getReason() != null) {
                    buf.append("<reason>").append(getReason()).append("</reason>");
                }
                buf.append("</destroy>");
            }
            return buf.toString();
        }

    }
}
