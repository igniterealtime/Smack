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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.workgroup.MetaData;
import org.jivesoftware.smackx.workgroup.agent.InvitationRequest;
import org.jivesoftware.smackx.workgroup.agent.OfferContent;
import org.jivesoftware.smackx.workgroup.agent.TransferRequest;
import org.jivesoftware.smackx.workgroup.agent.UserRequest;
import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;

import org.jxmpp.jid.Jid;

/**
 * An IQProvider for agent offer requests.
 *
 * @author loki der quaeler
 */
public class OfferRequestProvider extends IQProvider<IQ> {
    // FIXME It seems because OfferRequestPacket is also defined here, we can
    // not add it as generic to the provider, the provider and the packet should
    // be split, but since this is legacy code, I don't think that this will
    // happen anytime soon.

    @Override
    public OfferRequestPacket parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        XmlPullParser.Event eventType = parser.getEventType();
        String sessionID = null;
        int timeout = -1;
        OfferContent content = null;
        boolean done = false;
        Map<String, List<String>> metaData = new HashMap<>();

        if (eventType != XmlPullParser.Event.START_ELEMENT) {
            // throw exception
        }

        Jid userJID = ParserUtils.getJidAttribute(parser);
        // Default userID to the JID.
        Jid userID = userJID;

        while (!done) {
            eventType = parser.next();

            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                String elemName = parser.getName();

                if ("timeout".equals(elemName)) {
                    timeout = Integer.parseInt(parser.nextText());
                }
                else if (MetaData.ELEMENT_NAME.equals(elemName)) {
                    metaData = MetaDataUtils.parseMetaData(parser);
                }
                else if (SessionID.ELEMENT_NAME.equals(elemName)) {
                   sessionID = parser.getAttributeValue("", "id");
                }
                else if (UserID.ELEMENT_NAME.equals(elemName)) {
                    userID = ParserUtils.getJidAttribute(parser, "id");
                }
                else if ("user-request".equals(elemName)) {
                    content = UserRequest.getInstance();
                }
                else if (RoomInvitation.ELEMENT_NAME.equals(elemName)) {
                    RoomInvitation invitation = (RoomInvitation) PacketParserUtils
                            .parseExtensionElement(RoomInvitation.ELEMENT_NAME, RoomInvitation.NAMESPACE, parser, xmlEnvironment);
                    content = new InvitationRequest(invitation.getInviter(), invitation.getRoom(),
                            invitation.getReason());
                }
                else if (RoomTransfer.ELEMENT_NAME.equals(elemName)) {
                    RoomTransfer transfer = (RoomTransfer) PacketParserUtils
                            .parseExtensionElement(RoomTransfer.ELEMENT_NAME, RoomTransfer.NAMESPACE, parser, xmlEnvironment);
                    content = new TransferRequest(transfer.getInviter(), transfer.getRoom(), transfer.getReason());
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if ("offer".equals(parser.getName())) {
                    done = true;
                }
            }
        }

        OfferRequestPacket offerRequest =
                new OfferRequestPacket(userJID, userID, timeout, metaData, sessionID, content);
        offerRequest.setType(IQ.Type.set);

        return offerRequest;
    }

    public static class OfferRequestPacket extends IQ {

        public static final String ELEMENT = "offer";
        public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

        private final int timeout;
        private final Jid userID;
        private final Jid userJID;
        private final Map<String, List<String>> metaData;
        private final String sessionID;
        private final OfferContent content;

        public OfferRequestPacket(Jid userJID, Jid userID, int timeout, Map<String, List<String>> metaData,
                String sessionID, OfferContent content) {
            super(ELEMENT, NAMESPACE);
            this.userJID = userJID;
            this.userID = userID;
            this.timeout = timeout;
            this.metaData = metaData;
            this.sessionID = sessionID;
            this.content = content;
        }

        /**
         * Returns the userID, which is either the same as the userJID or a special
         * value that the user provided as part of their "join queue" request.
         *
         * @return the user ID.
         */
        public Jid getUserID() {
            return userID;
        }

        /**
         * The JID of the user that made the "join queue" request.
         *
         * @return the user JID.
         */
        public Jid getUserJID() {
            return userJID;
        }

        /**
         * Returns the session ID associated with the request and ensuing chat. If the offer
         * does not contain a session ID, <code>null</code> will be returned.
         *
         * @return the session id associated with the request.
         */
        public String getSessionID() {
            return sessionID;
        }

        /**
         * Returns the number of seconds the agent has to accept the offer before
         * it times out.
         *
         * @return the offer timeout (in seconds).
         */
        public int getTimeout() {
            return this.timeout;
        }

        public OfferContent getContent() {
            return content;
        }

        /**
         * Returns any meta-data associated with the offer.
         *
         * @return meta-data associated with the offer.
         */
        public Map<String, List<String>> getMetaData() {
            return this.metaData;
        }

        @Override
        protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
            buf.append(" jid=\"").append(userJID).append("\">");
            buf.append("<timeout>").append(Integer.toString(timeout)).append("</timeout>");

            if (sessionID != null) {
                buf.append('<').append(SessionID.ELEMENT_NAME);
                buf.append(" session=\"");
                buf.append(getSessionID()).append("\" xmlns=\"");
                buf.append(SessionID.NAMESPACE).append("\"/>");
            }

            if (metaData != null) {
                buf.append(MetaDataUtils.serializeMetaData(metaData));
            }

            if (userID != null) {
                buf.append('<').append(UserID.ELEMENT_NAME);
                buf.append(" id=\"");
                buf.append(userID).append("\" xmlns=\"");
                buf.append(UserID.NAMESPACE).append("\"/>");
            }

            return buf;
        }
    }
}
