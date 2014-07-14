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

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents extended presence information about roles, affiliations, full JIDs,
 * or status codes scoped by the 'http://jabber.org/protocol/muc#user' namespace.
 *
 * @author Gaston Dombiak
 */
public class MUCUser implements PacketExtension {

    public static final String ELEMENT = "x";
    public static final String NAMESPACE = MUCInitialPresence.NAMESPACE + "#user";

    private Invite invite;
    private Decline decline;
    private MUCItem item;
    private String password;
    private Status status;
    private Destroy destroy;

    public String getElementName() {
        return ELEMENT;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngelBracket();
        xml.optElement(getInvite());
        xml.optElement(getDecline());
        xml.optElement(getItem());
        xml.optElement("password", getPassword());
        xml.optElement(getStatus());
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
     * Retrieve the MUCUser PacketExtension from packet, if any.
     *
     * @param packet
     * @return the MUCUser PacketExtension or {@code null}
     */
    public static MUCUser getFrom(Packet packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * Represents an invitation for another user to a room. The sender of the invitation
     * must be an occupant of the room. The invitation will be sent to the room which in turn
     * will forward the invitation to the invitee.
     *
     * @author Gaston Dombiak
     */
    public static class Invite implements Element {
        public static final String ELEMENT ="invite";

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

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("to", getTo());
            xml.optAttribute("from", getFrom());
            xml.rightAngelBracket();
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
    public static class Decline implements Element {
        public static final String ELEMENT = "decline";

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

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("to", getTo());
            xml.optAttribute("from", getFrom());
            xml.rightAngelBracket();
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
     * list of existing error codes (@link http://www.xmpp.org/extensions/jep-0045.html#errorstatus).
     *
     * @author Gaston Dombiak
     */
    public static class Status implements Element {
        public static final String ELEMENT = "status";

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

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("code", getCode());
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }
}
