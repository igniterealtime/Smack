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

package org.jivesoftware.smackx.muc.packet;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.Stanza;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.EntityJid;

/**
 * Represents extended presence information about roles, affiliations, full JIDs,
 * or status codes scoped by the 'http://jabber.org/protocol/muc#user' namespace.
 *
 * @author Gaston Dombiak
 */
public class MUCUser implements ExtensionElement {

    public static final String ELEMENT = "x";
    public static final String NAMESPACE = MUCInitialPresence.NAMESPACE + "#user";

    private final Set<Status> statusCodes = new HashSet<Status>(4);

    private Invite invite;
    private Decline decline;
    private MUCItem item;
    private String password;
    private Destroy destroy;

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement(getInvite());
        xml.optElement(getDecline());
        xml.optElement(getItem());
        xml.optElement("password", getPassword());
        xml.append(statusCodes);
        xml.optElement(getDestroy());
        xml.closeElement(this);
        return xml;
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
    public MUCItem getItem() {
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
     * Returns a set of status which holds the status code that assist in presenting notification messages.
     *
     * @return the set of status which holds the status code that assist in presenting notification messages.
     */
    public Set<Status> getStatus() {
        return statusCodes;
    }

    /**
     * Returns true if this MUCUser instance has also {@link Status} information.
     * <p>
     * If <code>true</code> is returned, then {@link #getStatus()} will return a non-empty set.
     * </p>
     *
     * @return true if this MUCUser has status information.
     * @since 4.1
     */
    public boolean hasStatus() {
        return !statusCodes.isEmpty();
    }

    /**
     * Returns the notification that the room has been destroyed. After a room has been destroyed,
     * the room occupants will receive a Presence stanza(/packet) of type 'unavailable' with the reason for
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
    public void setItem(MUCItem item) {
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
     * Add the status codes which holds the codes that assists in presenting notification messages.
     *
     * @param statusCodes the status codes which hold the codes that assists in presenting notification
     * messages.
     */
    public void addStatusCodes(Set<Status> statusCodes) {
        this.statusCodes.addAll(statusCodes);
    }

    /**
     * Add a status code which hold a code that assists in presenting notification messages.
     *
     * @param status the status code which olds a code that assists in presenting notification messages.
     */
    public void addStatusCode(Status status) {
        this.statusCodes.add(status);
    }

    /**
     * Sets the notification that the room has been destroyed. After a room has been destroyed,
     * the room occupants will receive a Presence stanza(/packet) of type 'unavailable' with the reason for
     * the room destruction if provided by the room owner.
     *
     * @param destroy the notification that the room has been destroyed.
     */
    public void setDestroy(Destroy destroy) {
        this.destroy = destroy;
    }

    /**
     * Retrieve the MUCUser PacketExtension from packet, if any.
     *
     * @param packet
     * @return the MUCUser PacketExtension or {@code null}
     * @deprecated use {@link #from(Stanza)} instead
     */
    @Deprecated
    public static MUCUser getFrom(Stanza packet) {
        return from(packet);
    }

    /**
     * Retrieve the MUCUser PacketExtension from packet, if any.
     *
     * @param packet
     * @return the MUCUser PacketExtension or {@code null}
     */
    public static MUCUser from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * Represents an invitation for another user to a room. The sender of the invitation
     * must be an occupant of the room. The invitation will be sent to the room which in turn
     * will forward the invitation to the invitee.
     *
     * @author Gaston Dombiak
     */
    public static class Invite implements NamedElement {
        public static final String ELEMENT ="invite";

        private final String reason;

        /**
         * From XEP-0045 § 7.8.2: "… whose value is the bare JID, full JID, or occupant JID of the inviter …" 
         */
        private final EntityJid from;

        private final EntityBareJid to;

        public Invite(String reason, EntityFullJid from) {
            this(reason, from, null);
        }

        public Invite(String reason, EntityBareJid to) {
            this(reason, null, to);
        }

        public Invite(String reason, EntityJid from, EntityBareJid to) {
            this.reason = reason;
            this.from = from;
            this.to = to;
        }

        /**
         * Returns the bare JID of the inviter or, optionally, the room JID. (e.g.
         * 'crone1@shakespeare.lit/desktop').
         *
         * @return the room's occupant that sent the invitation.
         */
        public EntityJid getFrom() {
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
        public EntityBareJid getTo() {
            return to;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("to", getTo());
            xml.optAttribute("from", getFrom());
            xml.rightAngleBracket();
            xml.optElement("reason", getReason());
            xml.closeElement(this);
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * Represents a rejection to an invitation from another user to a room. The rejection will be
     * sent to the room which in turn will forward the refusal to the inviter.
     *
     * @author Gaston Dombiak
     */
    public static class Decline implements NamedElement {
        public static final String ELEMENT = "decline";

        private final String reason;
        private final EntityBareJid from;
        private final EntityBareJid to;

        public Decline(String reason, EntityBareJid to) {
            this(reason, null, to);
        }

        public Decline(String reason, EntityBareJid from, EntityBareJid to) {
            this.reason = reason;
            this.from = from;
            this.to = to;
        }

        /**
         * Returns the bare JID of the invitee that rejected the invitation. (e.g.
         * 'crone1@shakespeare.lit').
         *
         * @return the bare JID of the invitee that rejected the invitation.
         */
        public EntityBareJid getFrom() {
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
        public EntityBareJid getTo() {
            return to;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("to", getTo());
            xml.optAttribute("from", getFrom());
            xml.rightAngleBracket();
            xml.optElement("reason", getReason());
            xml.closeElement(this);
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * Status code assists in presenting notification messages. The following link provides the
     * list of existing error codes <a href="http://xmpp.org/registrar/mucstatus.html">Multi-User Chat Status Codes</a>.
     *
     * @author Gaston Dombiak
     */
    public static final class Status implements NamedElement {
        public static final String ELEMENT = "status";

        private static final Map<Integer, Status> statusMap = new HashMap<Integer, Status>(8);

        public static final Status PRESENCE_TO_SELF_110 = Status.create(110);
        public static final Status ROOM_CREATED_201 = Status.create(201);
        public static final Status BANNED_301 = Status.create(301);
        public static final Status NEW_NICKNAME_303 = Status.create(303);
        public static final Status KICKED_307 = Status.create(307);
        public static final Status REMOVED_AFFIL_CHANGE_321 = Status.create(321);

        private final Integer code;

        public static Status create(String string) {
            Integer integer = Integer.valueOf(string);
            return create(integer);
        }

        public static Status create(Integer i) {
            Status status = statusMap.get(i);
            if (status == null) {
                status = new Status(i);
                statusMap.put(i, status);
            }
            return status;
        }

        /**
         * Creates a new instance of Status with the specified code.
         *
         * @param code the code that uniquely identifies the reason of the error.
         */
        private Status(int code) {
            this.code = code;
        }

        /**
         * Returns the code that uniquely identifies the reason of the error. The code
         * assists in presenting notification messages.
         *
         * @return the code that uniquely identifies the reason of the error.
         */
        public int getCode() {
            return code;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("code", getCode());
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public String toString() {
            return code.toString();
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof Status) {
                Status otherStatus = (Status) other;
                return code.equals(otherStatus.getCode());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return code;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }
}
