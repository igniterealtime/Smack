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

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents extended presence information about roles, affiliations, full JIDs, 
 * or status codes scoped by the 'http://jabber.org/protocol/muc#user' namespace.
 * 
 * @author Gaston Dombiak
 */
public class MUCUser implements PacketExtension {

    private String alt;
    private Invite invite;
    private Decline decline;
    private Item item;
    private String password;
    private Status status;

    public String getElementName() {
        return "x";
    }

    public String getNamespace() {
        return "http://jabber.org/protocol/muc#user";
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        if (getAlt() != null) {
            buf.append("<alt>").append(getAlt()).append("</alt>");
        }
        if (getInvite() != null) {
            buf.append(getInvite().toXML());
        }
        if (getDecline() != null) {
            buf.append(getDecline().toXML());
        }
        if (getItem() != null) {
            buf.append(getItem().toXML());
        }
        if (getPassword() != null) {
            buf.append("<password>").append(getPassword()).append("</password>");
        }
        if (getStatus() != null) {
            buf.append(getStatus().toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * @return
     */
    public String getAlt() {
        return alt;
    }

    /**
     * @return
     */
    public Invite getInvite() {
        return invite;
    }

    /**
     * @return
     */
    public Decline getDecline() {
        return decline;
    }

    /**
     * @return
     */
    public Item getItem() {
        return item;
    }

    /**
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param string
     */
    public void setAlt(String string) {
        alt = string;
    }

    /**
     * @param invite
     */
    public void setInvite(Invite invite) {
        this.invite = invite;
    }

    /**
     * @param decline
     */
    public void setDecline(Decline decline) {
        this.decline = decline;
    }

    /**
     * @param item
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     * @param string
     */
    public void setPassword(String string) {
        password = string;
    }

    /**
     * @param status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * 
     * Represents an invitation for another user to a room. The sender of the invitation
     * must be an occupant of the room. The invitation will be sent to the room which in turn
     * will forward the invitation to the invitee. 
     *
     * @author Gaston Dombiak
     */
    public static class Invite {
        private String reason;
        private String from;
        private String to;
        
        /**
         * @return
         */
        public String getFrom() {
            return from;
        }

        /**
         * @return
         */
        public String getReason() {
            return reason;
        }

        /**
         * @return
         */
        public String getTo() {
            return to;
        }

        /**
         * @param string
         */
        public void setFrom(String string) {
            from = string;
        }

        /**
         * @param string
         */
        public void setReason(String string) {
            reason = string;
        }

        /**
         * @param string
         */
        public void setTo(String string) {
            to = string;
        }

        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<invite ");
            if (getTo() != null) {
                buf.append(" to=\"").append(getTo()).append("\"");
            }
            if (getFrom() != null) {
                buf.append(" from=\"").append(getFrom()).append("\"");
            }
            buf.append(">");
            if (getReason() != null) {
                buf.append("<reason>").append(getReason()).append("</reason>");
            }
            buf.append("</invite>");
            return buf.toString();
        }
    };

    /**
     * 
     * Represents a rejection to an invitation from another user to a room. The rejection will be 
     * sent to the room which in turn will forward the refusal to the inviter. 
     *
     * @author Gaston Dombiak
     */
    public static class Decline {
        private String reason;
        private String from;
        private String to;
        
        /**
         * @return
         */
        public String getFrom() {
            return from;
        }

        /**
         * @return
         */
        public String getReason() {
            return reason;
        }

        /**
         * @return
         */
        public String getTo() {
            return to;
        }

        /**
         * @param string
         */
        public void setFrom(String string) {
            from = string;
        }

        /**
         * @param string
         */
        public void setReason(String string) {
            reason = string;
        }

        /**
         * @param string
         */
        public void setTo(String string) {
            to = string;
        }

        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<decline ");
            if (getTo() != null) {
                buf.append(" to=\"").append(getTo()).append("\"");
            }
            if (getFrom() != null) {
                buf.append(" from=\"").append(getFrom()).append("\"");
            }
            buf.append(">");
            if (getReason() != null) {
                buf.append("<reason>").append(getReason()).append("</reason>");
            }
            buf.append("</decline>");
            return buf.toString();
        }
    };

    /**
     * 
     * Item child that holds information about roles, affiliation, jids and nicks.
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
         * @param role the privilege level of an occupant within a room.
         */
        public Item(String affiliation, String role) {
            this.affiliation = affiliation;
            this.role = role;
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
     * 
     * Status code assists in presenting notification messages.
     *
     * @author Gaston Dombiak
     */
    public static class Status {
        private String code; 

        /**
         * 
         * @param code
         */
        public Status(String code) {
            this.code = code;
        }
                
        /**
         * @return
         */
        public String getCode() {
            return code;
        }

        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<status code=\"").append(getCode()).append("\"/>");
            return buf.toString();
        }
    };
}