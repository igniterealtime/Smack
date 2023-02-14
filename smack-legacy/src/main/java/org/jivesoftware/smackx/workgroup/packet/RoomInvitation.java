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

package org.jivesoftware.smackx.workgroup.packet;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.IQChildElementXmlStringBuilder;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Stanza extension for {@link org.jivesoftware.smackx.workgroup.agent.InvitationRequest}.
 *
 * @author Gaston Dombiak
 */
public class RoomInvitation implements ExtensionElement {

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "invite";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT_NAME);

    /**
     * Type of entity being invited to a groupchat support session.
     */
    private Type type;
    /**
     * JID of the entity being invited. The entity could be another agent, user , a queue or a workgroup. In
     * the case of a queue or a workgroup the server will select the best agent to invite.
     */
    private Jid invitee;
    /**
     * Full JID of the user that sent the invitation.
     */
    private EntityJid inviter;
    /**
     * ID of the session that originated the initial user request.
     */
    private String sessionID;
    /**
     * JID of the room to join if offer is accepted.
     */
    private EntityBareJid room;
    /**
     * Text provided by the inviter explaining the reason why the invitee is invited.
     */
    private String reason;

    public RoomInvitation(Type type, Jid invitee, String sessionID, String reason) {
        this.type = type;
        this.invitee = invitee;
        this.sessionID = sessionID;
        this.reason = reason;
    }

    private RoomInvitation() {
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public EntityJid getInviter() {
        return inviter;
    }

    public EntityBareJid getRoom() {
        return room;
    }

    public String getReason() {
        return reason;
    }

    public String getSessionID() {
        return sessionID;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = getIQChildElementBuilder(new IQChildElementXmlStringBuilder(this, enclosingNamespace));
        xml.closeElement(this);
        return xml;
    }

    public IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.append(" type=\"").append(type.name()).append("\">");
        buf.append("<session xmlns=\"http://jivesoftware.com/protocol/workgroup\" id=\"").append(sessionID).append("\"></session>");
        if (invitee != null) {
            buf.append("<invitee>").append(invitee).append("</invitee>");
        }
        if (inviter != null) {
            buf.append("<inviter>").append(inviter).append("</inviter>");
        }
        if (reason != null) {
            buf.append("<reason>").append(reason).append("</reason>");
        }

        return buf;
    }

    /**
     * Type of entity being invited to a groupchat support session.
     */
    public enum Type {
        /**
         * A user is being invited to a groupchat support session. The user could be another agent
         * or just a regular XMPP user.
         */
        user,
        /**
         * Some agent of the specified queue will be invited to the groupchat support session.
         */
        queue,
        /**
         * Some agent of the specified workgroup will be invited to the groupchat support session.
         */
        workgroup
    }

    public static class RoomInvitationIQ extends IQ {
        private final RoomInvitation roomInvitation;
        public RoomInvitationIQ(RoomInvitation roomInvitation) {
            super(ELEMENT_NAME, NAMESPACE);
            this.roomInvitation = roomInvitation;
        }
        @Override
        protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
            return roomInvitation.getIQChildElementBuilder(xml);
        }
    }

    public static class Provider extends ExtensionElementProvider<RoomInvitation> {

        @Override
        public RoomInvitation parse(XmlPullParser parser,
                        int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException,
                        IOException {
            final RoomInvitation invitation = new RoomInvitation();
            invitation.type = Type.valueOf(parser.getAttributeValue("", "type"));

            outerloop: while (true) {
                parser.next();
                if (parser.getEventType() == XmlPullParser.Event.START_ELEMENT) {
                    String elementName = parser.getName();
                    if ("session".equals(elementName)) {
                        invitation.sessionID = parser.getAttributeValue("", "id");
                    }
                    else if ("invitee".equals(elementName)) {
                        String inviteeString = parser.nextText();
                        invitation.invitee = JidCreate.from(inviteeString);
                    }
                    else if ("inviter".equals(elementName)) {
                        String inviterString = parser.nextText();
                        invitation.inviter = JidCreate.entityFrom(inviterString);
                    }
                    else if ("reason".equals(elementName)) {
                        invitation.reason = parser.nextText();
                    }
                    else if ("room".equals(elementName)) {
                        String roomString = parser.nextText();
                        invitation.room = JidCreate.entityBareFrom(roomString);
                    }
                }
                else if (parser.getEventType() == XmlPullParser.Event.END_ELEMENT && parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
            return invitation;
        }
    }
}
