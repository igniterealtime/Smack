/**
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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smackx.workgroup.MetaData;
import org.jivesoftware.smackx.workgroup.agent.InvitationRequest;
import org.jivesoftware.smackx.workgroup.agent.OfferContent;
import org.jivesoftware.smackx.workgroup.agent.TransferRequest;
import org.jivesoftware.smackx.workgroup.agent.UserRequest;
import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

/**
 * An IQProvider for agent offer requests.
 *
 * @author loki der quaeler
 */
public class OfferRequestProvider implements IQProvider {

    public OfferRequestProvider() {
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        int eventType = parser.getEventType();
        String sessionID = null;
        int timeout = -1;
        OfferContent content = null;
        boolean done = false;
        Map metaData = new HashMap();

        if (eventType != XmlPullParser.START_TAG) {
            // throw exception
        }

        String userJID = parser.getAttributeValue("", "jid");
        // Default userID to the JID.
        String userID = userJID;

        while (!done) {
            eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
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
                    userID = parser.getAttributeValue("", "id");
                }
                else if ("user-request".equals(elemName)) {
                    content = UserRequest.getInstance();
                }
                else if (RoomInvitation.ELEMENT_NAME.equals(elemName)) {
                    RoomInvitation invitation = (RoomInvitation) PacketParserUtils
                            .parsePacketExtension(RoomInvitation.ELEMENT_NAME, RoomInvitation.NAMESPACE, parser);
                    content = new InvitationRequest(invitation.getInviter(), invitation.getRoom(),
                            invitation.getReason());
                }
                else if (RoomTransfer.ELEMENT_NAME.equals(elemName)) {
                    RoomTransfer transfer = (RoomTransfer) PacketParserUtils
                            .parsePacketExtension(RoomTransfer.ELEMENT_NAME, RoomTransfer.NAMESPACE, parser);
                    content = new TransferRequest(transfer.getInviter(), transfer.getRoom(), transfer.getReason());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if ("offer".equals(parser.getName())) {
                    done = true;
                }
            }
        }

        OfferRequestPacket offerRequest =
                new OfferRequestPacket(userJID, userID, timeout, metaData, sessionID, content);
        offerRequest.setType(IQ.Type.SET);

        return offerRequest;
    }

    public static class OfferRequestPacket extends IQ {

        private int timeout;
        private String userID;
        private String userJID;
        private Map metaData;
        private String sessionID;
        private OfferContent content;

        public OfferRequestPacket(String userJID, String userID, int timeout, Map metaData,
                String sessionID, OfferContent content)
        {
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
        public String getUserID() {
            return userID;
        }

        /**
         * The JID of the user that made the "join queue" request.
         *
         * @return the user JID.
         */
        public String getUserJID() {
            return userJID;
        }

        /**
         * Returns the session ID associated with the request and ensuing chat. If the offer
         * does not contain a session ID, <tt>null</tt> will be returned.
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
        public Map getMetaData() {
            return this.metaData;
        }

        public String getChildElementXML () {
            StringBuilder buf = new StringBuilder();

            buf.append("<offer xmlns=\"http://jabber.org/protocol/workgroup\" jid=\"").append(userJID).append("\">");
            buf.append("<timeout>").append(timeout).append("</timeout>");

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

            buf.append("</offer>");

            return buf.toString();
        }
    }
}
