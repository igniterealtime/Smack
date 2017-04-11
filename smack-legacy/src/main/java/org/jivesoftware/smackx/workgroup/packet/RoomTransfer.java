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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ.IQChildElementXmlStringBuilder;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Stanza(/Packet) extension for {@link org.jivesoftware.smackx.workgroup.agent.TransferRequest}.
 *
 * @author Gaston Dombiak
 */
public class RoomTransfer implements ExtensionElement {

    /**
     * Element name of the stanza(/packet) extension.
     */
    public static final String ELEMENT_NAME = "transfer";

    /**
     * Namespace of the stanza(/packet) extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    /**
     * Type of entity being invited to a groupchat support session.
     */
    private RoomTransfer.Type type;
    /**
     * JID of the entity being invited. The entity could be another agent, user , a queue or a workgroup. In
     * the case of a queue or a workgroup the server will select the best agent to invite.
     */
    private String invitee;
    /**
     * Full JID of the user that sent the invitation.
     */
    private String inviter;
    /**
     * ID of the session that originated the initial user request.
     */
    private String sessionID;
    /**
     * JID of the room to join if offer is accepted.
     */
    private String room;
    /**
     * Text provided by the inviter explaining the reason why the invitee is invited.
     */
    private String reason;

    public RoomTransfer(RoomTransfer.Type type, String invitee, String sessionID, String reason) {
        this.type = type;
        this.invitee = invitee;
        this.sessionID = sessionID;
        this.reason = reason;
    }

    private RoomTransfer() {
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public String getInviter() {
        return inviter;
    }

    public String getRoom() {
        return room;
    }

    public String getReason() {
        return reason;
    }

    public String getSessionID() {
        return sessionID;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = getIQChildElementBuilder(new IQChildElementXmlStringBuilder(this));
        xml.closeElement(this);
        return xml;
    }

    public IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.append("\" type=\"").append(type.name()).append("\">");
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
    public static enum Type {
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

    public static class RoomTransferIQ extends IQ {
        private final RoomTransfer roomTransfer;
        public RoomTransferIQ(RoomTransfer roomTransfer) {
            super(ELEMENT_NAME, NAMESPACE);
            this.roomTransfer = roomTransfer;
        }
        @Override
        protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
            return roomTransfer.getIQChildElementBuilder(xml);
        }
    }

    public static class Provider extends ExtensionElementProvider<RoomTransfer> {

        @Override
        public RoomTransfer parse(XmlPullParser parser,
                        int initialDepth) throws XmlPullParserException,
                        IOException {
            final RoomTransfer invitation = new RoomTransfer();
            invitation.type = RoomTransfer.Type.valueOf(parser.getAttributeValue("", "type"));

            boolean done = false;
            while (!done) {
                parser.next();
                String elementName = parser.getName();
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    if ("session".equals(elementName)) {
                        invitation.sessionID = parser.getAttributeValue("", "id");
                    }
                    else if ("invitee".equals(elementName)) {
                        invitation.invitee = parser.nextText();
                    }
                    else if ("inviter".equals(elementName)) {
                        invitation.inviter = parser.nextText();
                    }
                    else if ("reason".equals(elementName)) {
                        invitation.reason = parser.nextText();
                    }
                    else if ("room".equals(elementName)) {
                        invitation.room = parser.nextText();
                    }
                }
                else if (parser.getEventType() == XmlPullParser.END_TAG && ELEMENT_NAME.equals(elementName)) {
                    done = true;
                }
            }
            return invitation;
        }
    }
}
