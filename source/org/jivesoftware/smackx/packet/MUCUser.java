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

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents extended presence information about roles, affiliations, full JIDs,
 * or status codes scoped by the 'http://jabber.org/protocol/muc#user' namespace.
 *
 * @author Gaston Dombiak
 */
public class MUCUser implements PacketExtension {

    private Invite invite;
    private Decline decline;
    private Item item;
    private String password;
    private Status status;
    private Destroy destroy;

    public String getElementName() {
        return "x";
    }

    public String getNamespace() {
        return "http://jabber.org/protocol/muc#user";
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
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
        if (getDestroy() != null) {
            buf.append(getDestroy().toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * Returns the invitation for another user to a room. The sender of the invitation
     * must be an occupant of the room. The invitation will be sent to the room which in turn
     * will forward the invitation to the invitee.
     *
     * @return an invitation for another user to a room.
     */
    public Invite getInvite() {
        return invite;
    }

    /**
     * Returns the rejection to an invitation from another user to a room. The rejection will be
     * sent to the room which in turn will forward the refusal to the inviter.
     *
     * @return a rejection to an invitation from another user to a room.
     */
    public Decline getDecline() {
        return decline;
    }

    /**
     * Returns the item child that holds information about roles, affiliation, jids and nicks.
     *
     * @return an item child that holds information about roles, affiliation, jids and nicks.
     */
    public Item getItem() {
        return item;
    }

    /**
     * Returns the password to use to enter Password-Protected Room. A Password-Protected Room is
     * a room that a user cannot enter without first providing the correct password.
     *
     * @return the password to use to enter Password-Protected Room.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the status which holds a code that assists in presenting notification messages.
     *
     * @return the status which holds a code that assists in presenting notification messages.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the notification that the room has been destroyed. After a room has been destroyed,
     * the room occupants will receive a Presence packet of type 'unavailable' with the reason for
     * the room destruction if provided by the room owner.
     *
     * @return a notification that the room has been destroyed.
     */
    public Destroy getDestroy() {
        return destroy;
    }

    /**
     * Sets the invitation for another user to a room. The sender of the invitation
     * must be an occupant of the room. The invitation will be sent to the room which in turn
     * will forward the invitation to the invitee.
     *
     * @param invite the invitation for another user to a room.
     */
    public void setInvite(Invite invite) {
        this.invite = invite;
    }

    /**
     * Sets the rejection to an invitation from another user to a room. The rejection will be
     * sent to the room which in turn will forward the refusal to the inviter.
     *
     * @param decline the rejection to an invitation from another user to a room.
     */
    public void setDecline(Decline decline) {
        this.decline = decline;
    }

    /**
     * Sets the item child that holds information about roles, affiliation, jids and nicks.
     *
     * @param item the item child that holds information about roles, affiliation, jids and nicks.
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     * Sets the password to use to enter Password-Protected Room. A Password-Protected Room is
     * a room that a user cannot enter without first providing the correct password.
     *
     * @param string the password to use to enter Password-Protected Room.
     */
    public void setPassword(String string) {
        password = string;
    }

    /**
     * Sets the status which holds a code that assists in presenting notification messages.
     *
     * @param status the status which holds a code that assists in presenting notification
     * messages.
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Sets the notification that the room has been destroyed. After a room has been destroyed,
     * the room occupants will receive a Presence packet of type 'unavailable' with the reason for
     * the room destruction if provided by the room owner.
     *
     * @param destroy the notification that the room has been destroyed.
     */
    public void setDestroy(Destroy destroy) {
        this.destroy = destroy;
    }

    /**
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
         * Returns the bare JID of the inviter or, optionally, the room JID. (e.g.
         * 'crone1@shakespeare.lit/desktop').
         *
         * @return the room's occupant that sent the invitation.
         */
        public String getFrom() {
            return from;
        }

        /**
         * Returns the message explaining the invitation.
         *
         * @return the message explaining the invitation.
         */
        public String getReason() {
            return reason;
        }

        /**
         * Returns the bare JID of the invitee. (e.g. 'hecate@shakespeare.lit')
         *
         * @return the bare JID of the invitee.
         */
        public String getTo() {
            return to;
        }

        /**
         * Sets the bare JID of the inviter or, optionally, the room JID. (e.g.
         * 'crone1@shakespeare.lit/desktop')
         *
         * @param from the bare JID of the inviter or, optionally, the room JID.
         */
        public void setFrom(String from) {
            this.from = from;
        }

        /**
         * Sets the message explaining the invitation.
         *
         * @param reason the message explaining the invitation.
         */
        public void setReason(String reason) {
            this.reason = reason;
        }

        /**
         * Sets the bare JID of the invitee. (e.g. 'hecate@shakespeare.lit')
         *
         * @param to the bare JID of the invitee.
         */
        public void setTo(String to) {
            this.to = to;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
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
    }

    /**
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
         * Returns the bare JID of the invitee that rejected the invitation. (e.g.
         * 'crone1@shakespeare.lit/desktop').
         *
         * @return the bare JID of the invitee that rejected the invitation.
         */
        public String getFrom() {
            return from;
        }

        /**
         * Returns the message explaining why the invitation was rejected.
         *
         * @return the message explaining the reason for the rejection.
         */
        public String getReason() {
            return reason;
        }

        /**
         * Returns the bare JID of the inviter. (e.g. 'hecate@shakespeare.lit')
         *
         * @return the bare JID of the inviter.
         */
        public String getTo() {
            return to;
        }

        /**
         * Sets the bare JID of the invitee that rejected the invitation. (e.g.
         * 'crone1@shakespeare.lit/desktop').
         *
         * @param from the bare JID of the invitee that rejected the invitation.
         */
        public void setFrom(String from) {
            this.from = from;
        }

        /**
         * Sets the message explaining why the invitation was rejected.
         *
         * @param reason the message explaining the reason for the rejection.
         */
        public void setReason(String reason) {
            this.reason = reason;
        }

        /**
         * Sets the bare JID of the inviter. (e.g. 'hecate@shakespeare.lit')
         *
         * @param to the bare JID of the inviter.
         */
        public void setTo(String to) {
            this.to = to;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
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
    }

    /**
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
            return actor == null ? "" : actor;
        }

        /**
         * Returns the reason for the item child. The reason is optional and could be used to
         * explain the reason why a user (occupant) was kicked or banned.
         *
         * @return the reason for the item child.
         */
        public String getReason() {
            return reason == null ? "" : reason;
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
    }

    /**
     * Status code assists in presenting notification messages. The following link provides the
     * list of existing error codes (@link http://www.jabber.org/jeps/jep-0045.html#errorstatus).
     *
     * @author Gaston Dombiak
     */
    public static class Status {
        private String code;

        /**
         * Creates a new instance of Status with the specified code.
         *
         * @param code the code that uniquely identifies the reason of the error.
         */
        public Status(String code) {
            this.code = code;
        }

        /**
         * Returns the code that uniquely identifies the reason of the error. The code
         * assists in presenting notification messages.
         *
         * @return the code that uniquely identifies the reason of the error.
         */
        public String getCode() {
            return code;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<status code=\"").append(getCode()).append("\"/>");
            return buf.toString();
        }
    }

    /**
     * Represents a notification that the room has been destroyed. After a room has been destroyed,
     * the room occupants will receive a Presence packet of type 'unavailable' with the reason for
     * the room destruction if provided by the room owner.
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