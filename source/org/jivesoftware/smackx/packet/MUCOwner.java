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
 * Represents .....
 * 
 * @author Gaston Dombiak
 */
public class MUCOwner extends IQ {

    private List items = new ArrayList();
    private Destroy destroy;

    public Iterator getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(new ArrayList(items)).iterator();
        }
    }

    public Destroy getDestroy() {
        return destroy;
    }

    public void setDestroy(Destroy destroy) {
        this.destroy = destroy;
    }

    public void addItem(Item item) {
        synchronized (items) {
            items.add(item);
        }
    }

    public String getChildElementXML() {
        StringBuffer buf = new StringBuffer();
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
     * 
     * Item child that holds information about affiliation, jids and nicks.
     *
     * @author Gaston Dombiak
     */
    public static class Item {
        
        // TODO repasar si los comentarios estan OK porque son copy&paste
        
        private String actor;
        private String reason;
        private String affiliation;
        private String jid;
        private String nick;
        
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

        public String toXML() {
            StringBuffer buf = new StringBuffer();
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

    public static class Destroy {
        private String reason;
        private String jid;
        
        
        /**
         * @return
         */
        public String getJid() {
            return jid;
        }

        /**
         * @return
         */
        public String getReason() {
            return reason;
        }

        /**
         * @param jid
         */
        public void setJid(String jid) {
            this.jid = jid;
        }

        /**
         * @param reason
         */
        public void setReason(String reason) {
            this.reason = reason;
        }

        public String toXML() {
            StringBuffer buf = new StringBuffer();
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
